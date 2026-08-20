package com.vhyron.mhye.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.vhyron.mhye.R

/**
 * Poppins, bundled so the app looks the same on every device rather than
 * inheriting whatever the manufacturer ships as the system font.
 *
 * Only the weights Material 3 actually asks for are included — its type scale
 * uses Normal and Medium throughout, with SemiBold kept for emphasis. Each
 * file is roughly 155 KB, so adding weights is not free.
 */
private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold)
)

private val default = Typography()

/** The stock Material 3 scale with only the family swapped. */
val Typography = Typography(
    displayLarge = default.displayLarge.copy(fontFamily = Poppins),
    displayMedium = default.displayMedium.copy(fontFamily = Poppins),
    displaySmall = default.displaySmall.copy(fontFamily = Poppins),
    headlineLarge = default.headlineLarge.copy(fontFamily = Poppins),
    headlineMedium = default.headlineMedium.copy(fontFamily = Poppins),
    headlineSmall = default.headlineSmall.copy(fontFamily = Poppins),
    titleLarge = default.titleLarge.copy(fontFamily = Poppins),
    titleMedium = default.titleMedium.copy(fontFamily = Poppins),
    titleSmall = default.titleSmall.copy(fontFamily = Poppins),
    bodyLarge = default.bodyLarge.copy(fontFamily = Poppins),
    bodyMedium = default.bodyMedium.copy(fontFamily = Poppins),
    bodySmall = default.bodySmall.copy(fontFamily = Poppins),
    labelLarge = default.labelLarge.copy(fontFamily = Poppins),
    labelMedium = default.labelMedium.copy(fontFamily = Poppins),
    labelSmall = default.labelSmall.copy(fontFamily = Poppins)
)
