package com.vhyron.mhye.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vhyron.mhye.data.BillingCycle
import com.vhyron.mhye.data.Category
import com.vhyron.mhye.data.MonthlySpend
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionStatus
import com.vhyron.mhye.data.monthlySpend
import com.vhyron.mhye.ui.theme.MhyeTheme

@Composable
fun SubscriptionListScreen(
    modifier: Modifier = Modifier,
    viewModel: SubscriptionListViewModel = viewModel(factory = SubscriptionListViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }

    SubscriptionListScreen(
        uiState = uiState,
        onAddClick = {
            editingId = null
            showSheet = true
        },
        onSubscriptionClick = { subscription ->
            editingId = subscription.id
            showSheet = true
        },
        onSortOrderChange = viewModel::setSortOrder,
        onStatusFilterChange = viewModel::setStatusFilter,
        onCategoryFilterChange = viewModel::setCategoryFilter,
        modifier = modifier
    )

    if (showSheet) {
        // Resolved from the list so the sheet tracks the latest stored values.
        val editing = editingId?.let { id -> uiState.subscriptions.firstOrNull { it.id == id } }
        AddEditSubscriptionSheet(
            subscription = editing,
            categories = uiState.categories,
            onDismiss = { showSheet = false },
            onSave = { subscription ->
                if (editing == null) {
                    viewModel.addSubscription(subscription)
                } else {
                    viewModel.updateSubscription(subscription)
                }
                showSheet = false
            },
            onDelete = { subscription ->
                viewModel.deleteSubscription(subscription)
                showSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionListScreen(
    uiState: SubscriptionListUiState,
    onAddClick: () -> Unit,
    onSubscriptionClick: (Subscription) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onStatusFilterChange: (String?) -> Unit,
    onCategoryFilterChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoriesById = remember(uiState.categories) { uiState.categories.associateBy { it.id } }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Subscriptions") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add subscription")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Pinned above the list rather than scrolling away with it.
            if (uiState.monthlySpend.isNotEmpty()) {
                SpendSummary(uiState.monthlySpend)
            }

            FilterBar(
                uiState = uiState,
                onSortOrderChange = onSortOrderChange,
                onStatusFilterChange = onStatusFilterChange,
                onCategoryFilterChange = onCategoryFilterChange
            )
            HorizontalDivider()

            if (uiState.subscriptions.isEmpty()) {
                EmptyState(hasAnySubscriptions = uiState.hasAnySubscriptions)
            } else {
                LazyColumn {
                    items(uiState.subscriptions, key = { it.id }) { subscription ->
                        SubscriptionRow(
                            subscription = subscription,
                            category = categoriesById[subscription.categoryId],
                            onClick = { onSubscriptionClick(subscription) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendSummary(monthlySpend: List<MonthlySpend>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "Monthly spend",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // One line per currency — costs are never converted between them.
        monthlySpend.forEach { spend ->
            Text(
                text = formatAmount(spend.currency, spend.amount),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBar(
    uiState: SubscriptionListUiState,
    onSortOrderChange: (SortOrder) -> Unit,
    onStatusFilterChange: (String?) -> Unit,
    onCategoryFilterChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCategory = uiState.categories.firstOrNull { it.id == uiState.categoryFilter }

    // Wraps to a second line rather than scrolling chips off-screen, so every
    // filter stays reachable on narrow displays and with long category names.
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MenuChip(label = sortLabel(uiState.sortOrder), active = false) { dismiss ->
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(sortLabel(order)) },
                    onClick = {
                        onSortOrderChange(order)
                        dismiss()
                    }
                )
            }
        }

        MenuChip(
            label = uiState.statusFilter?.let(::statusLabel) ?: "Any status",
            active = uiState.statusFilter != null
        ) { dismiss ->
            DropdownMenuItem(
                text = { Text("Any status") },
                onClick = {
                    onStatusFilterChange(null)
                    dismiss()
                }
            )
            listOf(
                SubscriptionStatus.ACTIVE,
                SubscriptionStatus.PAUSED,
                SubscriptionStatus.CANCELLED
            ).forEach { status ->
                DropdownMenuItem(
                    text = { Text(statusLabel(status)) },
                    onClick = {
                        onStatusFilterChange(status)
                        dismiss()
                    }
                )
            }
        }

        MenuChip(
            label = selectedCategory?.name ?: "Any category",
            active = uiState.categoryFilter != null
        ) { dismiss ->
            DropdownMenuItem(
                text = { Text("Any category") },
                onClick = {
                    onCategoryFilterChange(null)
                    dismiss()
                }
            )
            uiState.categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    leadingIcon = { CategoryDot(category.colorHex) },
                    onClick = {
                        onCategoryFilterChange(category.id)
                        dismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    menuContent: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        FilterChip(
            selected = active,
            onClick = { expanded = true },
            label = {
                // User-defined category names can be arbitrarily long.
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 160.dp)
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            menuContent { expanded = false }
        }
    }
}

@Composable
private fun SubscriptionRow(
    subscription: Subscription,
    category: Category?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCancelled = subscription.status == SubscriptionStatus.CANCELLED
    val details = listOfNotNull(
        statusLabel(subscription.status).takeIf { subscription.status != SubscriptionStatus.ACTIVE },
        billingCycleLabel(subscription),
        "Renews ${formatRenewalDate(subscription.renewalDate)}"
    )

    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = { CategoryDot(category?.colorHex) },
        overlineContent = category?.let { { Text(it.name) } },
        headlineContent = {
            Text(
                text = subscription.name,
                textDecoration = if (isCancelled) TextDecoration.LineThrough else null
            )
        },
        supportingContent = { Text(details.joinToString(" · ")) },
        trailingContent = {
            Text(
                text = formatCost(subscription),
                style = MaterialTheme.typography.titleMedium,
                textDecoration = if (isCancelled) TextDecoration.LineThrough else null
            )
        }
    )
}

@Composable
private fun CategoryDot(colorHex: String?, modifier: Modifier = Modifier) {
    val color = remember(colorHex) { parseCategoryColor(colorHex) }
    Box(
        modifier = modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun EmptyState(hasAnySubscriptions: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (hasAnySubscriptions) {
                "Nothing matches these filters."
            } else {
                "No subscriptions yet.\nTap + to add your first one."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun sortLabel(order: SortOrder): String = when (order) {
    SortOrder.RENEWAL_DATE -> "Renewal date"
    SortOrder.NAME -> "Name"
    SortOrder.MONTHLY_COST -> "Monthly cost"
}

/** Falls back to grey rather than crashing on a malformed stored hex. */
private fun parseCategoryColor(colorHex: String?): Color =
    colorHex?.let { hex -> runCatching { Color(hex.toColorInt()) }.getOrNull() } ?: Color.Gray

@Preview(showBackground = true)
@Composable
private fun SubscriptionListPreview() {
    val categories = listOf(
        Category(id = 1, name = "Entertainment", colorHex = "#E53935"),
        Category(id = 2, name = "Infrastructure", colorHex = "#1E88E5")
    )
    val sample = listOf(
        Subscription(
            id = 1,
            name = "Netflix",
            cost = 549.0,
            currency = "PHP",
            billingCycle = BillingCycle.MONTHLY,
            renewalDate = 1_787_000_000_000L,
            categoryId = 1,
            status = SubscriptionStatus.ACTIVE
        ),
        Subscription(
            id = 2,
            name = "vhyron.dev",
            cost = 14.99,
            currency = "USD",
            billingCycle = BillingCycle.YEARLY,
            renewalDate = 1_800_000_000_000L,
            categoryId = 2,
            status = SubscriptionStatus.PAUSED
        ),
        Subscription(
            id = 3,
            name = "Proxy server",
            cost = 250.0,
            currency = "PHP",
            billingCycle = BillingCycle.CUSTOM_DAYS,
            customCycleDays = 90,
            renewalDate = 1_810_000_000_000L,
            categoryId = 2,
            status = SubscriptionStatus.CANCELLED
        )
    )

    MhyeTheme {
        SubscriptionListScreen(
            uiState = SubscriptionListUiState(
                subscriptions = sample,
                monthlySpend = monthlySpend(sample),
                categories = categories,
                hasAnySubscriptions = true
            ),
            onAddClick = {},
            onSubscriptionClick = {},
            onSortOrderChange = {},
            onStatusFilterChange = {},
            onCategoryFilterChange = {}
        )
    }
}
