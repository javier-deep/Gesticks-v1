package com.example.reloj.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun GesticksTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = wearColorScheme,
        content = content
    )
}
