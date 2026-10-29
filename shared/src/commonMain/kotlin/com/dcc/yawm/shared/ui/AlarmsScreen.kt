package com.dcc.yawm.shared.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.alarm.AlertStyle
import com.dcc.yawm.shared.alarm.DeviceStatus
import com.dcc.yawm.shared.alarm.ItemAlert
import com.dcc.yawm.shared.alarm.PrayerAlert
import com.dcc.yawm.shared.alarm.UserAlarm
import com.dcc.yawm.shared.alarm.prayerName
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.data.cleanTitle
import com.dcc.yawm.shared.prayer.Prayer
import com.dcc.yawm.shared.screen.DayView
import com.dcc.yawm.shared.screen.clock
import kotlinx.datetime.DayOfWeek

/** Saturday first, the way the week is spoken here. */
val SatFirst = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

fun dayFull(d: DayOfWeek) = com.dcc.yawm.shared.screen.DayNames[d.ordinal]

/** «كل يوم», «الأحد إلى الخميس», or the days by name. */
fun daysText(days: Set<DayOfWeek>): String {
    if (days.size == 7) return "كل يوم"
    if (days.isEmpty()) return "ولا يوم"
    val idx = days.map { SatFirst.indexOf(it) }.sorted()
    return if (idx.size >= 3 && idx.last() - idx.first() == idx.size - 1) "${dayFull(SatFirst[idx.first()])} إلى ${dayFull(SatFirst[idx.last()])}"
    else idx.joinToString("، ") { dayFull(SatFirst[it]) }
}

