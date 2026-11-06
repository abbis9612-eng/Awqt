package com.dcc.yawm.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.PartSpan
import com.dcc.yawm.shared.day.Remind
import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.day.partAt
import com.dcc.yawm.shared.screen.AddDraft
import com.dcc.yawm.shared.screen.AddTaps
import com.dcc.yawm.shared.screen.Where
import com.dcc.yawm.shared.screen.addDraft
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.ringsLine
import com.dcc.yawm.shared.screen.whereName

/*
 * «أضف» in the polished style the owner approved on 2026-11-06 (docs/design/yawm-polish-add.html):
 * a calm surface, white cards with a soft shadow, an icon on every row, a clear type scale, and the
 * part's colour only as an accent. «بساعة محددة» sets a clock time with a large picker.
 */

@Immutable
data class Calm(
    val surface: Color, val card: Color, val ink: Color, val ink2: Color, val ink3: Color, val hair: Color, val soft: Color, val dark: Boolean,
)

val CalmLight = Calm(Color(0xFFF7F8FA), Color.White, Color(0xFF14171C), Color(0xFF4F5663), Color(0xFF636B78), Color(0xFFE3E6EB), Color(0xFFEEF0F4), false)
val CalmDark = Calm(Color(0xFF121418), Color(0xFF1B1E24), Color(0xFFF1F3F6), Color(0xFFB4BBC6), Color(0xFF9AA2AF), Color(0xFF2A2F37), Color(0xFF262A32), true)

val LocalCalm = staticCompositionLocalOf { CalmLight }

/** The accent of a part: a deeper tone of its field colour that white text reads on at 4.5:1 or more. */
val PartAccent: Map<DayPart, Color> = mapOf(
    DayPart.FAJR to Color(0xFF2B4A86),
    DayPart.MORNING to Color(0xFF2F6FA8),
    DayPart.DHUHR to Color(0xFF8A6508),
    DayPart.ASR to Color(0xFFA35512),
    DayPart.MAGHRIB to Color(0xFFB23F2A),
    DayPart.ISHA to Color(0xFF33407A),
)

/** «باچر», «بعدين» and alarms: a quiet slate. */
val NeutralAccent = Color(0xFF4B5563)

/** The app's icon for each part (docs/design/icons). */
fun calmPartIcon(p: DayPart): String = when (p) {
    DayPart.FAJR -> "fajr"; DayPart.MORNING -> "tea"; DayPart.DHUHR -> "dhuhr"
    DayPart.ASR -> "asr"; DayPart.MAGHRIB -> "maghrib"; DayPart.ISHA -> "isha"
}

private val Ease = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/** Common things to start from (titles only, no religious text), each with one of the app's icons. */
private val Ideas = listOf(
    "قرآن بعد الفجر" to "rahl", "رياضة" to "dumbbell", "قراءة" to "book", "مشوار" to "steps",
    "اتصال بالأهل" to "phone", "دراسة" to "nib", "شغل البيت" to "house", "تسوّق" to "case",
)

private val QuickTimes = listOf(7 * 60, 8 * 60 + 30, 13 * 60, 16 * 60 + 30, 19 * 60, 21 * 60)

@Composable
private fun CText(text: String, size: TextUnit, color: Color, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start, lh: Float = 1.45f) {
    BasicText(text, modifier, style = TextStyle(color = color, fontSize = size, fontWeight = weight, fontFamily = LocalFonts.current.ui, textAlign = align, lineHeight = size * lh))
}

/** A white card with a soft shadow. */
private fun Modifier.calmCard(c: Calm, radius: Int = 18): Modifier =
    this.shadow(if (c.dark) 0.dp else 6.dp, RoundedCornerShape(radius.dp), clip = false, ambientColor = Color(0x14141720), spotColor = Color(0x30141720))
        .clip(RoundedCornerShape(radius.dp)).background(c.card)

