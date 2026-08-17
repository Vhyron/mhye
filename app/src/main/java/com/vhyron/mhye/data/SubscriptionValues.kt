package com.vhyron.mhye.data

/** Allowed values for [Subscription.billingCycle], stored as plain strings. */
object BillingCycle {
    const val MONTHLY = "MONTHLY"
    const val YEARLY = "YEARLY"
    const val CUSTOM_DAYS = "CUSTOM_DAYS"
}

/** Allowed values for [Subscription.status], stored as plain strings. */
object SubscriptionStatus {
    const val ACTIVE = "ACTIVE"
    const val PAUSED = "PAUSED"
    const val CANCELLED = "CANCELLED"
}
