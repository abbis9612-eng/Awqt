package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.alarm.UserAlarm
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.Remind
import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.prayer.Prayer
import com.dcc.yawm.shared.screen.MonthNames
import com.dcc.yawm.shared.screen.PartOfDay
import com.dcc.yawm.shared.screen.PartsView
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.dayName
import com.dcc.yawm.shared.screen.durText
import com.dcc.yawm.shared.screen.prayerIcon

/*
 * The one page of «يومك بين الصلوات» (prototype v11, docs/design/yawm-prayers-v11.html): the part
 * you are in with its prayer and things, the parts to come, the night with every alarm, what has
 * passed (folded), tomorrow and «بعدين». Ticks and buttons are at least 48dp.
 */

/** The prayer green, darker on paper so the word reads (contrast above 4.5). */
private fun prayerText(dark: Boolean) = if (dark) Color(0xFF8FE0C0) else Color(0xFF145F49)
private fun prayerBg(dark: Boolean, paper: Color) = lerp(paper, Color(0xFF1E8768), if (dark) 0.16f else 0.09f)

fun partIcon(p: DayPart): String = prayerIcon(p.opens)

@Composable
fun DayPage(
    view: PartsView,
    t: Int,
    isToday: Boolean,
    prayed: Set<String>,
    hijri: String,
    wakeOn: Boolean,
    onTick: (Thing) -> Unit,
    onOpen: (Thing) -> Unit,
    onPray: (String) -> Unit,
    onAdd: (DayPart?) -> Unit,
    onAddAlarm: () -> Unit,
    onAlarm: (UserAlarm, Boolean) -> Unit,
    onWake: (Boolean) -> Unit,
    openSettings: () -> Unit,
    backToToday: (() -> Unit)?,
) {
    val pal = LocalPalette.current
    val date = view.date
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Display(dayName(date), 26.sp)
            Txt("${date.day} ${MonthNames[date.month.ordinal]} · $hijri", size = 14.5.sp, color = pal.grey, lineHeight = 1.5f)
        }
        if (backToToday != null) PillButton("رجوع لليوم", backToToday)
        else Box(
            Modifier.heightIn(min = 48.dp).clickable(onClick = openSettings).semantics { role = Role.Button }.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) { Txt("الإعدادات", size = 15.sp, bold = true, color = pal.soft) }
    }

    val asleep = isToday && view.asleep(t)
    val cur = if (isToday) view.current(t) else null
    when {
        asleep -> NightNow(view, t, wakeOn, onWake)
        cur != null -> NowPart(view, cur, t, prayed, onTick, onOpen, onPray, onAdd)
    }
    val upcoming = if (!isToday) view.parts else if (asleep) (if (t < view.parts.first().span.start) view.parts else emptyList()) else view.future(t)
    for (p in upcoming) PartBlock(view, p, prayed, onTick, onOpen)
    NightPart(view, wakeOn, onAlarm, onWake, onAddAlarm)

    val past = if (!isToday) emptyList() else if (asleep && t >= view.bedtime) view.parts else view.past(t)
    if (past.isNotEmpty()) {
        var open by rememberSaveable(date.toString()) { mutableStateOf(false) }
        Box(
            Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 48.dp)
                .border(1.5.dp, pal.line, RoundedCornerShape(14.dp))
                .clickable { open = !open }.semantics { role = Role.Button }.padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) { Txt(if (open) "خفّي اللي فات" else "✓ اللي فات: " + past.joinToString("، ") { it.part.label }, size = 15.sp, color = pal.soft) }
        if (open) for (p in past) PartBlock(view, p, prayed, onTick, onOpen)
    }
    if (view.tomorrow.isNotEmpty()) {
        Heading("باچر")
        for (x in view.tomorrow) ThingRow(x, date, onTick, onOpen)
    }
    if (view.later.isNotEmpty()) {
        Heading("بعدين")
        for (x in view.later) ThingRow(x, date, onTick, onOpen)
    }
    Spacer(Modifier.height(110.dp))
}