/** The sheet itself: dimmed screen, a calm surface rising from the bottom. */
@Composable
fun BoxScope.CalmSheet(label: String, close: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val reduced = LocalReducedMotion.current
    val c = if (isSystemInDarkTheme()) CalmDark else CalmLight
    val shown = remember { MutableTransitionState(reduced) }
    shown.targetState = true
    Box(
        Modifier.fillMaxSize().background(Color(0x61000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close),
    )
    AnimatedVisibility(
        shown, Modifier.align(Alignment.BottomCenter),
        enter = if (reduced) fadeIn(tween(0)) else slideInVertically(spring(dampingRatio = 0.86f, stiffness = 420f)) { it } + fadeIn(tween(120)),
    ) {
        val fonts = LocalFonts.current
        CompositionLocalProvider(LocalCalm provides c, LocalFonts provides fonts.copy(naskh = fonts.ui, amiri = fonts.ui)) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 820.dp)
                    .background(c.surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .semantics { paneTitle = label }
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(38.dp, 5.dp).background(c.hair, RoundedCornerShape(3.dp)))
                }
                content()
            }
        }
    }
}

/** «إلغاء» · title · «حفظ». */
@Composable
private fun TopBar(title: String, accent: Color, canSave: Boolean, close: () -> Unit, save: () -> Unit) {
    val c = LocalCalm.current
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Box(Modifier.align(Alignment.CenterStart).heightIn(min = 48.dp).clickable(onClick = close).semantics { role = Role.Button }.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
            CText("إلغاء", 16.sp, c.ink2, FontWeight.Medium)
        }
        CText(title, 17.sp, c.ink, FontWeight.Bold, Modifier.align(Alignment.Center))
        Box(
            Modifier.align(Alignment.CenterEnd).heightIn(min = 48.dp).clickable(enabled = canSave, onClick = save).semantics { role = Role.Button }.padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) { CText("حفظ", 16.sp, if (canSave) accent else c.ink3, FontWeight.Bold) }
    }
}

@Composable
private fun IconTile(icon: String, accent: Color?, size: Int = 34) {
    val c = LocalCalm.current
    val bg by animateColorAsState(accent ?: c.soft, tween(300, easing = Ease), label = "tile")
    Box(Modifier.size(size.dp).background(bg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        val ink = if (accent != null) Color.White else c.ink2
        YIcon(icon, (size * 0.58f).dp, ink, ink)
    }
}

/** The iOS-like switch of the polished sheet. */
@Composable
private fun CalmSwitch(on: Boolean, label: String, accent: Color, onChange: (Boolean) -> Unit) {
    val c = LocalCalm.current
    val x by animateDpAsState(if (on) 20.dp else 0.dp, tween(if (LocalReducedMotion.current) 0 else 240, easing = Ease), label = "knob")
    val track by animateColorAsState(if (on) accent else c.hair, tween(200), label = "track")
    Box(
        Modifier.size(width = 56.dp, height = 48.dp).clickable { onChange(!on) }
            .semantics { role = Role.Switch; contentDescription = label; stateDescription = if (on) "شغّال" else "مطفي" },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(width = 51.dp, height = 31.dp).background(track, CircleShape), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.offset(x = x).padding(2.dp).size(27.dp).shadow(2.dp, CircleShape).background(Color.White, CircleShape))
        }
    }
}

/** Choices in a soft bar with a sliding white thumb. */
@Composable
private fun CalmSeg(options: List<String>, chosen: Int, onChoose: (Int) -> Unit) {
    val c = LocalCalm.current
    BoxWithConstraints(Modifier.fillMaxWidth().background(c.soft, RoundedCornerShape(12.dp)).padding(3.dp)) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * chosen.coerceAtLeast(0), tween(if (LocalReducedMotion.current) 0 else 260, easing = Ease), label = "thumb")
        if (chosen >= 0) Box(Modifier.offset(x = x).width(w).height(42.dp).align(Alignment.CenterStart).shadow(1.dp, RoundedCornerShape(9.dp)).background(c.card, RoundedCornerShape(9.dp)))
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, o ->
                Box(
                    Modifier.weight(1f).height(42.dp).clickable { onChoose(i) }.semantics { role = Role.RadioButton; selected = i == chosen; contentDescription = o },
                    contentAlignment = Alignment.Center,
                ) { CText(o, 14.5.sp, if (i == chosen) c.ink else c.ink2, FontWeight.SemiBold, align = TextAlign.Center) }
            }
        }
    }
}

/** A row of the details card: icon, name, a small line, and what is set now (or a switch). */
@Composable
private fun DetailRow(icon: String, accent: Color?, title: String, sub: String?, value: String?, onClick: (() -> Unit)?, trailing: (@Composable () -> Unit)? = null) {
    val c = LocalCalm.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 62.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick).semantics { role = Role.Button } else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, accent)
        Column(Modifier.weight(1f)) {
            CText(title, 16.sp, c.ink, FontWeight.SemiBold, lh = 1.3f)
            if (sub != null) CText(sub, 13.sp, c.ink3, lh = 1.35f)
        }
        if (value != null) CText(value, 15.sp, c.ink2, FontWeight.Medium)
        trailing?.invoke()
    }
}

