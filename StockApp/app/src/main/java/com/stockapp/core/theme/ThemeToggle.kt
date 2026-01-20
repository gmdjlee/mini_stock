package com.stockapp.core.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * Data class holding theme state and toggle callback.
 */
data class ThemeToggleState(
    val isDarkTheme: Boolean = false,
    val onToggle: () -> Unit = {}
)

/**
 * CompositionLocal for theme toggle functionality.
 * Allows any composable in the tree to access theme state and toggle.
 */
val LocalThemeToggle = compositionLocalOf { ThemeToggleState() }

/**
 * Theme toggle icon button for TopAppBar actions.
 * Uses LocalThemeToggle to get current state and toggle callback.
 */
@Composable
fun ThemeToggleButton() {
    val themeToggleState = LocalThemeToggle.current

    IconButton(onClick = themeToggleState.onToggle) {
        Icon(
            imageVector = if (themeToggleState.isDarkTheme) {
                Icons.Default.LightMode
            } else {
                Icons.Default.DarkMode
            },
            contentDescription = if (themeToggleState.isDarkTheme) {
                "라이트 모드로 전환"
            } else {
                "다크 모드로 전환"
            }
        )
    }
}
