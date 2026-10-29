package com.dcc.yawm.shared.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Text in Noto Naskh Arabic, line height 1.5 or more, no letter-spacing (IDENTITY.md). */
@Composable
fun Txt(
    text: String,
    size: TextUnit = 17.sp,
    bold: Boolean = false,
    color: androidx.compose.ui.graphics.Color = LocalPalette.current.ink,
    align: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier,
    lineHeight: Float = 1.6f,
) {
    BasicText(
        text,
        modifier = modifier,
        style = TextStyle(
            color = color, fontSize = size, fontFamily = LocalFonts.current.naskh,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, textAlign = align,
            lineHeight = size * lineHeight,
        ),
    )
}

/** A time or number in Amiri. */
@Composable
fun Num(text: String, size: TextUnit = 18.sp, color: androidx.compose.ui.graphics.Color = LocalPalette.current.grey, modifier: Modifier = Modifier, bold: Boolean = false) {
    BasicText(
        text, modifier = modifier,
        style = TextStyle(color = color, fontSize = size, fontFamily = LocalFonts.current.amiri, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal),
    )
}

/** A sentence with the times in Amiri, as in the prototype's `.when em`. */
@Composable
fun When(parts: List<Pair<String, Boolean>>, modifier: Modifier = Modifier) {
    val pal = LocalPalette.current
    val fonts = LocalFonts.current
    BasicText(
        buildAnnotatedString {
            for ((s, isTime) in parts) {
                if (isTime) withStyle(SpanStyle(fontFamily = fonts.amiri, fontSize = 21.sp)) { append(s) } else append(s)
            }
        },
        modifier = modifier.fillMaxWidth(),
        style = TextStyle(color = pal.ink, fontSize = 19.sp, fontFamily = fonts.naskh, textAlign = TextAlign.Center, lineHeight = 32.sp),
    )
}

/** Prototype fitFs: long words get a smaller size. */
fun wordSize(word: String, big: Dp = 138.dp): Dp = when {
    word.length > 7 -> 88.dp
    word.length > 5 -> 112.dp
    else -> big
}

/** A small grey heading (prototype `h4`). */
@Composable
fun Heading(text: String) {
    Txt(text, size = 15.sp, bold = true, color = LocalPalette.current.grey, modifier = Modifier.padding(top = 24.dp, bottom = 6.dp))
}

/** A list row at least 52dp tall with a hairline under it. */
@Composable
fun ListRow(onClick: (() -> Unit)? = null, minHeight: Dp = 52.dp, divider: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val pal = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = minHeight)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawBehind { if (divider) drawLine(pal.line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Row label with an optional grey line under it. */
@Composable
fun RowScope.RowText(title: String, sub: String? = null, bold: Boolean = false, color: androidx.compose.ui.graphics.Color = LocalPalette.current.ink) {
    Column(Modifier.weight(1f)) {
        Txt(title, bold = bold, color = color, lineHeight = 1.5f)
        if (sub != null) Txt(sub, size = 14.sp, color = LocalPalette.current.grey, lineHeight = 1.5f)
    }
}

/** The ink switch (prototype `.sw`). */
@Composable
fun InkSwitch(on: Boolean, label: String, onChange: (Boolean) -> Unit) {
    val pal = LocalPalette.current
    Box(
        Modifier.size(width = 54.dp, height = 48.dp)
            .semantics { role = Role.Switch; contentDescription = label; stateDescription = if (on) "شغّال" else "مطفي" }
            .clickable { onChange(!on) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(width = 54.dp, height = 32.dp)
                .background(if (on) pal.ink else pal.paper, CircleShape)
                .border(1.5.dp, pal.ink, CircleShape),
        ) {
            Box(
                Modifier.padding(3.dp).offset(x = if (on) 22.dp else 0.dp).size(23.dp)
                    .background(if (on) pal.paper else pal.ink.copy(alpha = 0.35f), CircleShape),
            )
        }
    }
}

/** − value + (prototype `.step`). */
@Composable
fun Stepper(value: String, minusLabel: String, plusLabel: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val pal = LocalPalette.current
    Row(
        Modifier.border(1.5.dp, pal.ink, RoundedCornerShape(999.dp)).padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", minusLabel, onMinus)
        Box(Modifier.width(60.dp), contentAlignment = Alignment.Center) { Num(value, size = 20.sp, color = pal.ink) }
        StepButton("+", plusLabel, onPlus)
    }
}

@Composable
private fun StepButton(sign: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clickable(onClick = onClick).semantics { contentDescription = label; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Txt(sign, size = 22.sp, bold = true) }
}

/** A round check (prototype `.ck`). */
@Composable
fun Check(on: Boolean) {
    val pal = LocalPalette.current
    Box(
        Modifier.size(28.dp).background(if (on) pal.ink else pal.paper, CircleShape).border(1.7.dp, if (on) pal.ink else pal.grey, CircleShape),
        contentAlignment = Alignment.Center,
    ) { if (on) Txt("✓", size = 15.sp, bold = true, color = pal.paper, lineHeight = 1.1f) }
}

/** The full-width ink button (prototype `.save`). */
@Composable
fun InkButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val pal = LocalPalette.current
    Box(
        modifier.fillMaxWidth().heightIn(min = 54.dp).background(pal.ink, CircleShape).clickable(onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Txt(text, bold = true, color = pal.paper, align = TextAlign.Center) }
}

/** An outlined pill (prototype `.back`). */
@Composable
fun PillButton(text: String, onClick: () -> Unit) {
    val pal = LocalPalette.current
    Box(
        Modifier.heightIn(min = 48.dp).border(1.5.dp, pal.ink, CircleShape).clickable(onClick = onClick)
            .semantics { role = Role.Button }.padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(text, size = 15.sp, bold = true) }
}
