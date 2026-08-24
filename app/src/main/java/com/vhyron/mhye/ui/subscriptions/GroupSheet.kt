package com.vhyron.mhye.ui.subscriptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vhyron.mhye.ui.components.MhyeBottomSheet

/** Matches the sort sheet: single-select, applies and closes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSheet(
    groupBy: GroupBy,
    onGroupByChange: (GroupBy) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    MhyeBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Group by",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
            )

            GroupBy.entries.forEach { option ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable {
                            onGroupByChange(option)
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp),
                    headlineContent = { Text(groupLabel(option)) },
                    trailingContent = {
                        if (option == groupBy) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    }
}

internal fun groupLabel(groupBy: GroupBy): String = when (groupBy) {
    GroupBy.NONE -> "Nothing"
    GroupBy.CATEGORY -> "Category"
}
