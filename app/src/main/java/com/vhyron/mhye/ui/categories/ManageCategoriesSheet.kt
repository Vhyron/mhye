package com.vhyron.mhye.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vhyron.mhye.data.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesSheet(
    categories: List<Category>,
    categoryUsage: Map<Int, Int>,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    // null = closed, a Category = editing it, Category(id = 0) = adding.
    var editing by rememberSaveable(stateSaver = CategorySaver) {
        mutableStateOf<Category?>(null)
    }
    var pendingDelete by rememberSaveable(stateSaver = CategorySaver) {
        mutableStateOf<Category?>(null)
    }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { editing = NEW_CATEGORY }) {
                    Icon(Icons.Default.Add, contentDescription = "New category")
                }
            }

            categories.forEach { category ->
                val inUse = categoryUsage[category.id] ?: 0
                CategoryRow(
                    category = category,
                    inUse = inUse,
                    // Deleting the last category would leave the add form with
                    // nothing to select.
                    canDelete = inUse == 0 && categories.size > 1,
                    onClick = { editing = category },
                    onDelete = { pendingDelete = category }
                )
                HorizontalDivider()
            }
        }
    }

    editing?.let { target ->
        CategoryEditDialog(
            category = target,
            existingNames = categories
                .filter { it.id != target.id }
                .map { it.name },
            onDismiss = { editing = null },
            onConfirm = {
                onSave(it)
                editing = null
            }
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${target.name}?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(target)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    inUse: Int,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { CategoryDot(category.colorHex) },
        headlineContent = {
            Text(category.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                when (inUse) {
                    0 -> "Unused"
                    1 -> "1 subscription"
                    else -> "$inUse subscriptions"
                }
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete, enabled = canDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${category.name}",
                    tint = if (canDelete) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditDialog(
    category: Category,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(category.name) }
    var colorHex by rememberSaveable {
        mutableStateOf(category.colorHex.ifBlank { CATEGORY_COLORS.first() })
    }

    val trimmed = name.trim()
    val isDuplicate = existingNames.any { it.equals(trimmed, ignoreCase = true) }
    val isValid = trimmed.isNotEmpty() && !isDuplicate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category.id == 0) "New category" else "Edit category") },
        text = {
            Column(
                modifier = Modifier.imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = isDuplicate,
                    supportingText = if (isDuplicate) {
                        { Text("A category with this name already exists") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CATEGORY_COLORS.forEach { swatch ->
                        ColorSwatch(
                            colorHex = swatch,
                            selected = swatch.equals(colorHex, ignoreCase = true),
                            onClick = { colorHex = swatch }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(category.copy(name = trimmed, colorHex = colorHex)) },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ColorSwatch(colorHex: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(parseCategoryColor(colorHex))
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

private val NEW_CATEGORY = Category(id = 0, name = "", colorHex = CATEGORY_COLORS.first())

/** Category isn't Parcelable, so persist it as its three primitive fields. */
private val CategorySaver: Saver<Category?, Any> = listSaver(
    save = { category -> category?.let { listOf(it.id, it.name, it.colorHex) } ?: emptyList() },
    restore = { saved ->
        saved.takeIf { it.isNotEmpty() }?.let {
            Category(id = it[0] as Int, name = it[1] as String, colorHex = it[2] as String)
        }
    }
)
