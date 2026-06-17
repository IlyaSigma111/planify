package com.planify.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF8B5CF6)
val Cyan = Color(0xFF22D3EE)
val Amber = Color(0xFFF59E0B)
val DarkBg = Color(0xFF05050A)
val DarkSurface = Color(0xFF0F0F1A)
val DarkCard = Color(0xFF1A1A2E)
val StatusTodo = Color(0xFF60A5FA)
val StatusProgress = Color(0xFFFBBF24)
val StatusDone = Color(0xFF34D399)

private val DarkScheme = darkColorScheme(
    primary = Primary, secondary = Cyan, tertiary = Amber,
    background = DarkBg, surface = DarkSurface,
    surfaceVariant = DarkCard, onBackground = Color(0xFFF0F0F5),
    onSurface = Color(0xFFF0F0F5), onSurfaceVariant = Color(0xFF8888A0)
)

@Composable
fun PlanifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
