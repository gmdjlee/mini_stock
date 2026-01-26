package com.stockapp.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography system for StockApp.
 * Based on DESIGN_SYSTEM_SPEC.md v1.0.0
 *
 * Note: Montserrat font is recommended for production.
 * To enable Montserrat, add font files to res/font/ and uncomment the MontserratFontFamily definition.
 *
 * Font files needed:
 * - montserrat_regular.ttf
 * - montserrat_medium.ttf
 * - montserrat_semibold.ttf
 * - montserrat_bold.ttf
 */

// Uncomment when Montserrat fonts are added to res/font/
// val MontserratFontFamily = FontFamily(
//     Font(R.font.montserrat_regular, FontWeight.Normal),
//     Font(R.font.montserrat_medium, FontWeight.Medium),
//     Font(R.font.montserrat_semibold, FontWeight.SemiBold),
//     Font(R.font.montserrat_bold, FontWeight.Bold)
// )

// Use default font family until Montserrat is added
private val AppFontFamily = FontFamily.Default

/**
 * StockApp Typography with Design System specifications.
 *
 * Style Guide:
 * - Display (Bold/SemiBold): Impactful headers, hero sections
 * - Headline (SemiBold): Section headers
 * - Title (Medium/SemiBold): Card/component titles
 * - Body (Regular): Content text
 * - Label (Medium): Buttons, tags, chips
 */
val Typography = Typography(
    // Display Styles - Bold/SemiBold for impactful headers
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // Headline Styles - SemiBold for section headers
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // Title Styles - Medium/SemiBold for card/component titles
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Body Styles - Regular for content text
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // Label Styles - Medium for buttons, tags, chips
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Creates a scaled typography for accessibility support.
 *
 * @param displayScale Scale for display styles (0.0 - 2.0)
 * @param headlineScale Scale for headline styles (0.0 - 2.0)
 * @param titleScale Scale for title styles (0.0 - 2.0)
 * @param bodyScale Scale for body styles (0.0 - 2.0)
 * @param labelScale Scale for label styles (0.0 - 2.0)
 * @return Scaled Typography
 */
fun createScaledTypography(
    displayScale: Float = 1.0f,
    headlineScale: Float = 1.0f,
    titleScale: Float = 1.0f,
    bodyScale: Float = 1.0f,
    labelScale: Float = 1.0f
): Typography {
    return Typography(
        displayLarge = Typography.displayLarge.copy(
            fontSize = (57 * displayScale).sp,
            lineHeight = (64 * displayScale).sp
        ),
        displayMedium = Typography.displayMedium.copy(
            fontSize = (45 * displayScale).sp,
            lineHeight = (52 * displayScale).sp
        ),
        displaySmall = Typography.displaySmall.copy(
            fontSize = (36 * displayScale).sp,
            lineHeight = (44 * displayScale).sp
        ),
        headlineLarge = Typography.headlineLarge.copy(
            fontSize = (32 * headlineScale).sp,
            lineHeight = (40 * headlineScale).sp
        ),
        headlineMedium = Typography.headlineMedium.copy(
            fontSize = (28 * headlineScale).sp,
            lineHeight = (36 * headlineScale).sp
        ),
        headlineSmall = Typography.headlineSmall.copy(
            fontSize = (24 * headlineScale).sp,
            lineHeight = (32 * headlineScale).sp
        ),
        titleLarge = Typography.titleLarge.copy(
            fontSize = (22 * titleScale).sp,
            lineHeight = (28 * titleScale).sp
        ),
        titleMedium = Typography.titleMedium.copy(
            fontSize = (16 * titleScale).sp,
            lineHeight = (24 * titleScale).sp
        ),
        titleSmall = Typography.titleSmall.copy(
            fontSize = (14 * titleScale).sp,
            lineHeight = (20 * titleScale).sp
        ),
        bodyLarge = Typography.bodyLarge.copy(
            fontSize = (16 * bodyScale).sp,
            lineHeight = (24 * bodyScale).sp
        ),
        bodyMedium = Typography.bodyMedium.copy(
            fontSize = (14 * bodyScale).sp,
            lineHeight = (20 * bodyScale).sp
        ),
        bodySmall = Typography.bodySmall.copy(
            fontSize = (12 * bodyScale).sp,
            lineHeight = (16 * bodyScale).sp
        ),
        labelLarge = Typography.labelLarge.copy(
            fontSize = (14 * labelScale).sp,
            lineHeight = (20 * labelScale).sp
        ),
        labelMedium = Typography.labelMedium.copy(
            fontSize = (12 * labelScale).sp,
            lineHeight = (16 * labelScale).sp
        ),
        labelSmall = Typography.labelSmall.copy(
            fontSize = (11 * labelScale).sp,
            lineHeight = (16 * labelScale).sp
        )
    )
}
