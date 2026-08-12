package com.appotato.shared.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

/**
 * One wrapper per entry in [AppotatoTypography], so a feature never has to reach for material3's
 * `Text` and pick a style by hand.
 */
@Composable
fun HeaderText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = LocalCustomColors.current.primary,
    textAlign: TextAlign? = null,
) = Text(
    modifier = modifier,
    text = text,
    color = color,
    textAlign = textAlign,
    style = LocalCustomTypography.current.header,
)

@Composable
fun SubheaderText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = LocalCustomColors.current.primary,
    textAlign: TextAlign? = null,
) = Text(
    modifier = modifier,
    text = text,
    color = color,
    textAlign = textAlign,
    style = LocalCustomTypography.current.subheader,
)

@Composable
fun BodyText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = LocalCustomColors.current.primary,
    textAlign: TextAlign? = null,
) = Text(
    modifier = modifier,
    text = text,
    color = color,
    textAlign = textAlign,
    style = LocalCustomTypography.current.body,
)

@Composable
fun CommentText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = LocalCustomColors.current.primary,
    textAlign: TextAlign? = null,
) = Text(
    modifier = modifier,
    text = text,
    color = color,
    textAlign = textAlign,
    style = LocalCustomTypography.current.comment,
)
