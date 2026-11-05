package com.dcc.yawm.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.screen.Field
import com.dcc.yawm.shared.screen.Where
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.metaLine

/*
 * «الألوان» parts shared by the home and the sheets (docs/design/yawm-colors-inside.html and
 * yawm-colors-add.html): cards in a deeper tone of the field, the prayer as the one inverted card,
 * a sheet that takes the colour of where things go, a small spectrum to choose a part.
 */

private val Ease = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/** The pressed scale of a tappable thing: .97 in 160 ms, none with reduced motion. */
@Composable
private fun pressScale(src: MutableInteractionSource, to: Float = 0.97f): Float {
    val pressed by src.collectIsPressedAsState()
    val reduced = LocalReducedMotion.current
    val s by animateFloatAsState(if (pressed && !reduced) to else 1f, tween(160, easing = Ease), label = "press")
    return s
}

/** A sheet over the dimmed screen in the colour of [field]; it changes colour when the field changes. */
@Composable
fun BoxScope.FieldSheet(field: Field, label: String, close: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val reduced = LocalReducedMotion.current
    val fonts = LocalFonts.current
    val bg by animateColorAsState(FieldColour.getValue(field).bg, tween(if (reduced) 0 else 240, easing = Ease), label = "sheet")
    val shown = remember { MutableTransitionState(reduced) }
    shown.targetState = true
    Box(
        Modifier.fillMaxSize().background(Color(0x6B000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close),
    )
    AnimatedVisibility(
        shown, Modifier.align(Alignment.BottomCenter),
        enter = if (reduced) fadeIn(tween(0)) else slideInVertically(spring(dampingRatio = 0.82f, stiffness = 420f)) { it } + fadeIn(tween(120)),
    ) {
        CompositionLocalProvider(LocalPalette provides fieldPalette(field), LocalFonts provides fonts.copy(naskh = fonts.ui, amiri = fonts.ui)) {
            val pal = LocalPalette.current
            Column(
                Modifier.fillMaxWidth().heightIn(max = 780.dp)
                    .background(bg, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .semantics { paneTitle = label }
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(44.dp, 5.dp).background(pal.line, RoundedCornerShape(3.dp)))
                }
                content()
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = close).semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) { Txt("سكّر", size = 16.sp, bold = true, color = pal.grey, align = TextAlign.Center, lineHeight = 1.3f) }
            }
        }
    }
}

/** The full-width button: filled in the text colour, or outlined. */
@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, outline: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val pal = LocalPalette.current
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src)
    Box(
        modifier.fillMaxWidth().heightIn(min = 60.dp).graphicsLayer { scaleX = s; scaleY = s }.alpha(if (enabled) 1f else 0.45f)
            .then(if (outline) Modifier.border(2.dp, pal.ink, CircleShape) else Modifier.background(pal.ink, CircleShape))
            .clickable(interactionSource = src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(text, size = 18.sp, bold = true, color = if (outline) pal.ink else pal.paper, align = TextAlign.Center, lineHeight = 1.35f) }
}

/** A small heading over a choice. */
@Composable
fun FieldHeading(text: String) {
    Txt(text, size = 15.sp, bold = true, color = LocalPalette.current.grey, modifier = Modifier.padding(top = 4.dp), lineHeight = 1.3f)
}

/** Choices joined in one outlined bar; the chosen one is filled. */
@Composable
fun Seg(options: List<String>, chosen: Int, onChoose: (Int) -> Unit) {
    val pal = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    Row(Modifier.fillMaxWidth().clip(shape).border(2.dp, pal.ink, shape)) {
        options.forEachIndexed { i, o ->
            val on = i == chosen
            if (i > 0) Box(Modifier.width(2.dp).heightIn(min = 52.dp).background(pal.line))
            Box(
                Modifier.weight(1f).heightIn(min = 52.dp).background(if (on) pal.ink else Color.Transparent)
                    .clickable { onChoose(i) }
                    .semantics { role = Role.RadioButton; selected = on; contentDescription = o },
                contentAlignment = Alignment.Center,
            ) { Txt(o, size = 15.5.sp, bold = true, color = if (on) pal.paper else pal.ink, align = TextAlign.Center, lineHeight = 1.3f) }
        }
    }
}

/** «وين؟»: the six parts as a small spectrum in their own colours, then «باچر» and «بعدين». */
@Composable
fun WhereBar(chosen: Where?, withTomorrow: Boolean = true, onPick: (Where) -> Unit) {
    val pal = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    Row(Modifier.fillMaxWidth().height(58.dp).clip(shape).border(2.dp, pal.line, shape)) {
        for (part in DayPart.entries) {
            val f = Field.of(part)
            val c = FieldColour.getValue(f)
            val on = chosen == Where.In(part)
            Box(
                Modifier.weight(1f).fillMaxHeight().background(c.bg)
                    .then(if (on) Modifier.border(3.dp, Color.White).padding(3.dp).border(1.5.dp, Color(0xFF111111)) else Modifier)
                    .clickable { onPick(Where.In(part)) }
                    .semantics { role = Role.RadioButton; selected = on; contentDescription = part.label },
                contentAlignment = Alignment.BottomCenter,
            ) { Txt(f.short, size = 13.sp, bold = true, color = c.fg, modifier = Modifier.padding(bottom = 6.dp), lineHeight = 1.2f) }
        }
    }
    if (withTomorrow) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("باچر", chosen == Where.Tomorrow) { onPick(Where.Tomorrow) }
        Pill("بعدين", chosen == Where.Later) { onPick(Where.Later) }
    }
}

