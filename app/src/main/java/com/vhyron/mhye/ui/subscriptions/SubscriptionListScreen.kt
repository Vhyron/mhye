package com.vhyron.mhye.ui.subscriptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vhyron.mhye.data.BillingCycle
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionStatus
import com.vhyron.mhye.ui.theme.MhyeTheme

@Composable
fun SubscriptionListScreen(
    modifier: Modifier = Modifier,
    viewModel: SubscriptionListViewModel = viewModel(factory = SubscriptionListViewModel.Factory)
) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }

    SubscriptionListScreen(
        subscriptions = subscriptions,
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

    if (showSheet) {
        // Resolved from the list so the sheet tracks the latest stored values.
        val editing = editingId?.let { id -> subscriptions.firstOrNull { it.id == id } }
        AddEditSubscriptionSheet(
            subscription = editing,
            categories = categories,
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
    subscriptions: List<Subscription>,
    onAddClick: () -> Unit,
    onSubscriptionClick: (Subscription) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Subscriptions") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add subscription")
            }
        }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            items(subscriptions, key = { it.id }) { subscription ->
                SubscriptionRow(
                    subscription = subscription,
                    onClick = { onSubscriptionClick(subscription) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SubscriptionRow(
    subscription: Subscription,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
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
            ),
            onAddClick = {},
            onSubscriptionClick = {}
        )
    }
}
