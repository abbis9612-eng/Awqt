package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.dcc.yawm.shared.day.arabicDate
import com.dcc.yawm.shared.prayer.AsrMadhab
import com.dcc.yawm.shared.prayer.CalcMethod
import com.dcc.yawm.shared.prayer.DayPrayerTimes
import com.dcc.yawm.shared.prayer.GeoPoint
import com.dcc.yawm.shared.prayer.Prayer
import com.dcc.yawm.shared.prayer.PrayerSettings
import com.dcc.yawm.shared.prayer.PrayerTimesEngine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Test screen for steps 1 and 2 only. The Ink identity (docs/design/IDENTITY.md) arrives in step 7.
private val Paper = Color(0xFFFFFFFF)
private val Ink = Color(0xFF121212)
private val Grey = Color(0xFF77736C)

/** The owner's city until first-run settings exist (step 7): Sulaymaniyah, Egyptian method, Shafi Asr. */
private val TestPlace = GeoPoint(35.5613, 45.4375)
private val TestZone = TimeZone.of("Asia/Baghdad")
private val TestSettings = PrayerSettings(CalcMethod.EGYPTIAN, AsrMadhab.SHAFI)

private val PrayerNames = listOf(
    Prayer.FAJR to "الفجر", Prayer.SUNRISE to "الشروق", Prayer.DHUHR to "الظهر",
    Prayer.ASR to "العصر", Prayer.MAGHRIB to "المغرب", Prayer.ISHA to "العشاء",
)

private fun hhmm(times: DayPrayerTimes, p: Prayer): String {
    val t = times[p].toLocalDateTime(times.zone)
    return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}

@Composable
fun App(today: SimpleDate) {
    val times = PrayerTimesEngine.compute(TestPlace, LocalDate(today.year, today.month, today.day), TestZone, TestSettings)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxSize().background(Paper).padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText("يوم", style = TextStyle(color = Ink, fontSize = 56.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
            BasicText(
                arabicDate(today) + " · السليمانية",
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
                "حسب الهيئة المصرية العامة للمساحة، والعصر بالمذهب الشافعي",
                style = TextStyle(color = Grey, fontSize = 14.sp, textAlign = TextAlign.Center),
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}