@Composable
private fun Hair() {
    val c = LocalCalm.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hair))
}

/** One of the six parts as a card: icon, name, its hours today. */
@Composable
private fun PartCard(span: PartSpan, chosen: Boolean, onClick: () -> Unit) {
    val c = LocalCalm.current
    val acc = PartAccent.getValue(span.part)
    val bg by animateColorAsState(if (chosen) acc else c.soft, tween(220, easing = Ease), label = "part")
    Column(
        Modifier.width(100.dp).heightIn(min = 100.dp).clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick)
            .semantics { role = Role.RadioButton; selected = chosen; contentDescription = span.part.label }
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(28.dp).background(if (chosen) Color.White.copy(alpha = 0.18f) else acc.copy(alpha = 0.14f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
            val ink = if (chosen) Color.White else acc
            YIcon(calmPartIcon(span.part), 18.dp, ink, ink)
        }
        Spacer(Modifier.height(8.dp))
        CText(span.part.label, 14.5.sp, if (chosen) Color.White else c.ink, FontWeight.Bold, lh = 1.25f)
        CText("${clock(span.start)} – ${clock(span.end)}", 12.sp, if (chosen) Color.White.copy(alpha = 0.9f) else c.ink3, lh = 1.25f)
    }
}

@Composable
private fun SoftPill(text: String, on: Boolean, onClick: () -> Unit) {
    val c = LocalCalm.current
    Box(
        Modifier.heightIn(min = 44.dp).clip(CircleShape).background(if (on) c.ink else c.soft).clickable(onClick = onClick)
            .semantics { role = Role.RadioButton; selected = on; contentDescription = text }.padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { CText(text, 14.5.sp, if (on) c.surface else c.ink2, FontWeight.SemiBold) }
}

/** The clock time, large, with hours and minutes each moved up or down. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClockPicker(at: Int, accent: Color, onChange: (Int) -> Unit) {
    val c = LocalCalm.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            ClockUnit((at / 60).toString().padStart(2, '0'), "ساعة", "ساعة أكثر", "ساعة أقل", { onChange((at + 60) % 1440) }, { onChange((at - 60 + 1440) % 1440) })
            CText(":", 40.sp, c.ink3, FontWeight.SemiBold, Modifier.padding(bottom = 22.dp))
            ClockUnit((at % 60).toString().padStart(2, '0'), "دقيقة", "5 دقايق أكثر", "5 دقايق أقل", { onChange((at + 5) % 1440) }, { onChange((at - 5 + 1440) % 1440) })
        }
    }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (q in QuickTimes) {
            val on = q == at
            Box(
                Modifier.heightIn(min = 40.dp).clip(CircleShape).background(if (on) accent else c.soft).clickable { onChange(q) }
                    .semantics { role = Role.RadioButton; selected = on; contentDescription = clock(q) }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { CText(clock(q), 14.sp, if (on) Color.White else c.ink2, FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun ClockUnit(value: String, label: String, upLabel: String, downLabel: String, up: () -> Unit, down: () -> Unit) {
    val c = LocalCalm.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Step("▲", upLabel, up)
        CText(value, 46.sp, c.ink, FontWeight.SemiBold, align = TextAlign.Center, lh = 1.1f)
        Step("▼", downLabel, down)
        CText(label, 12.sp, c.ink3, FontWeight.SemiBold)
    }
}

@Composable
private fun Step(glyph: String, label: String, onClick: () -> Unit) {
    val c = LocalCalm.current
    Box(
        Modifier.size(width = 52.dp, height = 44.dp).clip(RoundedCornerShape(12.dp)).background(c.soft).clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { CText(glyph, 14.sp, c.ink2, FontWeight.Bold) }
}

/** The big button at the bottom, in the accent. */
@Composable
private fun AccentButton(text: String, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(accent, tween(300, easing = Ease), label = "go")
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(16.dp), spotColor = accent)
            .clip(RoundedCornerShape(16.dp)).background(if (enabled) bg else bg.copy(alpha = 0.4f)).clickable(onClick = onClick)
            .semantics { role = Role.Button }.padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { CText(text, 17.sp, Color.White, FontWeight.Bold, align = TextAlign.Center) }
}

