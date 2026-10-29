package com.dcc.yawm.shared.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The signature (IDENTITY.md), made to read at a glance (owner, 2026-10-29): every letter keeps a
 * clear outline before the ink reaches it, and the ink fills it from the right with a soft edge.
 * When a word first appears it soaks into the paper (fades in, settles from slightly larger) and
 * the ink pours to now; afterwards it follows the clock. All motion stops with reduced motion.
 * The word is decoration: [label] carries the information for the screen reader.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun InkWord(word: String, progress: Float, size: Dp, label: String, modifier: Modifier = Modifier) {
    val pal = LocalPalette.current
    val fonts = LocalFonts.current
    val reduced = LocalReducedMotion.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val target = progress.coerceIn(0f, 1f)
    val ink = remember(word) { Animatable(if (reduced) target else 0f) }
    var poured by remember(word) { mutableStateOf(reduced) }
    LaunchedEffect(word, target) {
        if (reduced) ink.snapTo(target)
        else ink.animateTo(target, tween(if (poured) 600 else 1300, easing = FastOutSlowInEasing))
        poured = true
    }
    val soak = remember(word) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(word) { if (!reduced) soak.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }

    BoxWithConstraints(
        modifier.fillMaxWidth().padding(horizontal = 6.dp).clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        val maxPx = constraints.maxWidth
        // The word does not follow the font scale (it is decoration) and shrinks to fit its line.
        val fontSize: TextUnit = remember(word, maxPx, size, fonts) {
            var fs = size.value / density.fontScale
            fun width(v: Float) = measurer.measure(
                AnnotatedString(word),
                TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.ExtraBold, fontSize = v.sp),
            ).size.width
            while (fs > 44f && width(fs) > maxPx) fs -= 4f
            fs.sp
        }
        val base = TextStyle(
            fontFamily = fonts.display, fontWeight = FontWeight.ExtraBold, fontSize = fontSize,
            textAlign = TextAlign.Center, lineHeight = fontSize * 1.45f,
        )
        val p = ink.value
        val a = (1f - p - 0.02f).coerceIn(0f, 1f)
        val b = (1f - p + 0.02f).coerceIn(0f, 1f).coerceAtLeast(a)
        val stroke = with(density) { 1.8.dp.toPx() }
        Box(
            Modifier.graphicsLayer {
                alpha = soak.value
                val k = 1.06f - 0.06f * soak.value
                scaleX = k
                scaleY = k
            },
        ) {
            BasicText(word, style = base.copy(color = pal.faint))
            BasicText(word, style = base.copy(color = pal.outline, drawStyle = Stroke(width = stroke, join = StrokeJoin.Round)))
            BasicText(word, style = base.copy(brush = Brush.horizontalGradient(0f to Color.Transparent, a to Color.Transparent, b to pal.ink, 1f to pal.ink)))
        }
    }
}

/** A display word in the chosen display font (headings, the day name). */
@Composable
fun Display(text: String, size: TextUnit, color: Color = LocalPalette.current.ink, modifier: Modifier = Modifier) {
    BasicText(
        text, modifier,
        style = TextStyle(color = color, fontFamily = LocalFonts.current.display, fontWeight = FontWeight.ExtraBold, fontSize = size, lineHeight = size * 1.6f),
    )
}

/** A small rounded fact under the word: «تخلص 10:02». */
@Composable
fun Fact(label: String, value: String) {
    val pal = LocalPalette.current
    Row(
        Modifier.background(lerp(pal.paper, pal.ink, 0.05f), RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label, size = 15.sp, color = pal.grey, lineHeight = 1.4f)
        Num(value, size = 18.sp, color = pal.ink)
    }
}

/** A choice among a few, like the prototype's segmented control. */
@Composable
fun Segmented(options: List<String>, chosen: Int, modifier: Modifier = Modifier, onChoose: (Int) -> Unit) {
    val pal = LocalPalette.current
    Row(
        modifier.fillMaxWidth().background(lerp(pal.paper, pal.ink, 0.06f), RoundedCornerShape(16.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, o ->
            val on = i == chosen
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .background(if (on) (if (pal.isDark) Color(0xFF2A292E) else pal.paper) else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { onChoose(i) }
                    .semantics { role = Role.RadioButton; selected = on; contentDescription = o },
                contentAlignment = Alignment.Center,
            ) { Txt(o, size = 15.sp, bold = on, color = if (on) pal.ink else pal.grey, align = TextAlign.Center, lineHeight = 1.3f) }
        }
    }
}

/** A tappable card with an icon, a line and a grey line under it (prototype `.tonight`). */
@Composable
fun InkCard(icon: String, title: String, sub: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val pal = LocalPalette.current
    Row(
        modifier.fillMaxWidth().padding(top = 12.dp)
            .background(lerp(pal.paper, pal.ink, 0.05f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).semantics { role = Role.Button }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tile(icon, false)
        Column(Modifier.weight(1f)) {
            Txt(title, size = 16.sp, bold = true, lineHeight = 1.5f)
            Txt(sub, size = 14.sp, color = pal.grey, lineHeight = 1.5f)
        }
        Txt("‹", bold = true)
    }
}

/** An outlined box that fills when on: «مهمة جداً», day chips and the like. */
@Composable
fun OutlineBox(on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    val pal = LocalPalette.current
    Box(
        modifier.heightIn(min = 56.dp)
            .background(if (on) lerp(pal.paper, pal.ink, 0.06f) else pal.paper, RoundedCornerShape(16.dp))
            .border(1.5.dp, if (on) pal.ink else pal.line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) { content() }
}
