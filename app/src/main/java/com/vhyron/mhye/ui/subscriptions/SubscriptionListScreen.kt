package com.vhyron.mhye.ui.subscriptions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import com.vhyron.mhye.data.BackupResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
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
    var showGroup by rememberSaveable { mutableStateOf(false) }
    var pendingImport by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showReminderSettings by rememberSaveable { mutableStateOf(false) }
    val defaultReminderDays by viewModel.defaultReminderDays.collectAsStateWithLifecycle()

    val backupResult by viewModel.backupResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportTo) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingImport = uri }

    // Surface the outcome once, then clear it so rotation doesn't repeat it.
    LaunchedEffect(backupResult) {
        backupResult?.let {
            snackbarHostState.showSnackbar(backupMessage(it))
            viewModel.clearBackupResult()
        }
    }

    SubscriptionListScreen(
        uiState = uiState,
        onManageCategoriesClick = { showCategories = true },
        onReminderSettingsClick = { showReminderSettings = true },
        onExportClick = { exportLauncher.launch(defaultBackupFileName()) },
        onImportClick = { importLauncher.launch(arrayOf("application/json")) },
        snackbarHostState = snackbarHostState,
        onSortClick = { showSort = true },
        onGroupClick = { showGroup = true },
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

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Restore this backup?") },
            text = {
                Text(
                    "Everything currently in Mhye will be replaced by the " +
                        "contents of this file. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importFrom(uri)
                        pendingImport = null
                    }
                ) {
                    Text("Restore", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Cancel") }
            }
        )
    }

    if (showReminderSettings) {
        ReminderSettingsSheet(
            defaultReminderDays = defaultReminderDays,
            onDefaultReminderDaysChange = viewModel::setDefaultReminderDays,
            onDismiss = { showReminderSettings = false }
        )
    }

    if (showGroup) {
        GroupSheet(
            groupBy = uiState.groupBy,
            onGroupByChange = viewModel::setGroupBy,
            onDismiss = { showGroup = false }
        )
    }

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
            defaultReminderDays = defaultReminderDays,
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
            onDelete = { category, reassignTo ->
                viewModel.deleteCategory(category, reassignTo)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionListScreen(
    uiState: SubscriptionListUiState,
    onManageCategoriesClick: () -> Unit,
    onReminderSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onAddClick: () -> Unit,
    onSubscriptionClick: (Subscription) -> Unit,
    onSortClick: () -> Unit,
    onGroupClick: () -> Unit,
    onFiltersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoriesById = remember(uiState.categories) { uiState.categories.associateBy { it.id } }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Subscriptions", fontWeight = FontWeight.ExtraBold)
                        // Counts what's on screen, so it tracks the filters.
                        if (uiState.hasAnySubscriptions) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Text(
                                    text = uiState.subscriptions.size.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 3.dp
                                    )
                                )
                            }
                        }
                    }
                },
                actions = {
                    OverflowMenu(
                        onManageCategoriesClick = onManageCategoriesClick,
                        onReminderSettingsClick = onReminderSettingsClick,
                        onExportClick = onExportClick,
                        onImportClick = onImportClick
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
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
                onGroupClick = onGroupClick,
                onFiltersClick = onFiltersClick
            )

            if (uiState.subscriptions.isEmpty()) {
                EmptyState(hasAnySubscriptions = uiState.hasAnySubscriptions)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(CONNECTED_GAP)
                ) {
                    uiState.groups.forEachIndexed { groupIndex, group ->
                        group.category?.let { category ->
                            item(key = "header-${category.id}") {
                                CategoryHeader(category, isFirst = groupIndex == 0)
                            }
                        }
                        itemsIndexed(
                            items = group.subscriptions,
                            key = { _, subscription -> subscription.id }
                        ) { index, subscription ->
                            SubscriptionRow(
                                subscription = subscription,
                                category = categoriesById[subscription.categoryId],
                                // The header already names the category.
                                showCategory = group.category == null,
                                shape = connectedShape(index, group.subscriptions.size),
                                onClick = { onSubscriptionClick(subscription) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverflowMenu(
    onManageCategoriesClick: () -> Unit,
    onReminderSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
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
        DropdownMenuItem(
            text = { Text("Reminder settings") },
            onClick = {
                expanded = false
                onReminderSettingsClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Export backup") },
            onClick = {
                expanded = false
                onExportClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Restore backup") },
            onClick = {
                expanded = false
                onImportClick()
            }
        )
    }
}

private fun defaultBackupFileName(): String {
    val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now())
    return "mhye-backup-$stamp.json"
}

private fun backupMessage(result: BackupResult): String = when (result) {
    is BackupResult.Exported -> "Exported ${result.subscriptions} subscriptions"
    is BackupResult.Imported ->
        "Restored ${result.subscriptions} subscriptions in ${result.categories} categories"
    is BackupResult.Failed -> result.reason
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpendSummary(monthlySpend: List<MonthlySpend>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(GROUP_CORNER),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
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
@OptIn(ExperimentalLayoutApi::class)
private fun ListControls(
    uiState: SubscriptionListUiState,
    onSortClick: () -> Unit,
    onGroupClick: () -> Unit,
    onFiltersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Three controls no longer fit every screen width, so let them wrap
    // rather than clip.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        FilterChip(
            selected = uiState.groupBy != GroupBy.NONE,
            onClick = onGroupClick,
            label = { Text("Group") }
        )
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
        }
    )
}

@Composable
private fun SubscriptionRow(
    subscription: Subscription,
    category: Category?,
    showCategory: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCancelled = subscription.status == SubscriptionStatus.CANCELLED
    val details = listOfNotNull(
        statusLabel(subscription.status).takeIf { subscription.status != SubscriptionStatus.ACTIVE },
        billingCycleLabel(subscription),
        "Renews ${formatRenewalDate(subscription.renewalDate)}"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            // Inside a group the header carries the colour and name, so the
            // per-row dot is redundant — and dropping it gives the text room.
            leadingContent = if (showCategory) {
                { CategoryDot(category?.colorHex) }
            } else {
                null
            },
            overlineContent = category?.takeIf { showCategory }?.let { { Text(it.name) } },
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

/**
 * Rounding for a run of cards that read as one block, as the system Settings
 * app does it: only the outer edges of the run are fully rounded, and
 * neighbours nearly touch.
 */
@Composable
private fun CategoryHeader(category: Category, isFirst: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(
            start = 4.dp,
            // Groups need air between them; the first sits under the controls.
            top = if (isFirst) 0.dp else GROUP_GAP,
            bottom = 6.dp
        )
    ) {
        CategoryDot(category.colorHex)
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val GROUP_GAP = 16.dp
private val GROUP_CORNER = 20.dp
private val ITEM_CORNER = 4.dp
private val CONNECTED_GAP = 2.dp

private fun connectedShape(index: Int, count: Int): Shape = when {
    count == 1 -> RoundedCornerShape(GROUP_CORNER)
    index == 0 -> RoundedCornerShape(
        topStart = GROUP_CORNER, topEnd = GROUP_CORNER,
        bottomStart = ITEM_CORNER, bottomEnd = ITEM_CORNER
    )
    index == count - 1 -> RoundedCornerShape(
        topStart = ITEM_CORNER, topEnd = ITEM_CORNER,
        bottomStart = GROUP_CORNER, bottomEnd = GROUP_CORNER
    )
    else -> RoundedCornerShape(ITEM_CORNER)
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
                groups = listOf(SubscriptionGroup(category = null, subscriptions = sample)),
                monthlySpend = monthlySpend(sample),
                categories = categories,
                categoryUsage = sample.groupingBy { it.categoryId }.eachCount(),
                hasAnySubscriptions = true
            ),
            onManageCategoriesClick = {},
            onReminderSettingsClick = {},
            onExportClick = {},
            onImportClick = {},
            snackbarHostState = remember { SnackbarHostState() },
            onAddClick = {},
            onSubscriptionClick = {},
            onSortClick = {},
            onGroupClick = {},
            onFiltersClick = {}
        )
    }
}
