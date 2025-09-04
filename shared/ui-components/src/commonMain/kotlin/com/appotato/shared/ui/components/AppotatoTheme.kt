package com.appotato.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class AppotatoColors(
    val primary: Color,
    val secondary: Color,
    val background: List<Color>,
    val transparent: Color,
    val white: Color,
    val warning: Color,
    val info: Color,
)

@Immutable
data class AppotatoTypography(
    val header: TextStyle,
    val subheader: TextStyle,
    val body: TextStyle,
    val comment: TextStyle
)

@Immutable
data class AppotatoElevation(
    val small: Dp,
    val medium: Dp,
    val large: Dp,
)

internal val LocalCustomColors = staticCompositionLocalOf {
    AppotatoColors(
        primary = Color.Unspecified,
        secondary = Color.Unspecified,
        background = emptyList(),
        transparent = Color.Unspecified,
        white = Color.Unspecified,
        warning = Color.Unspecified,
        info = Color.Unspecified,
    )
}
internal val LocalCustomTypography = staticCompositionLocalOf {
    AppotatoTypography(
        header = TextStyle.Default,
        subheader = TextStyle.Default,
        body = TextStyle.Default,
        comment = TextStyle.Default,
    )
}
internal val LocalCustomElevation = staticCompositionLocalOf {
    AppotatoElevation(
        small = Dp.Unspecified,
        medium = Dp.Unspecified,
        large = Dp.Unspecified,
    )
}

@Composable
fun AppotatoTheme(
    content: @Composable () -> Unit
) {
    val customColors = AppotatoColors(
        primary = Color.Blue,
        secondary = Color.Green,
        background = listOf(Color.White, Color(0xFFF8BBD0)),
        transparent = Color.Transparent,
        white = Color.White,
        warning = Color.Red,
        info = Color.Yellow,
    )
    val customTypography = AppotatoTypography(
        header = TextStyle(fontSize = 32.sp),
        subheader = TextStyle(fontSize = 24.sp),
        body = TextStyle(fontSize = 16.sp),
        comment = TextStyle(fontSize = 12.sp),
    )
    val customElevation = AppotatoElevation(
        small = 2.dp,
        medium = 4.dp,
        large = 8.dp
    )
    CompositionLocalProvider(
        LocalCustomColors provides customColors,
        LocalCustomTypography provides customTypography,
        LocalCustomElevation provides customElevation,
        content = content,
    )
}

// Use with eg. CustomTheme.elevation.small
object AppotatoTheme {
    val colors: AppotatoColors
        @Composable
        get() = LocalCustomColors.current
    val typography: AppotatoTypography
        @Composable
        get() = LocalCustomTypography.current
    val elevation: AppotatoElevation
        @Composable
        get() = LocalCustomElevation.current
}
