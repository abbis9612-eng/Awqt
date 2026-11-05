package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.alarm.DeviceStatus
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.Remind
import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.screen.Field
import com.dcc.yawm.shared.screen.clock

/*
 * First run of v11: three steps (city; bedtime and fixed work hours; what you want every day)
 * with the notification check on the last one. Everything here can be changed later.
 */

private val Suggestions = listOf(
    "قرآن" to DayPart.FAJR, "مشي" to DayPart.MORNING, "قيلولة قصيرة" to DayPart.DHUHR,
    "رياضة" to DayPart.ASR, "وقت ويّا الأهل" to DayPart.MAGHRIB, "قراءة" to DayPart.ISHA,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirstRun(
    settings: UserSettings,
    status: DeviceStatus,
    device: com.dcc.yawm.shared.alarm.DeviceControls,
    platform: Platform,
    save: (UserSettings) -> Unit,
    onField: (Field) -> Unit,
) {
    var step by remember { mutableIntStateOf(1) }
    // «الألوان»: the three steps walk with the day, from Fajr to the morning to Dhuhr.
    val field = when (step) { 1 -> Field.FAJR; 2 -> Field.MORNING; else -> Field.DHUHR }
    SideEffect { onField(field) }
    Fielded(field) { FirstRunSteps(settings, status, device, platform, save, step) { step = it } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FirstRunSteps(
    settings: UserSettings,
    status: DeviceStatus,
    device: com.dcc.yawm.shared.alarm.DeviceControls,
    platform: Platform,
    save: (UserSettings) -> Unit,
    step: Int,
    setStep: (Int) -> Unit,
) {
    val pal = LocalPalette.current
    var work by remember { mutableStateOf(true) }
    var workStart by remember { mutableIntStateOf(8 * 60) }
    var workEnd by remember { mutableIntStateOf(14 * 60) }
    var picked by remember { mutableStateOf(setOf("رياضة", "وقت ويّا الأهل")) }

    fun finish(withChoices: Boolean) {
        var things = settings.things
        fun add(t: Thing) { things = things + t.copy(id = "h${(things.mapNotNull { it.id.removePrefix("h").toIntOrNull() }.maxOrNull() ?: 0) + 1}") }
        if (withChoices) {
            if (work) add(Thing("", "الدوام", at = workStart, end = workEnd, repeat = Repeat.WORKDAYS, remind = Remind.NONE))
            for ((title, part) in Suggestions) if (title in picked) add(Thing("", title, part, repeat = Repeat.DAILY))
        }
        save(settings.copy(setupDone = true, things = things))
    }

    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..3) Box(Modifier.weight(1f).height(8.dp).background(if (i <= step) pal.ink else pal.line, RoundedCornerShape(4.dp)))
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        if (step > 1) PillButton("رجوع") { setStep(step - 1) } else Spacer(Modifier.width(1.dp))
        PillButton("تخطّى") { finish(false) }
    }
    when (step) {
        1 -> {
            Display("هلا بيك بـ«يوم»", 30.sp)
            Txt("يرتّب يومك بين الصلوات، ويذكّرك بوكت كل شي. كلشي يبقى بتلفونك، بلا حساب وبلا إعلانات.", size = 16.sp, color = pal.soft, lineHeight = 1.8f)
            Heading("مدينتك")
            CardGroup { CityChooser(settings, platform, save) }
            InkButton("التالي", Modifier.padding(vertical = 22.dp)) { setStep(2) }
        }
        2 -> {
            Display("شوكت تنام؟", 30.sp)
            Txt("حتى نحسب نومك وصلاة الليل.", size = 16.sp, color = pal.soft)
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
                Stepper(
                    clock(settings.bedtime), "أبكر ربع ساعة", "أتأخر ربع ساعة",
                    onMinus = { save(settings.copy(bedtime = (settings.bedtime - 15).coerceAtLeast(19 * 60 + 30))) },
                    onPlus = { save(settings.copy(bedtime = (settings.bedtime + 15).coerceAtMost(23 * 60 + 30))) },
                )
            }
            Txt("عندك دوام أو دراسة بساعة ثابتة؟", size = 17.sp, bold = true, modifier = Modifier.padding(top = 10.dp))
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("إي", work, Modifier.weight(1f)) { work = true }
                Chip("لا", !work, Modifier.weight(1f)) { work = false }
            }
            if (work) {
                ListRow {
                    RowText("يبدي")
                    Stepper(clock(workStart), "أبكر نص ساعة", "أتأخر نص ساعة",
                        onMinus = { workStart = (workStart - 30).coerceAtLeast(4 * 60) },
                        onPlus = { workStart = (workStart + 30).coerceAtMost(workEnd - 30) })
                }
                ListRow {
                    RowText("يخلص")
                    Stepper(clock(workEnd), "أبكر نص ساعة", "أتأخر نص ساعة",
                        onMinus = { workEnd = (workEnd - 30).coerceAtLeast(workStart + 30) },
                        onPlus = { workEnd = (workEnd + 30).coerceAtMost(23 * 60) })
                }
                Txt("أيام الدوام", size = 15.sp, bold = true, color = pal.grey, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (d in SatFirst) Chip(DayShort[d.ordinal], d in settings.workDays, Modifier.weight(1f)) {
                        save(settings.copy(workDays = if (d in settings.workDays) settings.workDays - d else settings.workDays + d))
                    }
                }
            }
            InkButton("التالي", Modifier.padding(vertical = 22.dp)) { setStep(3) }
        }
        else -> {
            Display("تريد شي كل يوم؟", 30.sp)
            Txt("اضغط اللي يعجبك. تكدر تضيف وتغيّر بعدين من «+ أضف».", size = 16.sp, color = pal.soft, lineHeight = 1.8f)
            FlowRow(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((title, part) in Suggestions) Chip("$title · ${part.label}", title in picked) {
                    picked = if (title in picked) picked - title else picked + title
                }
            }
            Heading("التنبيهات")
            Txt("حتى ينبّهك بوكت الصلاة والمنبّه، لازم تسمحله:", size = 15.sp, color = pal.soft)
            AlarmChecks(status, device)
            InkButton("وريني يومي", Modifier.padding(vertical = 22.dp)) { finish(true) }
        }
    }
}