/** «حطّها بالصبح», «حطّها الساعة 19:05», «حطّها باچر». */
fun polishedSaveLabel(d: AddDraft): String = when {
    d.alarm -> "احفظ المنبّه"
    d.at != null && d.where != Where.Later -> "حطّها الساعة ${clock(d.at)}"
    else -> "حطّها " + whereName(d.where).let { if (it.startsWith("ال")) "ب$it" else it }
}

/** The line under the details: what will happen, in words. */
fun polishedSays(d: AddDraft): String = when {
    d.alarm -> d.ringsLine()
    d.where == Where.Later -> "«${d.title}» تنحط بعدين، بدون وكت."
    else -> "«${d.title}» " + (d.at?.let { "الساعة ${clock(it)}، " } ?: "") + whereName(d.where) + "، ${d.repeat.label}" +
        when (d.remind) { Remind.NONE -> ""; Remind.START -> "، وأذكّرك بأوله"; Remind.AT -> "، وأذكّرك بوكتها" } + "."
}

/** «أضف»: the regular way to add (the assistant is the other). */
@Composable
fun BoxScope.PolishedAddSheet(
    alarmMode: Boolean, defaultPart: DayPart, spans: List<PartSpan>, nowMinute: Int,
    close: () -> Unit, openAssistant: (() -> Unit)?, onSave: (AddDraft) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var taps by remember { mutableStateOf(AddTaps()) }
    var open by remember { mutableStateOf(if (alarmMode) "" else "when") }
    var asked by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val d = addDraft(text, alarmMode, taps, defaultPart, spans, nowMinute)
    val part = (d.where as? Where.In)?.part
    val accent = if (d.alarm || part == null) NeutralAccent else PartAccent.getValue(part)
    val valid = d.alarm || d.title.isNotBlank()
    fun save() { if (valid) onSave(d) else { asked = true; runCatching { focus.requestFocus() } } }
    fun setAt(m: Int) {
        // A clock time puts the thing in the part of that time, unless it is for tomorrow.
        val w = if (d.where == Where.Tomorrow) Where.Tomorrow else partAt(m, spans)?.let { Where.In(it.part) } ?: d.where
        taps = taps.copy(at = m, noTime = false, where = w, remind = if (d.remind == Remind.NONE) Remind.NONE else Remind.AT)
    }
    CalmSheet(if (d.alarm) "منبّه جديد" else "شغلة جديدة", close) {
        val c = LocalCalm.current
        TopBar(if (d.alarm) "منبّه جديد" else "شغلة جديدة", accent, valid, close, ::save)

        // What to do.
        Column(Modifier.fillMaxWidth().calmCard(c).padding(horizontal = 16.dp, vertical = 10.dp)) {
            CText(if (alarmMode) "اسم المنبّه (إذا تريد)" else "شنو تريد تسوي؟", 13.sp, c.ink3, FontWeight.SemiBold)
            LaunchedEffect(Unit) { if (!alarmMode) runCatching { focus.requestFocus() } }
            Box(Modifier.fillMaxWidth().heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) CText(if (alarmMode) "مثلاً: الدواء" else "اكتب هنا…", 20.sp, c.ink3, FontWeight.Medium)
                BasicTextField(
                    text, { text = it.take(60); taps = AddTaps(); asked = false }, singleLine = true,
                    textStyle = TextStyle(color = c.ink, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, fontFamily = LocalFonts.current.ui, lineHeight = 30.sp),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "شنو تريد تسوي؟" },
                )
            }
        }
        if (!alarmMode) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (openAssistant != null) {
                    Row(
                        Modifier.heightIn(min = 42.dp).clip(CircleShape).background(PartAccent.getValue(DayPart.MORNING)).clickable(onClick = openAssistant)
                            .semantics { role = Role.Button }.padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
                    ) { CText("✦ گول للمساعد", 14.5.sp, Color.White, FontWeight.SemiBold) }
                }
                for ((idea, icon) in Ideas) {
                    Row(
                        Modifier.heightIn(min = 42.dp).calmCard(c, 21).clickable { text = idea; taps = AddTaps(); asked = false }
                            .semantics { role = Role.Button }.padding(horizontal = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        YIcon(icon, 17.dp, c.ink3, c.ink3)
                        CText(idea, 14.5.sp, c.ink2, FontWeight.Medium)
                    }
                }
            }
        }
        if (asked) CText("اكتب أول شنو تريد تسوي، أو اختار فكرة.", 14.5.sp, accent, FontWeight.Bold)

        // The details.
        Column(Modifier.fillMaxWidth().calmCard(c)) {
            if (!d.alarm) {
                val span = part?.let { p -> spans.firstOrNull { it.part == p } }
                DetailRow(
                    icon = part?.let { calmPartIcon(it) } ?: if (d.where == Where.Tomorrow) "week" else "hourglass",
                    accent = accent, title = "وكتها",
                    sub = when (d.where) { Where.Later -> "بدون وكت، تلكاها تحت يومك"; Where.Tomorrow -> "باچر"; else -> span?.let { "${clock(it.start)} – ${clock(it.end)}" } },
                    value = whereName(d.where), onClick = { open = if (open == "when") "" else "when" },
                )
                if (open == "when") Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (s in spans) PartCard(s, part == s.part) {
                            taps = taps.copy(where = Where.In(s.part), at = if (d.at != null) s.start + 30 else null, noTime = d.at == null)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoftPill("باچر", d.where == Where.Tomorrow) { taps = taps.copy(where = Where.Tomorrow) }
                        SoftPill("بعدين، بدون وكت", d.where == Where.Later) { taps = taps.copy(where = Where.Later, noTime = true, at = null) }
                    }
                }
                if (d.where != Where.Later) {
                    Hair()
                    DetailRow(
                        "hourglass", if (d.at != null) accent else null, "بساعة محددة",
                        d.at?.let { "الساعة ${clock(it)}" } ?: "لا، بأي وكت بالقسم", null, null,
                    ) {
                        CalmSwitch(d.at != null, "بساعة محددة", accent) { on ->
                            if (on) setAt(spans.firstOrNull { it.part == (part ?: defaultPart) }?.let { it.start + 30 } ?: (16 * 60 + 30))
                            else taps = taps.copy(at = null, noTime = true, remind = if (d.remind == Remind.AT) Remind.START else d.remind)
                        }
                    }
                    d.at?.let { at ->
                        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ClockPicker(at, accent, ::setAt)
                            CText(
                                if (d.where == Where.Tomorrow) "باچر، الساعة ${clock(at)}" else "هاي الساعة تصير ${whereName(d.where)}",
                                13.5.sp, c.ink2, align = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            } else {
                DetailRow("hourglass", accent, "يرن الساعة", null, clock(d.at ?: 0), null)
                Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    ClockPicker(d.at ?: 0, accent) { taps = taps.copy(at = it) }
                }
            }
            if (d.where != Where.Later) {
                Hair()
                val order = if (d.alarm) listOf(Repeat.DAILY, Repeat.WORKDAYS, Repeat.ONCE) else listOf(Repeat.ONCE, Repeat.DAILY, Repeat.WORKDAYS)
                DetailRow("week", null, "يتكرر", null, d.repeat.label, { open = if (open == "rep") "" else "rep" })
                if (open == "rep") Box(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    CalmSeg(order.map { it.label }, order.indexOf(d.repeat)) { taps = taps.copy(repeat = order[it]) }
                }
            }
            if (!d.alarm && d.where != Where.Later) {
                Hair()
                DetailRow(
                    "seal", null, "ذكّرني",
                    when (d.remind) { Remind.NONE -> "بدون تنبيه"; Remind.START -> "بأول القسم، بإشعار هادي"; Remind.AT -> "الساعة ${clock(d.at ?: 0)} بالضبط" },
                    null, null,
                ) {
                    CalmSwitch(d.remind != Remind.NONE, "ذكّرني", accent) { on ->
                        taps = taps.copy(remind = if (!on) Remind.NONE else if (d.at != null) Remind.AT else Remind.START)
                    }
                }
            }
        }
        if (d.alarm) CText(d.ringsLine(), 14.sp, c.ink2, modifier = Modifier.padding(horizontal = 4.dp).semantics { liveRegion = LiveRegionMode.Polite })
        else if (d.title.isNotBlank()) CText(polishedSays(d), 14.sp, c.ink2, modifier = Modifier.padding(horizontal = 4.dp).semantics { liveRegion = LiveRegionMode.Polite })
        AccentButton(if (valid) polishedSaveLabel(d) else "اكتب شنو تريد تسوي", accent, valid, ::save)
        Spacer(Modifier.height(4.dp))
    }
}
