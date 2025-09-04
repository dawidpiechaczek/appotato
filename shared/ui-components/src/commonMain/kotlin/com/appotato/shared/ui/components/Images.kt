package com.appotato.shared.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import coil3.compose.SubcomposeAsyncImage

@Composable
fun UrlImage(modifier: Modifier = Modifier, contentDescription: String? = null, url: String) =
    SubcomposeAsyncImage(
        modifier = modifier,
        contentDescription = contentDescription,
        loading = { CircularProgressIndicator() },
        model = url,
    )

@Composable
fun FileImage(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    painter: Painter,
) = Image(modifier = modifier, contentDescription = contentDescription, painter = painter)
