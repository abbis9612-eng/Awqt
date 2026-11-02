package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.PartSpan
import com.dcc.yawm.shared.day.Remind
import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.screen.AddDraft
import com.dcc.yawm.shared.screen.AddTaps
import com.dcc.yawm.shared.screen.AllWheres
import com.dcc.yawm.shared.screen.Where
import com.dcc.yawm.shared.screen.addDraft
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.ringsLine
import com.dcc.yawm.shared.screen.whereName
import kotlinx.datetime.LocalDate

/*
 * The add sheet and the sheet of one thing (prototype v11). The words decide; the chips show what
 * they decided and let the user change it with one tap.
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.AddSheetBody(alarmMode: Boolean, defaultPart: DayPart, spans: List<PartSpan>, nowMinute: Int, onSave: (AddDraft) -> Unit) {
    val pal = LocalPalette.current
    var text by remember { mutableStateOf("") }
    var taps by remember { mutableStateOf(AddTaps()) }
    val d = addDraft(text, alarmMode, taps, defaultPart, spans, nowMinute)
    Txt(if (d.alarm) "منبّه جديد" else "شنو تريد تسوي؟", size = 21.sp, bold = true)
    Txt(
        if (alarmMode) "اسم المنبّه إذا تريد، مثلاً: الدواء" else "اكتبها مثل ما تحجي: «زيارة خالي بعد المغرب»، «قرآن بعد الفجر كل يوم»، «منبّه الساعة 05:45»",
        size = 14.5.sp, color = pal.soft, lineHeight = 1.7f,
    )
    Field(text, if (alarmMode) "شنو المنبّه؟" else "شنو تريد تسوي؟") { text = it; taps = AddTaps() }
    if (d.alarm) {
        Txt("يرن الساعة", size = 16.sp, bold = true, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Stepper(
                clock(d.at ?: 0), "أبكر 5 دقايق", "أتأخر 5 دقايق",
                onMinus = { taps = taps.copy(at = ((d.at ?: 0) - 5 + 1440) % 1440) },
                onPlus = { taps = taps.copy(at = ((d.at ?: 0) + 5) % 1440) },
            )
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            Chip("أبكر ساعة", false) { taps = taps.copy(at = ((d.at ?: 0) - 60 + 1440) % 1440) }
            Chip("أتأخر ساعة", false) { taps = taps.copy(at = ((d.at ?: 0) + 60) % 1440) }
        }
        RepeatChips(d.repeat, listOf(Repeat.DAILY, Repeat.WORKDAYS, Repeat.ONCE)) { taps = taps.copy(repeat = it) }
        Txt(
            d.ringsLine(), size = 15.sp, color = pal.soft, lineHeight = 1.6f,
            modifier = Modifier.fillMaxWidth().background(lerp(pal.paper, pal.ink, 0.05f), RoundedCornerShape(14.dp)).padding(10.dp),
        )
        InkButton("احفظ المنبّه", Modifier.padding(top = 6.dp)) { onSave(d) }
        return
    }
    Txt("وين؟", size = 16.sp, bold = true, modifier = Modifier.padding(top = 6.dp))
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (w in AllWheres) Chip(whereName(w), w == d.where) { taps = taps.copy(where = w) }
    }
    d.at?.let { Txt("بساعة ثابتة: ${clock(it)}", size = 15.sp, color = pal.soft) }
    if (d.where != Where.Later) {
        RepeatChips(d.repeat, listOf(Repeat.ONCE, Repeat.DAILY, Repeat.WORKDAYS)) { taps = taps.copy(repeat = it) }
        Txt("نبّهني؟", size = 16.sp, bold = true, modifier = Modifier.padding(top = 6.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("لا", d.remind == Remind.NONE) { taps = taps.copy(remind = Remind.NONE) }
            Chip("بأول القسم", d.remind == Remind.START) { taps = taps.copy(remind = Remind.START) }
            if (d.at != null) Chip("بوكتها", d.remind == Remind.AT) { taps = taps.copy(remind = Remind.AT) }
        }
    }
    InkButton("حطّها", Modifier.padding(top = 6.dp)) { if (d.title.isNotBlank()) onSave(d) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatChips(chosen: Repeat, order: List<Repeat>, onPick: (Repeat) -> Unit) {
    Txt("يتكرر؟", size = 16.sp, bold = true, modifier = Modifier.padding(top = 6.dp))
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in order) Chip(r.label, r == chosen) { onPick(r) }
    }
}

/** One thing: done, move it, its reminder, how often, delete. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.ThingSheetBody(
    x: Thing, date: LocalDate, where: String,
    onToggle: () -> Unit, onMove: (Where) -> Unit, onRemind: (Remind) -> Unit, onRepeat: (Repeat) -> Unit, onDelete: () -> Unit,
) {
    val pal = LocalPalette.current
    var sure by remember(x.id) { mutableStateOf(false) }
    Txt(x.title, size = 21.sp, bold = true, lineHeight = 1.45f)
    Txt(
        where + (x.at?.let { " · الساعة ${clock(it)}" + (x.end?.let { e -> " إلى ${clock(e)}" } ?: "") } ?: "") +
            (if (x.repeat != Repeat.ONCE) " · ${x.repeat.label}" else ""),
        size = 15.sp, color = pal.soft,
    )
    InkButton(if (x.doneOn(date)) "رجّعها، ما خلصت" else "خلّصت ✓", Modifier.padding(top = 8.dp), onClick = onToggle)
    if (x.repeat == Repeat.ONCE) {
        Txt("انقلها لـ", size = 16.sp, bold = true, modifier = Modifier.padding(top = 8.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (w in AllWheres) Chip(whereName(w), false) { onMove(w) }
        }
    }
    RepeatChips(x.repeat, listOf(Repeat.ONCE, Repeat.DAILY, Repeat.WORKDAYS), onRepeat)
    Txt("نبّهني؟", size = 16.sp, bold = true, modifier = Modifier.padding(top = 6.dp))
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip("لا", x.remind == Remind.NONE) { onRemind(Remind.NONE) }
        Chip("بأول القسم", x.remind == Remind.START) { onRemind(Remind.START) }
        if (x.at != null) Chip("بوكتها", x.remind == Remind.AT) { onRemind(Remind.AT) }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
        PillButton(if (sure) "اضغط مرة ثانية حتى تنحذف" else "احذفها") { if (sure) onDelete() else sure = true }
    }
}
