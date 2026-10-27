package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.SimpleDate
import com.dcc.yawm.shared.alarm.DeviceControls
import com.dcc.yawm.shared.alarm.batteryAdvice
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.data.YawmData
import com.dcc.yawm.shared.day.ChecklistPoint
import com.dcc.yawm.shared.day.DayResolver
import com.dcc.yawm.shared.day.arabicDate
import com.dcc.yawm.shared.day.checklistScore
import com.dcc.yawm.shared.prayer.AsrMadhab
import com.dcc.yawm.shared.prayer.CalcMethod
import com.dcc.yawm.shared.prayer.DayPrayerTimes
import com.dcc.yawm.shared.prayer.Prayer
import com.dcc.yawm.shared.prayer.PrayerTimesEngine
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Test screen for steps 1 to 5 only. The Ink identity (docs/design/IDENTITY.md) arrives in step 7.
private val Paper = Color(0xFFFFFFFF)
private val Ink = Color(0xFF121212)
private val Grey = Color(0xFF77736C)

private val PrayerNames = listOf(
    Prayer.FAJR to "الفجر", Prayer.SUNRISE to "الشروق", Prayer.DHUHR to "الظهر",
    Prayer.ASR to "العصر", Prayer.MAGHRIB to "المغرب", Prayer.ISHA to "العشاء",
)

private fun hhmm(times: DayPrayerTimes, p: Prayer): String {
    val t = times[p].toLocalDateTime(times.zone)
    return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}

private fun clock(minutes: Int): String {
    val m = minutes.mod(1440)
    return "${(m / 60).toString().padStart(2, '0')}:${(m % 60).toString().padStart(2, '0')}"
}

@Composable
fun App(today: SimpleDate, data: YawmData, device: DeviceControls) {
    val date = LocalDate(today.year, today.month, today.day)
    val settings by remember { data.settings.settings }.collectAsState(UserSettings())
    val done by remember(date) { data.dayLog.doneOn(date) }.collectAsState(emptySet())
    val status by device.status.collectAsState()
    val scope = rememberCoroutineScope()
    val zone = runCatching { TimeZone.of(settings.zoneId) }.getOrElse { TimeZone.of(UserSettings().zoneId) }
    fun timesOn(d: LocalDate) = PrayerTimesEngine.compute(settings.place, d, zone, settings.prayer)
    val times = timesOn(date)
    val night = DayResolver.resolve(
        timesOn(date.minus(1, DateTimeUnit.DAY)),
        times,
        timesOn(date.plus(1, DateTimeUnit.DAY)),
        userBedtime = settings.bedtime,
        qiyamEnabled = settings.qiyamEnabled,
    ).plan
    val score = checklistScore(done, settings.roles)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText("يوم", style = TextStyle(color = Ink, fontSize = 56.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
            BasicText(
                arabicDate(today) + " · " + settings.cityName,
                style = TextStyle(color = Grey, fontSize = 18.sp, textAlign = TextAlign.Center),
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
            )
            for ((p, name) in PrayerNames) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    BasicText(name, style = TextStyle(color = Ink, fontSize = 20.sp))
                    BasicText(hhmm(times, p), style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                }
            }
            BasicText(
                "الليلة: تنام ${clock(night.bedtime)}، وتكعد ${clock(night.wake)}" + (if (night.qiyam) " للسَّحَر" else "") +
                    "\nنوم الليل ${night.nightSleep / 60}:${(night.nightSleep % 60).toString().padStart(2, '0')}، وقيلولة ${night.nap} دقيقة" +
                    (if (night.shortfall > 0) "\nينقصك ${night.shortfall} دقيقة عن 7 ساعات" else ""),
                style = TextStyle(color = Ink, fontSize = 17.sp, textAlign = TextAlign.Center),
                modifier = Modifier.padding(top = 20.dp),
            )
            if (settings.prayer.method == CalcMethod.EGYPTIAN && settings.prayer.madhab == AsrMadhab.SHAFI) {
                BasicText(
                    "حسب الهيئة المصرية العامة للمساحة، والعصر بالمذهب الشافعي",
                    style = TextStyle(color = Grey, fontSize = 14.sp, textAlign = TextAlign.Center),
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
            BasicText(
                "نقاط اليوم: ${score.done} من ${score.shown}",
                style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                modifier = Modifier.padding(top = 32.dp, bottom = 8.dp),
            )
            for (point in ChecklistPoint.visibleFor(settings.roles)) {
                val isDone = point in done
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable { scope.launch { data.dayLog.setDone(date, point, !isDone) } }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(point.label, style = TextStyle(color = if (isDone) Ink else Grey, fontSize = 17.sp), modifier = Modifier.weight(1f))
                    BasicText(if (isDone) "✓" else "○", style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold))
                }
            }
            AlarmSection(status, device, settings) { changed ->
                scope.launch {
                    data.settings.save(changed)
                    device.reschedule()
                }
            }
        }
    }
}

// Step 6 test section: permissions, the battery self-check and the alarm switches.
@Composable
private fun AlarmSection(
    status: com.dcc.yawm.shared.alarm.DeviceStatus,
    device: DeviceControls,
    settings: UserSettings,
    save: (UserSettings) -> Unit,
) {
    BasicText(
        "التنبيهات",
        style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        modifier = Modifier.padding(top = 32.dp, bottom = 4.dp),
    )
    BasicText(
        status.next?.let { "التنبيه الجاي: $it · ${status.scheduledCount} تنبيه لـ48 ساعة" } ?: "ما مضبوط أي تنبيه بعد",
        style = TextStyle(color = Grey, fontSize = 15.sp, textAlign = TextAlign.Center),
    )
    CheckRow("الإشعارات مسموحة", status.notificationsAllowed, "اسمح", device::requestNotifications)
    CheckRow("الأذان بالدقيقة (المنبهات والتذكيرات)", status.exactAlarmsAllowed, "افتح الإعداد", device::openExactAlarmSettings)
    if (!status.exactAlarmsAllowed) {
        Note("بدون هذا الإذن الأذان ممكن يتأخر دقائق. افتح الإعداد وفعّل «السماح بضبط المنبهات والتذكيرات» لـ«يوم».")
    }
    CheckRow("البطارية ما توقف التطبيق", status.batteryUnrestricted, "افتح الإعداد", device::openBatterySettings)
    if (!status.batteryUnrestricted) {
        Note("بالقائمة اختار «كل التطبيقات» ثم «يوم» ثم «لا تحسّن».")
    }
    for (step in batteryAdvice(status.manufacturer)) Note(step)
    ActionRow("صوت الأذان: اختاره من أصوات تلفونك", device::openAdhanSound)
    ToggleRow("تذكير ببداية كل فقرة", settings.blockReminders) { save(settings.copy(blockReminders = it)) }
    ToggleRow("منبّه الكعدة قبل الفجر", settings.wakeAlarm) { save(settings.copy(wakeAlarm = it)) }
}

@Composable
private fun CheckRow(label: String, ok: Boolean, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = !ok, onClick = onClick).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(label, style = TextStyle(color = Ink, fontSize = 17.sp), modifier = Modifier.weight(1f))
        BasicText(if (ok) "✓" else "✗ $action", style = TextStyle(color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onChange(!on) }.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(label, style = TextStyle(color = Ink, fontSize = 17.sp), modifier = Modifier.weight(1f))
        BasicText(if (on) "شغّال" else "مطفي", style = TextStyle(color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(label + " ←", style = TextStyle(color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun Note(text: String) {
    BasicText(text, style = TextStyle(color = Grey, fontSize = 14.sp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
}
