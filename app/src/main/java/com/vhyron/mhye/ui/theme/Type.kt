package com.vhyron.mhye.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.vhyron.mhye.R

/**
 * Open Sans, bundled so the app looks the same on every device rather than
 * inheriting whatever the manufacturer ships as the system font.
 *
 * Material 3's type scale only ever asks for Normal and Medium; SemiBold is
 * kept for emphasis and ExtraBold for the list header. Each file is roughly
 * 120 KB, so adding weights is not free.
 *
 * Licensed under the SIL Open Font License 1.1; see licenses/OpenSans-OFL.txt.
 */
private val OpenSans = FontFamily(
    Font(R.font.open_sans_regular, FontWeight.Normal),
    Font(R.font.open_sans_medium, FontWeight.Medium),
    Font(R.font.open_sans_semibold, FontWeight.SemiBold),
    Font(R.font.open_sans_extrabold, FontWeight.ExtraBold)
)

private val default = Typography()

/** The stock Material 3 scale with only the family swapped. */
val Typography = Typography(
    displayLarge = default.displayLarge.copy(fontFamily = OpenSans),
    displayMedium = default.displayMedium.copy(fontFamily = OpenSans),
    displaySmall = default.displaySmall.copy(fontFamily = OpenSans),
    headlineLarge = default.headlineLarge.copy(fontFamily = OpenSans),
    headlineMedium = default.headlineMedium.copy(fontFamily = OpenSans),
    headlineSmall = default.headlineSmall.copy(fontFamily = OpenSans),
    titleLarge = default.titleLarge.copy(fontFamily = OpenSans),
    titleMedium = default.titleMedium.copy(fontFamily = OpenSans),
    titleSmall = default.titleSmall.copy(fontFamily = OpenSans),
    bodyLarge = default.bodyLarge.copy(fontFamily = OpenSans),
    bodyMedium = default.bodyMedium.copy(fontFamily = OpenSans),
    bodySmall = default.bodySmall.copy(fontFamily = OpenSans),
    labelLarge = default.labelLarge.copy(fontFamily = OpenSans),
    labelMedium = default.labelMedium.copy(fontFamily = OpenSans),
    labelSmall = default.labelSmall.copy(fontFamily = OpenSans)
)
