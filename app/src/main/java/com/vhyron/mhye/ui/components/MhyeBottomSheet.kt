package com.vhyron.mhye.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Every sheet in the app, so they can't drift apart.
 *
 * The container colour matches the list screen's background rather than
 * Material's default, which is a shade lighter and read as a different
 * surface. Sheets always open fully expanded — none of them are worth
 * showing half.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MhyeBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
        content = content
    )
}
