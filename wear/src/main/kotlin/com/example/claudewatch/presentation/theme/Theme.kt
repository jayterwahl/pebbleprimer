package com.example.claudewatch.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

/**
 * Theme for Claude Watch app using Wear OS Material 3.
 */
@Composable
fun ClaudeWatchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}