@Composable
private fun Pill(text: String, on: Boolean, onClick: () -> Unit) {
    val pal = LocalPalette.current
    Box(
        Modifier.heightIn(min = 48.dp).background(if (on) pal.ink else Color.Transparent, CircleShape).border(2.dp, pal.ink, CircleShape)
            .clickable(onClick = onClick).semantics { role = Role.RadioButton; selected = on; contentDescription = text }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(text, size = 16.sp, bold = true, color = if (on) pal.paper else pal.ink, lineHeight = 1.3f) }
}

/** The round tick of a card: an outline, filled with a check when done. */
@Composable
private fun Tick(done: Boolean) {
    val pal = LocalPalette.current
    Canvas(Modifier.size(44.dp)) {
        val r = size.minDimension / 2
        if (done) drawCircle(pal.ink, r) else drawCircle(pal.ink, r - 1.25.dp.toPx(), style = Stroke(2.5.dp.toPx()))
        if (done) {
            val w = size.width
            val p = Path().apply {
                moveTo(w * 0.29f, w * 0.52f); lineTo(w * 0.44f, w * 0.67f); lineTo(w * 0.72f, w * 0.36f)
            }
            drawPath(p, pal.paper, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** One thing as a card: the tick, the title with its small line, and its time. Tap to open it. */
@Composable
fun ThingCard(x: Thing, date: kotlinx.datetime.LocalDate, onTick: (Thing) -> Unit, onOpen: (Thing) -> Unit) {
    val pal = LocalPalette.current
    val done = x.doneOn(date)
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, 0.985f)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).graphicsLayer { scaleX = s; scaleY = s }
            .clip(RoundedCornerShape(22.dp)).background(pal.card)
            .clickable(interactionSource = src, indication = null, role = Role.Button) { onOpen(x) }
            .semantics { contentDescription = x.title + (x.at?.let { "، الساعة ${clock(it)}" } ?: "") }
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).clickable { onTick(x) }
                .semantics { role = Role.Checkbox; contentDescription = "خلّصت ${x.title}"; stateDescription = if (done) "خلصت" else "باقية" },
            contentAlignment = Alignment.Center,
        ) { Tick(done) }
        Column(Modifier.weight(1f)) {
            Txt(x.title, size = 18.sp, bold = !done, color = if (done) pal.grey else pal.ink, lineHeight = 1.4f)
            Txt(x.metaLine(), size = 14.sp, color = pal.grey, lineHeight = 1.45f)
        }
        x.at?.let { Txt(clock(it), size = 20.sp, bold = true, lineHeight = 1.2f) }
    }
}

/**
 * The prayer of the part, the one inverted card: its name and time, and «صلّيت» once its time has come.
 * Before its time it is a plain card that says when it comes in.
 */
@Composable
fun PrayCard(name: String, at: Int, started: Boolean, done: Boolean, onPray: () -> Unit) {
    val pal = LocalPalette.current
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).background(if (started) pal.ink else pal.card, shape).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        val text = if (started) pal.paper else pal.ink
        Column(Modifier.weight(1f)) {
            Txt("$name ${clock(at)}", size = 19.sp, bold = true, color = text, lineHeight = 1.35f)
            Txt(
                when { !started -> "يدخل وكتها الساعة ${clock(at)}"; done -> "صلّيتها"; else -> "دخل وكتها" },
                size = 14.sp, color = if (started) text else pal.grey, lineHeight = 1.4f,
            )
        }
        if (started) {
            val src = remember { MutableInteractionSource() }
            val s = pressScale(src)
            Box(
                Modifier.heightIn(min = 48.dp).graphicsLayer { scaleX = s; scaleY = s }
                    .background(if (done) pal.paper else Color.Transparent, CircleShape).border(2.dp, pal.paper, CircleShape)
                    .clickable(interactionSource = src, indication = null, onClick = onPray)
                    .semantics { role = Role.Checkbox; contentDescription = "صلّيت $name"; stateDescription = if (done) "صلّيت" else "بعد" }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) { Txt(if (done) "✓ صلّيت" else "صلّيت", size = 16.sp, bold = true, color = if (done) pal.ink else pal.paper, lineHeight = 1.3f) }
        }
    }
}

/** «+ أضف لـ…» as an outlined card. */
@Composable
fun AddCard(label: String, onClick: () -> Unit) {
    val pal = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape).border(2.dp, pal.line, shape)
            .clickable(onClick = onClick).semantics { role = Role.Button }.padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).border(2.dp, pal.ink, CircleShape), contentAlignment = Alignment.Center) {
            Txt("+", size = 19.sp, bold = true, lineHeight = 1.1f)
        }
        Txt(label, size = 17.sp, bold = true, lineHeight = 1.4f)
    }
}

/** Rows grouped in one card (settings, the night). */
@Composable
fun CardGroup(content: @Composable ColumnScope.() -> Unit) {
    val pal = LocalPalette.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(pal.card).padding(horizontal = 16.dp, vertical = 4.dp), content = content)
}

/** A thin rule in the text colour, for under the big text field. */
@Composable
fun Rule(strong: Boolean) {
    val pal = LocalPalette.current
    Canvas(Modifier.fillMaxWidth().height(3.dp)) {
        drawLine(if (strong) pal.ink else pal.line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), if (strong) 3.dp.toPx() else 2.dp.toPx())
    }
}
