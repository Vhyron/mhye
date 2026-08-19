package com.vhyron.mhye.ui.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vhyron.mhye.data.SubscriptionStatus
import com.vhyron.mhye.ui.categories.CategoryDot

/**
 * Status and category filters, opened from the Filters chip. Sort lives in
 * its own control so its current value stays visible. Selections apply
 * immediately — "Done" only closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FiltersSheet(
    uiState: SubscriptionListUiState,
    onStatusFilterChange: (String?) -> Unit,
    onCategoryFilterChange: (Int?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionLabel("Status")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.statusFilter == null,
                    onClick = { onStatusFilterChange(null) },
                    label = { Text("Any") }
                )
                listOf(
                    SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.PAUSED,
                    SubscriptionStatus.CANCELLED
                ).forEach { status ->
                    FilterChip(
                        selected = uiState.statusFilter == status,
                        onClick = { onStatusFilterChange(status) },
                        label = { Text(statusLabel(status)) }
                    )
                }
            }

            SectionLabel("Category")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = uiState.categoryFilter == null,
                    onClick = { onCategoryFilterChange(null) },
                    label = { Text("Any") }
                )
                uiState.categories.forEach { category ->
                    FilterChip(
                        selected = uiState.categoryFilter == category.id,
                        onClick = { onCategoryFilterChange(category.id) },
                        leadingIcon = {
                            CategoryDot(
                                colorHex = category.colorHex,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                                size = 12
                            )
                        },
                        label = {
                            Text(
                                text = category.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 180.dp)
                            )
                        }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                TextButton(
                    onClick = {
                        onStatusFilterChange(null)
                        onCategoryFilterChange(null)
                    },
                    enabled = uiState.activeFilterCount > 0
                ) {
                    Text("Clear filters")
                }
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

internal fun sortLabel(order: SortOrder): String = when (order) {
    SortOrder.RENEWAL_DATE -> "Renewal date"
    SortOrder.NAME -> "Name"
    SortOrder.MONTHLY_COST -> "Monthly cost"
}
