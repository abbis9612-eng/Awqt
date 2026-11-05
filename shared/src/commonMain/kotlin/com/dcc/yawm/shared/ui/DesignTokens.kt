package com.dcc.yawm.shared.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import com.dcc.yawm.shared.screen.Field

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
    /** A card on this paper (on «الألوان», a deeper tone of the field). */
    val card: Color = faint,
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
    /** Lalezar: the big name of the part on «الألوان» (2026-11-05). */
    val poster: FontFamily = display,
    /** Vazirmatn: text and numbers on «الألوان» (2026-11-05). */
    val ui: FontFamily = naskh,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }
val LocalFonts = staticCompositionLocalOf { YawmFonts(FontFamily.Default, FontFamily.Default, FontFamily.Default) }
val LocalReducedMotion = staticCompositionLocalOf { false }

/*
 * «الألوان» (approved 2026-11-05, docs/design/yawm-colors.html): each part of the day has one fixed flat
 * colour that fills the screen, with a text colour chosen for contrast on it (FieldColoursTest checks
 * 4.5:1 or more for the text and for the softer grey).
 */
@Immutable
data class FieldColours(val bg: Color, val fg: Color, val card: Color) {
    /** White text on a deep field: the system bars take light icons. */
    val isDark: Boolean get() = fg == Color.White
}

val FieldColour: Map<Field, FieldColours> = mapOf(
    // card: a deeper tone of the field (docs/design/yawm-colors-inside.html): 16% black on fajr and maghrib,
    // 10% of the text colour on the light fields, 12% white on isha and the night.
    Field.FAJR to FieldColours(Color(0xFF2B4A86), Color.White, Color(0xFF243E71)),
    Field.MORNING to FieldColours(Color(0xFFCFE3F4), Color(0xFF0F2133), Color(0xFFBCD0E1)),
    Field.DHUHR to FieldColours(Color(0xFFF7E6A6), Color(0xFF2A2208), Color(0xFFE2D296)),
    Field.ASR to FieldColours(Color(0xFFE09A3E), Color(0xFF1C1206), Color(0xFFCC8C38)),
    Field.MAGHRIB to FieldColours(Color(0xFFB23F2A), Color.White, Color(0xFF963523)),
    Field.ISHA to FieldColours(Color(0xFF1B2347), Color.White, Color(0xFF363D5D)),
    Field.NIGHT to FieldColours(Color(0xFF0A0A0A), Color.White, Color(0xFF272727)),
)

/** The palette of a field, so the shared rows and switches take its colours. */
fun fieldPalette(f: Field): Palette {
    val c = FieldColour.getValue(f)
    return Palette(
        paper = c.bg,
        ink = c.fg,
        faint = lerp(c.bg, c.fg, 0.22f),
        grey = lerp(c.bg, c.fg, 0.86f),
        line = c.fg.copy(alpha = 0.22f),
        soft = lerp(c.bg, c.fg, 0.93f),
        isDark = c.isDark,
        outline = lerp(c.bg, c.fg, 0.55f),
        card = c.card,
    )
}
