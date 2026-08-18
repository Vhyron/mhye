package com.vhyron.mhye.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt

/** Palette offered when creating or editing a category. */
internal val CATEGORY_COLORS = listOf(
    "#E53935", // red
    "#FB8C00", // orange
    "#FDD835", // yellow
    "#43A047", // green
    "#00ACC1", // cyan
    "#1E88E5", // blue
    "#5E35B1", // deep purple
    "#D81B60", // pink
    "#6D4C41", // brown
    "#9E9E9E"  // grey
)

/** Falls back to grey rather than crashing on a malformed stored hex. */
internal fun parseCategoryColor(colorHex: String?): Color =
    colorHex?.let { hex -> runCatching { Color(hex.toColorInt()) }.getOrNull() } ?: Color.Gray

@Composable
internal fun CategoryDot(
    colorHex: String?,
    modifier: Modifier = Modifier,
    size: Int = 14
) {
    val color = remember(colorHex) { parseCategoryColor(colorHex) }
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}
