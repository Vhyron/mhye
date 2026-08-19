package com.vhyron.mhye.ui.subscriptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vhyron.mhye.data.BillingCycle
import com.vhyron.mhye.data.Category
import com.vhyron.mhye.data.MonthlySpend
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionStatus
import com.vhyron.mhye.data.monthlySpend
import com.vhyron.mhye.ui.categories.CategoryDot
import com.vhyron.mhye.ui.categories.ManageCategoriesSheet
import com.vhyron.mhye.ui.theme.MhyeTheme

@Composable
fun SubscriptionListScreen(
    modifier: Modifier = Modifier,
    viewModel: SubscriptionListViewModel = viewModel(factory = SubscriptionListViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showCategories by rememberSaveable { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showSort by rememberSaveable { mutableStateOf(false) }

    SubscriptionListScreen(
        uiState = uiState,
        onManageCategoriesClick = { showCategories = true },
        onSortClick = { showSort = true },
        onFiltersClick = { showFilters = true },
        onAddClick = {
            editingId = null
            showSheet = true
        },
        onSubscriptionClick = { subscription ->
            editingId = subscription.id
            showSheet = true
        },
        modifier = modifier
    )

    if (showSort) {
        SortSheet(
            sortOrder = uiState.sortOrder,
            onSortOrderChange = viewModel::setSortOrder,
            onDismiss = { showSort = false }
        )
    }

    if (showFilters) {
        FiltersSheet(
            uiState = uiState,
            onStatusFilterChange = viewModel::setStatusFilter,
            onCategoryFilterChange = viewModel::setCategoryFilter,
            onDismiss = { showFilters = false }
        )
    }

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

    if (showCategories) {
        ManageCategoriesSheet(
            categories = uiState.categories,
            categoryUsage = uiState.categoryUsage,
            onDismiss = { showCategories = false },
            onSave = viewModel::saveCategory,
            onDelete = viewModel::deleteCategory
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionListScreen(
    uiState: SubscriptionListUiState,
    onManageCategoriesClick: () -> Unit,
    onAddClick: () -> Unit,
    onSubscriptionClick: (Subscription) -> Unit,
    onSortClick: () -> Unit,
    onFiltersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoriesById = remember(uiState.categories) { uiState.categories.associateBy { it.id } }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Subscriptions")
                        // Counts what's on screen, so it tracks the filters.
                        if (uiState.hasAnySubscriptions) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Text(uiState.subscriptions.size.toString())
                            }
                        }
                    }
                },
                actions = { OverflowMenu(onManageCategoriesClick = onManageCategoriesClick) }
            )
        },
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

            ListControls(
                uiState = uiState,
                onSortClick = onSortClick,
                onFiltersClick = onFiltersClick
            )

            if (uiState.subscriptions.isEmpty()) {
                EmptyState(hasAnySubscriptions = uiState.hasAnySubscriptions)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.subscriptions, key = { it.id }) { subscription ->
                        SubscriptionRow(
                            subscription = subscription,
                            category = categoriesById[subscription.categoryId],
                            onClick = { onSubscriptionClick(subscription) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverflowMenu(onManageCategoriesClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Manage categories") },
            onClick = {
                expanded = false
                onManageCategoriesClick()
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpendSummary(monthlySpend: List<MonthlySpend>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Monthly spend",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Side by side, wrapping if there are more currencies than fit —
            // they're never summed, since nothing converts between them.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                monthlySpend.forEach { spend ->
                    Text(
                        text = formatAmount(spend.currency, spend.amount),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ListControls(
    uiState: SubscriptionListUiState,
    onSortClick: () -> Unit,
    onFiltersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        FilterChip(
            selected = uiState.activeFilterCount > 0,
            onClick = onFiltersClick,
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            },
            label = { Text("Filters") }
        )
        SortChip(sortOrder = uiState.sortOrder, onClick = onSortClick)
    }
}

/** Labelled with the active sort so it never needs opening to check. */
@Composable
private fun SortChip(sortOrder: SortOrder, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Text(text = sortLabel(sortOrder), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        }
    )
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

    Card(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
                categoryUsage = sample.groupingBy { it.categoryId }.eachCount(),
                hasAnySubscriptions = true
            ),
            onManageCategoriesClick = {},
            onAddClick = {},
            onSubscriptionClick = {},
            onSortClick = {},
            onFiltersClick = {}
        )
    }
}
