package com.vhyron.mhye.data

/** Average length of a month, used to normalize custom day-based cycles. */
private const val DAYS_PER_MONTH = 365.25 / 12

/** A monthly-equivalent total for one currency. */
data class MonthlySpend(val currency: String, val amount: Double)

/**
 * This subscription's cost expressed as a monthly figure, or `null` when the
 * cycle can't be interpreted (unknown value, or a custom cycle with no valid
 * day count).
 */
fun Subscription.monthlyCost(): Double? = when (billingCycle) {
    BillingCycle.MONTHLY -> cost
    BillingCycle.YEARLY -> cost / 12
    BillingCycle.CUSTOM_DAYS -> customCycleDays
        ?.takeIf { it > 0 }
        ?.let { days -> cost / days * DAYS_PER_MONTH }
    else -> null
}

/**
 * Monthly-equivalent spend across ACTIVE subscriptions, grouped by currency.
 *
 * Currencies are deliberately kept apart rather than summed into one number:
 * the app stores a currency label with no FX rates, so a combined total would
 * be meaningless. Ordered by largest total first.
 */
fun monthlySpend(subscriptions: List<Subscription>): List<MonthlySpend> = subscriptions
    .filter { it.status == SubscriptionStatus.ACTIVE }
    .mapNotNull { subscription ->
        subscription.monthlyCost()?.let { subscription.currency to it }
    }
    .groupBy({ it.first }, { it.second })
    .map { (currency, amounts) -> MonthlySpend(currency, amounts.sum()) }
    .sortedByDescending { it.amount }
