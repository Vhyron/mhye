package com.vhyron.mhye.ui.subscriptions

import com.vhyron.mhye.data.Category
import com.vhyron.mhye.data.MonthlySpend
import com.vhyron.mhye.data.Subscription

enum class SortOrder { RENEWAL_DATE, NAME, MONTHLY_COST }

/**
 * Everything the list screen renders. [subscriptions] and [monthlySpend] are
 * already filtered and sorted — the summary reflects what's on screen, so
 * filtering by category also answers "what am I spending on this category".
 */
data class SubscriptionListUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val monthlySpend: List<MonthlySpend> = emptyList(),
    val categories: List<Category> = emptyList(),
    val sortOrder: SortOrder = SortOrder.RENEWAL_DATE,
    val statusFilter: String? = null,
    val categoryFilter: Int? = null,
    /** Distinguishes "nothing added yet" from "nothing matches the filter". */
    val hasAnySubscriptions: Boolean = false
)
