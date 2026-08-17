package com.vhyron.mhye.ui.subscriptions

import com.vhyron.mhye.data.BillingCycle
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val renewalDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

internal fun formatRenewalDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(renewalDateFormatter)

internal fun formatAmount(currency: String, amount: Double): String =
    String.format(Locale.getDefault(), "%s %,.2f", currency, amount)

internal fun formatCost(subscription: Subscription): String =
    formatAmount(subscription.currency, subscription.cost)

internal fun statusLabel(status: String): String =
    when (status) {
        SubscriptionStatus.ACTIVE -> "Active"
        SubscriptionStatus.PAUSED -> "Paused"
        SubscriptionStatus.CANCELLED -> "Cancelled"
        else -> status
    }

internal fun billingCycleLabel(subscription: Subscription): String =
    when (subscription.billingCycle) {
        BillingCycle.MONTHLY -> "Monthly"
        BillingCycle.YEARLY -> "Yearly"
        BillingCycle.CUSTOM_DAYS ->
            subscription.customCycleDays?.let { "Every $it days" } ?: "Custom"
        else -> subscription.billingCycle
    }
