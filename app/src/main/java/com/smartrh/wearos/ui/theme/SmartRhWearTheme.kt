package com.smartrh.wearos.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val SmartRhColors = Colors(
    primary = Color(0xFF2DD4BF),
    primaryVariant = Color(0xFF14B8A6),
    secondary = Color(0xFF38BDF8),
    secondaryVariant = Color(0xFF0EA5E9),
    background = Color(0xFF06111F),
    surface = Color(0xFF0F172A),
    error = Color(0xFFEF4444),
    onPrimary = Color(0xFF03111A),
    onSecondary = Color(0xFF03111A),
    onBackground = Color(0xFFE5F7FF),
    onSurface = Color(0xFFE5F7FF),
    onError = Color.White
)

@Composable
fun SmartRhWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = SmartRhColors,
        content = content
    )
}
