package com.saveclip.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Palette {
    val Bg = Color(0xFF0E0E14)
    val Surface = Color(0xFF171720)
    val SurfaceHigh = Color(0xFF21212D)
    val Stroke = Color(0xFF2C2C3A)
    val Text = Color(0xFFF4F4F8)
    val Muted = Color(0xFF9A9AB0)
    val Accent1 = Color(0xFF7C5CFF)
    val Accent2 = Color(0xFFFF5C9A)
    val Error = Color(0xFFFF6B6B)
}

val AccentBrush = Brush.linearGradient(listOf(Palette.Accent1, Palette.Accent2))

@Composable
fun SaveClipTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Accent1,
            secondary = Palette.Accent2,
            background = Palette.Bg,
            surface = Palette.Surface,
            surfaceVariant = Palette.SurfaceHigh,
            onBackground = Palette.Text,
            onSurface = Palette.Text,
            onSurfaceVariant = Palette.Muted,
            outline = Palette.Stroke,
            error = Palette.Error,
        ),
        content = content,
    )
}
