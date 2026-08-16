package com.appotato.shared.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage

/**
 * A remote image with something to show in its place.
 *
 * [fallback] stands in while the image loads *and* if it never arrives, rather than a spinner and
 * then a hole: these are decorative thumbnails, and at that size a spinner flashing in every row is
 * more distracting than the placeholder the row would have had anyway.
 *
 * [ContentScale.Fit] because product photography is portrait on a white background — cropping it to
 * a square cuts the label, which is the part that identifies it.
 */
@Composable
fun UrlImage(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    fallback: @Composable () -> Unit = {},
) = SubcomposeAsyncImage(
    modifier = modifier,
    contentDescription = contentDescription,
    contentScale = contentScale,
    loading = { fallback() },
    error = { fallback() },
    model = url,
)

@Composable
fun FileImage(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    painter: Painter,
) = Image(modifier = modifier, contentDescription = contentDescription, painter = painter)
