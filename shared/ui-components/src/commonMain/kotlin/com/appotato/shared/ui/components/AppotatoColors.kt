package com.appotato.shared.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Every colour the app is allowed to use, named by the job it does rather than by what it looks
 * like. That is what makes a second palette possible at all: `white` cannot be dark, but a
 * `surface` can.
 *
 * The two palettes are one hue family — slate for everything structural, sky for the brand — so
 * [primary] and [primaryContainer] read as two weights of the same colour instead of two
 * competing ones. Contrast of text roles against their own background is at or above WCAG AA.
 */
@Immutable
data class AppotatoColors(
    /** Behind everything. One step below [surface] so cards have something to sit on. */
    val background: Color,
    /** Cards, sheets, bars. */
    val surface: Color,
    /** Hairlines, unfilled control borders. Never text. */
    val outline: Color,
    /** Brand colour: primary actions, selection, anything tappable that is not text. */
    val primary: Color,
    /** Content placed on [primary]. Not white in dark mode — the fill is light there. */
    val onPrimary: Color,
    /** A quiet weight of [primary]: unselected chips, the navigation indicator. */
    val primaryContainer: Color,
    /**
     * Content on [primaryContainer]. Separate from [primary] because in dark mode the brand blue
     * lands at 4.42:1 on its own container — just under AA — and one step lighter fixes it.
     */
    val onPrimaryContainer: Color,
    /** Main text and icons. */
    val content: Color,
    /** Secondary text: present, not competing with the line above it. */
    val muted: Color,
    /** Something is wrong or already lost — expired food, a failed purchase. */
    val danger: Color,
    /** Act soon, but nothing is lost yet. */
    val caution: Color,
    /** Fine, no action needed. */
    val success: Color,
    /** Content over photography or a camera feed, where there is no themed surface underneath. */
    val onOverlay: Color,
    val transparent: Color,
)

/**
 * Darker brand and status colours than the dark palette uses: the same hue needs more weight to
 * stay legible on white than it does on slate.
 */
internal val LightColors = AppotatoColors(
    background = Color(color = 0xFFF8FAFC),
    surface = Color(color = 0xFFFFFFFF),
    outline = Color(color = 0xFFE2E8F0),
    primary = Color(color = 0xFF0369A1),
    onPrimary = Color(color = 0xFFFFFFFF),
    primaryContainer = Color(color = 0xFFE0F2FE),
    onPrimaryContainer = Color(color = 0xFF0369A1),
    content = Color(color = 0xFF0F172A),
    muted = Color(color = 0xFF64748B),
    danger = Color(color = 0xFFDC2626),
    caution = Color(color = 0xFFB45309),
    success = Color(color = 0xFF047857),
    onOverlay = Color(color = 0xFFFFFFFF),
    transparent = Color.Transparent,
)

internal val DarkColors = AppotatoColors(
    background = Color(color = 0xFF0F172A),
    surface = Color(color = 0xFF1E293B),
    outline = Color(color = 0xFF334155),
    primary = Color(color = 0xFF38BDF8),
    // Dark text on a light-blue fill. White here would be unreadable.
    onPrimary = Color(color = 0xFF0F172A),
    primaryContainer = Color(color = 0xFF0C4A6E),
    onPrimaryContainer = Color(color = 0xFF7DD3FC),
    content = Color(color = 0xFFF1F5F9),
    muted = Color(color = 0xFF94A3B8),
    danger = Color(color = 0xFFF87171),
    caution = Color(color = 0xFFFBBF24),
    success = Color(color = 0xFF34D399),
    onOverlay = Color(color = 0xFFFFFFFF),
    transparent = Color.Transparent,
)
