package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.PartSpan
import com.dcc.yawm.shared.day.Remind
import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.screen.AddDraft
import com.dcc.yawm.shared.screen.AddTaps
import com.dcc.yawm.shared.screen.Field
import com.dcc.yawm.shared.screen.Where
import com.dcc.yawm.shared.screen.addDraft
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.saveLabel
import com.dcc.yawm.shared.screen.saysLine
import kotlinx.datetime.LocalDate

/*
 * The add sheet and the sheet of one thing in «الألوان» (docs/design/yawm-colors-add.html, approved
 * 2026-11-05). The words decide and the colour answers at once: the sheet takes the colour of where the
 * thing goes; one sentence says what was understood, and the button names what will happen.
 */

/** The big one-line field the title is typed into, with a rule under it. */
@Composable
private fun BigField(value: String, hint: String, onChange: (String) -> Unit) {
    val pal = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Txt(hint, size = 26.sp, bold = true, color = pal.grey, lineHeight = 1.35f)
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(60)) },
            singleLine = true,
            textStyle = TextStyle(color = pal.ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = LocalFonts.current.ui, lineHeight = 35.sp),
            cursorBrush = SolidColor(pal.ink),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).onFocusChanged { focused = it.isFocused }.semantics { contentDescription = hint },
        )
    }
    Rule(strong = focused)
}

/** The alarm's time, large, with − and + five minutes. */
@Composable
private fun TimeStepper(at: Int, onStep: (Int) -> Unit) {
    val pal = LocalPalette.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        RoundStep("−", "أبكر 5 دقايق") { onStep(-5) }
        BasicText(
            clock(at),
            style = TextStyle(color = pal.ink, fontSize = 60.sp, fontFamily = LocalFonts.current.ui, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center),
        )
        RoundStep("+", "أتأخر 5 دقايق") { onStep(5) }
    }
}

@Composable
private fun RoundStep(sign: String, label: String, onClick: () -> Unit) {
    val pal = LocalPalette.current
    Box(
        Modifier.size(56.dp).clip(CircleShape).border(2.dp, pal.ink, CircleShape).clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Txt(sign, size = 26.sp, bold = true, lineHeight = 1.1f) }
}

private val AddRepeats = listOf(Repeat.ONCE, Repeat.DAILY, Repeat.WORKDAYS)
private val AlarmRepeats = listOf(Repeat.DAILY, Repeat.WORKDAYS, Repeat.ONCE)

@Composable
private fun RepeatSeg(chosen: Repeat, order: List<Repeat>, onPick: (Repeat) -> Unit) {
    FieldHeading("يتكرر؟")
    Seg(order.map { it.label }, order.indexOf(chosen)) { onPick(order[it]) }
}

@Composable
private fun RemindSeg(chosen: Remind, hasTime: Boolean, onPick: (Remind) -> Unit) {
    val order = if (hasTime) listOf(Remind.NONE, Remind.START, Remind.AT) else listOf(Remind.NONE, Remind.START)
    val names = order.map { when (it) { Remind.NONE -> "لا"; Remind.START -> "بأول القسم"; Remind.AT -> "بوكتها" } }
    FieldHeading("نبّهني؟")
    Seg(names, order.indexOf(chosen)) { onPick(order[it]) }
}

/** «أضف»: what to do, where, how often, and the reminder; or a new alarm. */
@Composable
fun BoxScope.AddSheet(
    alarmMode: Boolean, defaultPart: DayPart, spans: List<PartSpan>, nowMinute: Int, fallback: Field,
    close: () -> Unit, onSave: (AddDraft) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var taps by remember { mutableStateOf(AddTaps()) }
    val d = addDraft(text, alarmMode, taps, defaultPart, spans, nowMinute)
    val field = if (d.alarm) Field.NIGHT else (d.where as? Where.In)?.let { Field.of(it.part) } ?: fallback
    FieldSheet(field, if (d.alarm) "منبّه جديد" else "أضف", close) {
        val pal = LocalPalette.current
        BigField(text, if (alarmMode) "شنو المنبّه؟" else "شنو تريد تسوي؟") { text = it; taps = AddTaps() }
        if (text.isEmpty()) Txt(
            if (alarmMode) "اسم المنبّه إذا تريد، مثلاً: الدواء" else "اكتبها مثل ما تحچي: «زيارة خالي بعد المغرب»، «قرآن بعد الفجر كل يوم»، «منبّه الساعة 05:45»",
            size = 14.5.sp, color = pal.grey, lineHeight = 1.6f,
        )
        Txt(d.saysLine(), size = 18.sp, bold = true, lineHeight = 1.5f, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (d.alarm) {
            FieldHeading("يرن الساعة")
            TimeStepper(d.at ?: 0) { m -> taps = taps.copy(at = ((d.at ?: 0) + m + 1440) % 1440) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                Chip("أبكر ساعة", false, Modifier.width(120.dp)) { taps = taps.copy(at = ((d.at ?: 0) - 60 + 1440) % 1440) }
                Chip("أتأخر ساعة", false, Modifier.width(120.dp)) { taps = taps.copy(at = ((d.at ?: 0) + 60) % 1440) }
            }
            RepeatSeg(d.repeat, AlarmRepeats) { taps = taps.copy(repeat = it) }
        } else {
            FieldHeading("وين؟")
            WhereBar(d.where) { taps = taps.copy(where = it) }
            if (d.where != Where.Later) {
                RepeatSeg(d.repeat, AddRepeats) { taps = taps.copy(repeat = it) }
                RemindSeg(d.remind, d.at != null) { taps = taps.copy(remind = it) }
            }
        }
        BigButton(d.saveLabel(), Modifier.padding(top = 6.dp), enabled = d.alarm || d.title.isNotBlank()) { onSave(d) }
    }
}

/** One thing, in the colour of its part: done, move it, how often, its reminder, delete. */
@Composable
fun BoxScope.ThingSheet(
    x: Thing, date: LocalDate, where: Where?, whereText: String, field: Field, close: () -> Unit,
    onToggle: () -> Unit, onMove: (Where) -> Unit, onRemind: (Remind) -> Unit, onRepeat: (Repeat) -> Unit, onDelete: () -> Unit,
) {
    FieldSheet(field, x.title, close) {
        val pal = LocalPalette.current
        var sure by remember(x.id) { mutableStateOf(false) }
        Txt(x.title, size = 26.sp, bold = true, lineHeight = 1.35f)
        Txt(
            whereText + (x.at?.let { "، الساعة ${clock(it)}" + (x.end?.let { e -> " إلى ${clock(e)}" } ?: "") } ?: ""),
            size = 16.sp, color = pal.grey, lineHeight = 1.5f,
        )
        val done = x.doneOn(date)
        BigButton(if (done) "✓ خلّصتها، رجّعها" else "خلّصتها", outline = done, onClick = onToggle)
        if (x.repeat == Repeat.ONCE) {
            FieldHeading("انقلها لـ")
            WhereBar(where, onPick = onMove)
        }
        RepeatSeg(x.repeat, AddRepeats, onRepeat)
        RemindSeg(x.remind, x.at != null, onRemind)
        Box(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(CircleShape).background(if (sure) pal.card else Color.Transparent)
                .clickable { if (sure) onDelete() else sure = true }.semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Txt(if (sure) "متأكد؟ اضغط مرة ثانية حتى تنحذف" else "احذفها", size = 16.sp, bold = true, color = if (sure) pal.ink else pal.grey, align = TextAlign.Center, lineHeight = 1.3f)
        }
    }
}