@Composable
private fun NowPart(
    view: PartsView, cur: PartOfDay, t: Int, prayed: Set<String>,
    onTick: (Thing) -> Unit, onOpen: (Thing) -> Unit, onPray: (String) -> Unit, onAdd: (DayPart?) -> Unit,
) {
    val pal = LocalPalette.current
    val s = cur.span
    val p = cur.part
    Column(
        Modifier.fillMaxWidth().padding(top = 14.dp).background(lerp(pal.paper, pal.ink, 0.04f), RoundedCornerShape(24.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Txt("هسه", size = 14.sp, bold = true, color = pal.grey, lineHeight = 1.4f)
        Display(p.label, 38.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SpanEnd(view.prayerName(p) ?: "الشروق", s.start)
            SpanEnd(p.endLabel, s.end)
        }
        Progress(((t - s.start).toFloat() / (s.end - s.start)).coerceIn(0f, 1f))
        Txt("باقي ${durText(s.end - t)} لـ${p.endLabel}.", size = 15.sp, color = pal.soft, lineHeight = 1.5f)
        view.prayerKey(p)?.let { key -> PrayerRow(view.prayerName(p)!!, p.opens, s.start, key in prayed) { onPray(key) } }
        if (cur.things.isEmpty()) Txt("ماكو شي بهالقسم. وقتك فاضي.", size = 15.sp, color = pal.grey)
        for (x in cur.things) ThingRow(x, view.date, onTick, onOpen)
        AddHere("+ أضف شي ${p.label}") { onAdd(p) }
    }
}

@Composable
private fun SpanEnd(name: String, at: Int) {
    val pal = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(name, size = 15.sp, color = pal.soft, lineHeight = 1.4f)
        Num(clock(at), size = 17.sp, color = pal.ink, bold = true)
    }
}

@Composable
private fun Progress(f: Float) {
    val pal = LocalPalette.current
    Box(Modifier.fillMaxWidth().height(10.dp).background(pal.faint, CircleShape)) {
        Box(Modifier.fillMaxWidth(f).height(10.dp).background(pal.ink, CircleShape))
    }
}

@Composable
private fun PrayerRow(name: String, prayer: Prayer, at: Int, done: Boolean, onPray: () -> Unit) {
    val pal = LocalPalette.current
    val fg = prayerText(pal.isDark)
    Row(
        Modifier.fillMaxWidth().background(prayerBg(pal.isDark, pal.paper), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Tile(prayerIcon(prayer), false, TileSize.XS)
        Txt("صلاة $name", size = 16.sp, modifier = Modifier.weight(1f), lineHeight = 1.4f)
        Num(clock(at), size = 17.sp, color = pal.ink)
        Box(
            Modifier.heightIn(min = 48.dp).background(if (done) fg else Color.Transparent, RoundedCornerShape(999.dp))
                .border(2.dp, fg, RoundedCornerShape(999.dp)).clickable(onClick = onPray)
                .semantics { role = Role.Checkbox; contentDescription = "صلّيت $name"; stateDescription = if (done) "صلّيت" else "بعد" }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) { Txt(if (done) "صلّيت ✓" else "صلّيت", size = 16.sp, bold = true, color = if (done) pal.paper else fg, lineHeight = 1.3f) }
    }
}

@Composable
fun ThingRow(x: Thing, date: kotlinx.datetime.LocalDate, onTick: (Thing) -> Unit, onOpen: (Thing) -> Unit) {
    val pal = LocalPalette.current
    val done = x.doneOn(date)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onOpen(x) }
            .semantics { role = Role.Button; contentDescription = x.title + (x.at?.let { "، الساعة ${clock(it)}" } ?: "") },
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clickable { onTick(x) }
                .semantics { role = Role.Checkbox; contentDescription = "خلّصت ${x.title}"; stateDescription = if (done) "خلصت" else "باقية" },
            contentAlignment = Alignment.Center,
        ) { Check(done) }
        Txt(x.title, size = 17.sp, color = if (done) pal.grey else pal.ink, modifier = Modifier.weight(1f), lineHeight = 1.45f)
        if (x.repeat != Repeat.ONCE) Badge("↻ ${x.repeat.label}")
        x.at?.let { Badge(clock(it) + (x.end?.let { e -> "–" + clock(e) } ?: ""), num = true) }
        if (x.remind != Remind.NONE) YIcon("hourglass", 18.dp, pal.grey, pal.grey)
    }
}

@Composable
private fun Badge(text: String, num: Boolean = false) {
    val pal = LocalPalette.current
    Box(Modifier.border(1.dp, pal.line, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        if (num) Num(text, size = 15.sp, color = pal.soft) else Txt(text, size = 14.sp, color = pal.soft, lineHeight = 1.4f)
    }
}

@Composable
private fun AddHere(label: String, onClick: () -> Unit) {
    val pal = LocalPalette.current
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).border(1.5.dp, pal.line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Txt(label, size = 16.sp, color = pal.soft, align = TextAlign.Center) }
}