/** «المنبّه»: your alarms, the wake alarm, the daily summaries, item reminders and prayer alerts. */
@Composable
fun AlarmsScreen(
    settings: UserSettings,
    day: DayView,
    t: Int,
    status: DeviceStatus,
    device: com.dcc.yawm.shared.alarm.DeviceControls,
    save: (UserSettings) -> Unit,
    editing: String?,
    setEditing: (String?) -> Unit,
    openSounds: () -> Unit,
) {
    val pal = LocalPalette.current
    val a = settings.alerts
    fun put(f: (com.dcc.yawm.shared.alarm.AlertPrefs) -> com.dcc.yawm.shared.alarm.AlertPrefs) = save(settings.copy(alerts = f(a)))
    val todays = a.alarms.filter { it.on }.map { it.minute to it.label }.let { l -> if (settings.wakeAlarm) l + (day.plan.wake.mod(1440) to "منبّه الصحيان") else l }
    val next = todays.filter { it.first > t.mod(1440) }.minByOrNull { it.first } ?: todays.minByOrNull { it.first }
    InkWord("المنبّه", 0f, 112.dp, next?.let { "المنبّه الجاي ${it.second} ${clock(it.first)}" } ?: "ما في منبّه", Modifier.padding(top = 18.dp))
    Txt(next?.let { "الجاي: ${it.second}، الساعة ${clock(it.first)}" } ?: "ما عندك منبّه", size = 18.sp, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    InkCard("hourglass", "شكل التنبيهات وأصواتها", "اسمعها، وشوف شلون تطلع وشلون تمشي سوا", onClick = openSounds)

    Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt("منبّهاتك", size = 15.sp, bold = true, color = pal.grey, modifier = Modifier.weight(1f))
        PillButton("+ منبّه") {
            val id = a.nextAlarmId()
            put { it.copy(alarms = it.alarms + UserAlarm(id, "منبّه", 7 * 60)) }
            setEditing(id)
        }
    }
    for (al in a.alarms) {
        ListRow(onClick = { setEditing(if (editing == al.id) null else al.id) }) {
            Column(Modifier.weight(1f)) {
                Num(clock(al.minute), size = 32.sp, color = if (al.on) pal.ink else pal.grey)
                Txt("${al.label} · ${daysText(al.days)}", size = 14.sp, color = pal.grey, lineHeight = 1.5f)
            }
            InkSwitch(al.on, al.label) { on -> put { p -> p.copy(alarms = p.alarms.map { if (it.id == al.id) it.copy(on = on) else it }) } }
        }
        if (editing == al.id) AlarmEditor(al, onChange = { n -> put { p -> p.copy(alarms = p.alarms.map { if (it.id == al.id) n else it }) } }) {
            put { p -> p.copy(alarms = p.alarms.filter { it.id != al.id }) }
            setEditing(null)
        }
    }
    ListRow {
        Column(Modifier.weight(1f)) {
            Num(clock(day.plan.wake), size = 32.sp, color = if (settings.wakeAlarm) pal.ink else pal.grey)
            Txt("منبّه الصحيان ${if (day.plan.qiyam) "لصلاة الليل" else "قبل الفجر"} · يتحرك وياك كل ليلة", size = 14.sp, color = pal.grey, lineHeight = 1.5f)
        }
        InkSwitch(settings.wakeAlarm, "منبّه الصحيان") { save(settings.copy(wakeAlarm = it)) }
    }

    Heading("ملخص الصبح ومحاسبة الليل")
    ListRow {
        RowText("ملخص الصبح", "الساعة ${clock(day.times.sunrise)}، وية الشروق")
        InkSwitch(a.morningSummary, "ملخص الصبح") { v -> put { it.copy(morningSummary = v) } }
    }
    ListRow {
        RowText("محاسبة الليل", "الساعة ${clock(day.plan.bedtime - 30)}، قبل النوم بنص ساعة")
        InkSwitch(a.eveningReview, "محاسبة الليل") { v -> put { it.copy(eveningReview = v) } }
    }

    Heading("تنبيهات فقراتك")
    ListRow {
        RowText("تنبيه بداية كل فقرة", "لكل فقرة تكدر تغيّره من تفاصيلها")
        InkSwitch(settings.blockReminders, "تنبيه بداية كل فقرة") { save(settings.copy(blockReminders = it)) }
    }
    if (settings.blockReminders) {
        Segmented(ItemAlert.entries.map { it.label }, ItemAlert.entries.indexOf(a.itemDefault)) { i -> put { it.copy(itemDefault = ItemAlert.entries[i]) } }
    }
    ListRow {
        RowText("لما تخلص الفقرة", "«خلص التركيز، الجاي: الرسائل»")
        InkSwitch(a.endAlert, "لما تخلص الفقرة") { v -> put { it.copy(endAlert = v) } }
    }
    Txt("شلون ينبّهك", size = 15.sp, bold = true, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
    Segmented(AlertStyle.entries.map { it.label }, AlertStyle.entries.indexOf(a.style)) { i -> put { it.copy(style = AlertStyle.entries[i]) } }

    Heading("الصلاة")
    for (p in listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Tile(com.dcc.yawm.shared.screen.prayerIcon(p), true, TileSize.XS)
            Txt(prayerName(p), size = 16.sp, modifier = Modifier.weight(0.7f))
            Segmented(PrayerAlert.entries.map { it.label }, PrayerAlert.entries.indexOf(a.prayerAlert(p)), modifier = Modifier.weight(2f)) { i ->
                put { it.copy(prayer = it.prayer + (p to PrayerAlert.entries[i])) }
            }
        }
    }
    ListRow(onClick = device::openAdhanSound) {
        RowText("صوت الأذان", "اختاره من أصوات تلفونك")
        Txt("‹", bold = true)
    }
    Heading("فحص التنبيهات")
    AlarmChecks(status, device)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun AlarmEditor(al: UserAlarm, onChange: (UserAlarm) -> Unit, onDelete: () -> Unit) {
    var label by rememberSaveable(al.id) { mutableStateOf(al.label) }
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Field(label, "اسم المنبّه، مثلاً: الدواء") { label = it; onChange(al.copy(label = cleanTitle(it))) }
        ListRow {
            RowText("الساعة")
            Stepper(clock(al.minute), "أبكر 5 دقايق", "أتأخر 5 دقايق", onMinus = { onChange(al.copy(minute = (al.minute - 5).mod(1440))) }, onPlus = { onChange(al.copy(minute = (al.minute + 5).mod(1440))) })
        }
        ListRow {
            RowText("ساعة كاملة")
            Stepper(clock(al.minute).take(2), "أبكر ساعة", "أتأخر ساعة", onMinus = { onChange(al.copy(minute = (al.minute - 60).mod(1440))) }, onPlus = { onChange(al.copy(minute = (al.minute + 60).mod(1440))) })
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (d in SatFirst) {
                Chip(DayShort[d.ordinal], d in al.days, Modifier.weight(1f)) {
                    onChange(al.copy(days = if (d in al.days) al.days - d else al.days + d))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WideAction("hourglass", "احذفه", modifier = Modifier.weight(1f), onClick = onDelete)
        }
    }
}
