package com.vhyron.mhye.ui.subscriptions

import com.vhyron.mhye.data.Category
import com.vhyron.mhye.data.MonthlySpend
import com.vhyron.mhye.data.Subscription

enum class SortOrder { RENEWAL_DATE, NAME, MONTHLY_COST }

enum class GroupBy { NONE, CATEGORY }

/**
 * One run of rows drawn as a connected block. [category] is null when the list
 * isn't grouped, in which case there is a single group holding everything.
 */
data class SubscriptionGroup(
    val category: Category?,
    val subscriptions: List<Subscription>
)

/**
 * Everything the list screen renders. [subscriptions] and [monthlySpend] are
 * already filtered and sorted — the summary reflects what's on screen, so
 * filtering by category also answers "what am I spending on this category".
 */
data class SubscriptionListUiState(
    val subscriptions: List<Subscription> = emptyList(),
    /** [subscriptions] arranged into connected blocks for rendering. */
    val groups: List<SubscriptionGroup> = emptyList(),
    val monthlySpend: List<MonthlySpend> = emptyList(),
    val categories: List<Category> = emptyList(),
    /** Subscriptions per category id, counted across *all* rows, not the filtered set. */
    val categoryUsage: Map<Int, Int> = emptyMap(),
    val sortOrder: SortOrder = SortOrder.RENEWAL_DATE,
    val groupBy: GroupBy = GroupBy.NONE,
    val statusFilter: String? = null,
    val categoryFilter: Int? = null,
    /** Distinguishes "nothing added yet" from "nothing matches the filter". */
    val hasAnySubscriptions: Boolean = false
) {
    /** Sort isn't counted — it always has a value, so it's never "active". */
    val activeFilterCount: Int
        get() = listOfNotNull(statusFilter, categoryFilter).size
}

/**
 * Arranges [visible] into connected blocks. Ungrouped yields a single block so
 * rendering has one shape to handle either way; grouping by category follows
 * the category list's own order and drops categories with nothing in them.
 */
fun groupsFor(
    visible: List<Subscription>,
    categories: List<Category>,
    grouping: GroupBy
): List<SubscriptionGroup> = when (grouping) {
    GroupBy.NONE -> listOfNotNull(
        SubscriptionGroup(category = null, subscriptions = visible)
            .takeIf { visible.isNotEmpty() }
    )
    GroupBy.CATEGORY -> {
        val byCategory = visible.groupBy { it.categoryId }
        categories.mapNotNull { category ->
            byCategory[category.id]?.let { SubscriptionGroup(category, it) }
        }
    }
}
