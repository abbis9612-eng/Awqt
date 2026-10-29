package com.dcc.yawm.shared.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// The approved identity «الحبر» (docs/design/IDENTITY.md): ink and paper, colour only in icons.

@Immutable
data class Palette(
    val paper: Color,
    val ink: Color,
    /** The part of the ink word not filled yet. */
    val faint: Color,
    val grey: Color,
    val line: Color,
    /** Body text of secondary rows (prototype `.after`). */
    val soft: Color,
    val isDark: Boolean,
    /** The edge of the big word's letters before the ink reaches them (3:1 or more on paper). */
    val outline: Color,
)

val LightPalette = Palette(
    paper = Color(0xFFFFFFFF),
    ink = Color(0xFF121212),
    faint = Color(0xFFE6E4DF),
    grey = Color(0xFF77736C),
    line = Color(0x14121212),
    soft = Color(0xFF3A3835),
    isDark = false,
    outline = Color(0xFF8F8A82),
)

/** After Isha until Fajr. */
val DarkPalette = Palette(
    paper = Color(0xFF0F0F11),
    ink = Color(0xFFF1EEE7),
    faint = Color(0xFF38373D),
    grey = Color(0xFF9B978F),
    line = Color(0x1AF1EEE7),
    soft = Color(0xFFD3CFC7),
    isDark = true,
    outline = Color(0xFF77736C),
)

/** Unselected tab (prototype `.ios-tab button`). */
val TabIdleLight = Color(0xFF8E8A83)
val TabIdleDark = Color(0xFF8A867F)

/** Colour of the suhur part of the night line (the lantern's colour). */
val SuhurColor = Color(0xFF3F57A6)

@Immutable
data class YawmFonts(
    /** Aref Ruqaa: kept for the owner's earlier screens; display words now use [display]. */
    val ruqaa: FontFamily,
    /** Noto Naskh Arabic: all text. */
    val naskh: FontFamily,
    /** Amiri: times and numbers. */
    val amiri: FontFamily,
    /** Noto Kufi Arabic ExtraBold: the big word and every display word (owner's choice 2026-10-29). */
    val display: FontFamily = ruqaa,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }
val LocalFonts = staticCompositionLocalOf { YawmFonts(FontFamily.Default, FontFamily.Default, FontFamily.Default) }
val LocalReducedMotion = staticCompositionLocalOf { false }
