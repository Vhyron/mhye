package com.vhyron.mhye.ui.subscriptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vhyron.mhye.data.REMINDERS_OFF
import com.vhyron.mhye.data.REMINDER_DAY_OPTIONS

/** The app-wide default, used by any subscription without its own override. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsSheet(
    defaultReminderDays: Int,
    onDefaultReminderDaysChange: (Int) -> Unit,
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
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Remind me before renewal",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
            )

            (REMINDER_DAY_OPTIONS + REMINDERS_OFF).forEach { days ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable {
                            onDefaultReminderDaysChange(days)
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp),
                    headlineContent = { Text(reminderLabel(days)) },
                    trailingContent = {
                        if (days == defaultReminderDays) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }

            Text(
                text = "Subscriptions can override this individually.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
            )
        }
    }
}

internal fun reminderLabel(days: Int): String = when (days) {
    REMINDERS_OFF -> "Don't remind me"
    0 -> "On the renewal date"
    1 -> "1 day before"
    7 -> "1 week before"
    14 -> "2 weeks before"
    30 -> "1 month before"
    else -> "$days days before"
}
