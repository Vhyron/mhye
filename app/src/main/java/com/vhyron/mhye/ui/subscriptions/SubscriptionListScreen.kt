package com.vhyron.mhye.ui.subscriptions

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vhyron.mhye.data.BillingCycle
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionStatus
import com.vhyron.mhye.ui.theme.MhyeTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun SubscriptionListScreen(
    modifier: Modifier = Modifier,
    viewModel: SubscriptionListViewModel = viewModel(factory = SubscriptionListViewModel.Factory)
) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    SubscriptionListScreen(subscriptions = subscriptions, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionListScreen(
    subscriptions: List<Subscription>,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Subscriptions") }) }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            items(subscriptions, key = { it.id }) { subscription ->
                SubscriptionRow(subscription)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SubscriptionRow(subscription: Subscription, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(subscription.name) },
        supportingContent = {
            Text("${billingCycleLabel(subscription)} · Renews ${formatRenewalDate(subscription.renewalDate)}")
        },
        trailingContent = {
            Text(
                text = formatCost(subscription),
                style = MaterialTheme.typography.titleMedium
            )
        }
    )
}

private val renewalDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatRenewalDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(renewalDateFormatter)

private fun formatCost(subscription: Subscription): String =
    String.format(Locale.getDefault(), "%s %,.2f", subscription.currency, subscription.cost)

private fun billingCycleLabel(subscription: Subscription): String =
    when (subscription.billingCycle) {
        BillingCycle.MONTHLY -> "Monthly"
        BillingCycle.YEARLY -> "Yearly"
        BillingCycle.CUSTOM_DAYS ->
            subscription.customCycleDays?.let { "Every $it days" } ?: "Custom"
        else -> subscription.billingCycle
    }

@Preview(showBackground = true)
@Composable
private fun SubscriptionListPreview() {
    MhyeTheme {
        SubscriptionListScreen(
            subscriptions = listOf(
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
                    categoryId = 1,
                    status = SubscriptionStatus.ACTIVE
                ),
                Subscription(
                    id = 3,
                    name = "Proxy server",
                    cost = 250.0,
                    currency = "PHP",
                    billingCycle = BillingCycle.CUSTOM_DAYS,
                    customCycleDays = 90,
                    renewalDate = 1_810_000_000_000L,
                    categoryId = 1,
                    status = SubscriptionStatus.ACTIVE
                )
            )
        )
    }
}
