package com.vhyron.mhye.ui.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vhyron.mhye.data.BillingCycle
import com.vhyron.mhye.data.Category
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

private const val DEFAULT_CURRENCY = "PHP"

private val billingCycleOptions = listOf(
    BillingCycle.MONTHLY to "Monthly",
    BillingCycle.YEARLY to "Yearly",
    BillingCycle.CUSTOM_DAYS to "Custom"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionSheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Subscription) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        SubscriptionForm(categories = categories, onSave = onSave)
    }
}

@Composable
private fun SubscriptionForm(
    categories: List<Category>,
    onSave: (Subscription) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var cost by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf(DEFAULT_CURRENCY) }
    var billingCycle by rememberSaveable { mutableStateOf(BillingCycle.MONTHLY) }
    var customCycleDays by rememberSaveable { mutableStateOf("") }
    var renewalDate by rememberSaveable { mutableStateOf(defaultRenewalDate()) }
    var categoryId by rememberSaveable { mutableStateOf(0) }
    var notes by rememberSaveable { mutableStateOf("") }

    // Categories arrive from Room a frame or two after the sheet opens.
    LaunchedEffect(categories) {
        if (categoryId == 0) categoryId = categories.firstOrNull()?.id ?: 0
    }

    val parsedCost = cost.toDoubleOrNull()
    val parsedCustomCycleDays = customCycleDays.toIntOrNull()
    val isValid = name.isNotBlank() &&
        parsedCost != null && parsedCost > 0 &&
        currency.isNotBlank() &&
        categoryId != 0 &&
        (billingCycle != BillingCycle.CUSTOM_DAYS ||
            (parsedCustomCycleDays != null && parsedCustomCycleDays > 0))

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Add subscription",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = cost,
                onValueChange = { cost = it },
                label = { Text("Cost") },
                singleLine = true,
                isError = cost.isNotBlank() && (parsedCost == null || parsedCost <= 0),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(2f)
            )
            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it },
                label = { Text("Currency") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        CategoryDropdown(
            categories = categories,
            selectedCategoryId = categoryId,
            onCategorySelected = { categoryId = it }
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            billingCycleOptions.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = billingCycle == value,
                    onClick = { billingCycle = value },
                    shape = SegmentedButtonDefaults.itemShape(index, billingCycleOptions.size)
                ) {
                    Text(label)
                }
            }
        }

        if (billingCycle == BillingCycle.CUSTOM_DAYS) {
            OutlinedTextField(
                value = customCycleDays,
                onValueChange = { customCycleDays = it },
                label = { Text("Renews every (days)") },
                singleLine = true,
                isError = customCycleDays.isNotBlank() &&
                    (parsedCustomCycleDays == null || parsedCustomCycleDays <= 0),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        RenewalDateField(
            renewalDate = renewalDate,
            onRenewalDateChange = { renewalDate = it }
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                onSave(
                    Subscription(
                        name = name.trim(),
                        cost = checkNotNull(parsedCost),
                        currency = currency.trim().uppercase(),
                        billingCycle = billingCycle,
                        customCycleDays = parsedCustomCycleDays
                            .takeIf { billingCycle == BillingCycle.CUSTOM_DAYS },
                        renewalDate = renewalDate,
                        categoryId = categoryId,
                        status = SubscriptionStatus.ACTIVE,
                        notes = notes.trim().ifBlank { null }
                    )
                )
            },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Save")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: Int,
    onCategorySelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.name.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenewalDateField(
    renewalDate: Long,
    onRenewalDateChange: (Long) -> Unit
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = formatRenewalDate(renewalDate),
        onValueChange = {},
        readOnly = true,
        label = { Text("Renewal date") },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Pick renewal date")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localStartOfDayToUtcMillis(renewalDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onRenewalDateChange(utcMillisToLocalStartOfDay(it))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun defaultRenewalDate(): Long =
    LocalDate.now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/**
 * [DatePicker] works in UTC-midnight millis while the rest of the app formats
 * renewal dates in the device time zone, so translate across that boundary
 * rather than storing a timestamp that renders as the wrong day.
 */
private fun utcMillisToLocalStartOfDay(utcMillis: Long): Long =
    Instant.ofEpochMilli(utcMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

private fun localStartOfDayToUtcMillis(localMillis: Long): Long =
    Instant.ofEpochMilli(localMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
