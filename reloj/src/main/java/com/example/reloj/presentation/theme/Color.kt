package com.example.reloj.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

val PrimaryDark = Color(0xFF1B3A5C)
val AccentYellow = Color(0xFFFFD100)
val SubtitleTeal = Color(0xFF2DC4D4)
val TextGrey = Color(0xFF7A8B99)
val CriticalRed = Color(0xFFFF6B6B)
val CardBg = PrimaryDark

val wearColorScheme: ColorScheme = ColorScheme(
    primary = AccentYellow,
    onPrimary = PrimaryDark,
    secondary = SubtitleTeal,
    onSecondary = PrimaryDark,
    background = Color.Black,
    onBackground = Color.White,
    surfaceContainer = CardBg,
    onSurface = Color.White,
    onSurfaceVariant = TextGrey,
    error = CriticalRed,
    onError = Color.White
)
