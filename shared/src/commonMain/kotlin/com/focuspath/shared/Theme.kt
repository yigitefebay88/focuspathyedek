package com.focuspath.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AccentRed = Color(0xFFE57373)
val AccentYellow = Color(0xFFFFD54F)
val TerminalGreen = Color(0xFF81C784)
val DarkBackground = Color(0xFF121212)
val SurfaceColor = Color(0xFF1E1E1E)

private val SharedColorScheme = darkColorScheme(
    primary = TerminalGreen,
    secondary = AccentYellow,
    error = AccentRed,
    background = DarkBackground,
    surface = SurfaceColor,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun FocusPathTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SharedColorScheme,
        typography = Typography(
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp
            )
        ),
        content = content
    )
}
