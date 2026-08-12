package com.appotato.shared.ui.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Loader(modifier: Modifier = Modifier) = CircularProgressIndicator(
    modifier = modifier,
    color = LocalCustomColors.current.primary,
)