@Composable
private fun PartHead(icon: String, title: String, at: String?) {
    val pal = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Tile(icon, false, TileSize.XS)
        Display(title, 18.sp, modifier = Modifier.weight(1f))
        if (at != null) Num(at, size = 17.sp, color = pal.soft)
    }
}

@Composable
private fun PartBlock(view: PartsView, p: PartOfDay, prayed: Set<String>, onTick: (Thing) -> Unit, onOpen: (Thing) -> Unit) {
    val pal = LocalPalette.current
    PartHead(partIcon(p.part), p.part.label + (view.prayerKey(p.part)?.takeIf { it in prayed }?.let { " ✓" } ?: ""), clock(p.span.start))
    if (p.things.isEmpty()) Txt("فاضي", size = 15.sp, color = pal.grey, modifier = Modifier.padding(start = 38.dp))
    for (x in p.things) Box(Modifier.padding(start = 20.dp)) { ThingRow(x, view.date, onTick, onOpen) }
}

@Composable
private fun NightNow(view: PartsView, t: Int, wakeOn: Boolean, onWake: (Boolean) -> Unit) {
    val pal = LocalPalette.current
    val total = (view.wake - view.bedtime).coerceAtLeast(1)
    val gone = (t - view.bedtime).coerceIn(0, total)
    val beforeBed = t < view.bedtime
    Column(
        Modifier.fillMaxWidth().padding(top = 14.dp).background(lerp(pal.paper, pal.ink, 0.04f), RoundedCornerShape(24.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Txt("هسه", size = 14.sp, bold = true, color = pal.grey, lineHeight = 1.4f)
        Display("الليل", 38.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SpanEnd(if (beforeBed) "تنام" else "نمت", view.bedtime)
            SpanEnd("تصحى", view.wake)
        }
        Progress(if (beforeBed) 0f else gone.toFloat() / total)
        Txt(
            "باقي ${durText((view.wake - t).coerceAtLeast(0))} للصحيان" + if (view.day.plan.qiyam) "، لصلاة الليل قبل الفجر." else ".",
            size = 15.sp, color = pal.soft, lineHeight = 1.5f,
        )
        ListRow(divider = false) {
            RowText("منبّه الصحيان ${clock(view.wake)}", "يتحرك ويّا الفجر كل يوم")
            InkSwitch(wakeOn, "منبّه الصحيان", onWake)
        }
    }
}

@Composable
private fun NightPart(view: PartsView, wakeOn: Boolean, onAlarm: (UserAlarm, Boolean) -> Unit, onWake: (Boolean) -> Unit, onAddAlarm: () -> Unit) {
    val pal = LocalPalette.current
    PartHead("bed", "الليل والمنبّهات", clock(view.bedtime))
    Txt(
        "تنام ${clock(view.bedtime)} وتصحى ${clock(view.wake)}" + if (view.day.plan.qiyam) " لصلاة الليل" else "",
        size = 15.sp, color = pal.soft, modifier = Modifier.padding(start = 38.dp),
    )
    AlarmLine(clock(view.wake), "منبّه الصحيان", "يتحرك ويّا الفجر كل يوم", wakeOn, onWake)
    for (a in view.alarms) {
        val days = when {
            a.onceOn != null -> "مرة وحدة"
            a.days.size == 7 -> "كل يوم"
            else -> daysText(a.days)
        }
        AlarmLine(clock(a.minute), a.label, days, a.on) { onAlarm(a, it) }
    }
    Box(Modifier.padding(start = 38.dp, top = 6.dp)) { AddHere("+ منبّه جديد", onAddAlarm) }
}

@Composable
private fun AlarmLine(time: String, label: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val pal = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 38.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Num(time, size = 28.sp, color = if (on) pal.ink else pal.grey, bold = true, modifier = Modifier.width(86.dp))
        Column(Modifier.weight(1f)) {
            Txt(label, size = 16.sp, color = if (on) pal.ink else pal.grey, lineHeight = 1.4f)
            Txt(sub, size = 13.5.sp, color = pal.grey, lineHeight = 1.4f)
        }
        InkSwitch(on, "$label $time", onChange)
    }
}
