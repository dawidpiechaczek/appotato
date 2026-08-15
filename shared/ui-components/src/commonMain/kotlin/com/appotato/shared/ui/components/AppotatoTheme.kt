package com.appotato.shared.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

internal val LocalCustomColors = staticCompositionLocalOf { LightColors }

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
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (isDark) DarkColors else LightColors
    val typography = AppotatoTypography(
        header = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
        subheader = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
        body = TextStyle(fontSize = 15.sp),
        comment = TextStyle(fontSize = 13.sp),
    )
    val elevation = AppotatoElevation(
        small = 1.dp,
        medium = 3.dp,
        large = 8.dp
    )

    CompositionLocalProvider(
        LocalCustomColors provides colors,
        LocalCustomTypography provides typography,
        LocalCustomElevation provides elevation,
    ) {
        // material3 components the app builds on — ModalBottomSheet, NavigationBar, TextField,
        // Surface — read their scrims, ripples and internal fills from MaterialTheme, not from the
        // wrappers. Without this the sheet scrim and text-field internals stay light in dark mode.
        MaterialTheme(
            colorScheme = colors.toColorScheme(isDark),
            content = content,
        )
    }
}

private fun AppotatoColors.toColorScheme(isDark: Boolean): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = onPrimaryContainer,
        background = background,
        onBackground = content,
        surface = surface,
        onSurface = content,
        surfaceVariant = primaryContainer,
        onSurfaceVariant = muted,
        surfaceContainer = surface,
        surfaceContainerLow = surface,
        surfaceContainerHigh = surface,
        outline = outline,
        outlineVariant = outline,
        error = danger,
        onError = onPrimary,
    )
}

// Use with eg. AppotatoTheme.elevation.small
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
