package com.dcc.yawm.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.screen.DayView
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.durText
import com.dcc.yawm.shared.screen.hm

/** Earliest and latest bedtime the stepper allows (prototype: 19:30 to 23:30). */
private const val BED_MIN = 19 * 60 + 30
private const val BED_MAX = 23 * 60 + 30

@Composable
fun NightScreen(day: DayView, t: Int, settings: UserSettings, save: (UserSettings) -> Unit) {
    val pal = LocalPalette.current
    val p = day.plan
    val asleep = t >= p.bedtime
    val ink = day.nightInk(t)
    val label = when {
        asleep -> "${ink.word} بعد ${durText(ink.target - t)}"
        t < day.times.isha -> "تنام الساعة ${clock(p.bedtime)}"
        else -> "تنام بعد ${durText(p.bedtime - t)}"
    }
    InkWord(ink.word, ink.progress, wordSize(ink.word), label, Modifier.padding(top = 22.dp))
    if (asleep) {
        When(listOf("بعد ${durText(ink.target - t)}، الساعة " to false, clock(ink.target) to true))
    } else {
        When(listOf("تنام " to false, clock(p.bedtime) to true, "، وتكعد " to false, clock(p.wake) to true, (if (p.qiyam) " للسَّحَر" else "") to false))
    }

    NightLine(day, t)

    val ok = p.shortfall == 0
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Tile(if (ok) "seal" else "hourglass", solid = false)
        Column(Modifier.weight(1f)) {
            Txt(if (ok) "نومك يكفي" else "ينقصك ${durText(p.shortfall)}", size = 18.sp, bold = true, lineHeight = 1.5f)
            Txt("ليل ${hm(p.nightSleep)} وقيلولة ${p.nap} د، المجموع ${hm(p.total)} من 7 ساعات", size = 14.5.sp, color = pal.grey, lineHeight = 1.5f)
        }
    }

    Heading("ليلتك خطوة بخطوة")
    val steps = day.nightSteps()
    val current = steps.indexOfLast { t >= it.start }
    steps.forEachIndexed { i, s ->
        ListRow {
            Tile(s.icon, solid = s.isPrayer, size = TileSize.SM, done = i < current)
            Num(clock(s.start), modifier = Modifier.width(48.dp))
            RowText(s.title, bold = i == current, color = if (i < current) pal.grey else pal.ink)
        }
    }

    Heading("عدّل ليلتك")
    ListRow(minHeight = 60.dp) {
        RowText("ساعة نومك", "ما تنام قبل العشاء بنص ساعة")
        Stepper(
            clock(settings.bedtime), "أبكر ربع ساعة", "أتأخر ربع ساعة",
            onMinus = { save(settings.copy(bedtime = (settings.bedtime - 15).coerceAtLeast(BED_MIN))) },
            onPlus = { save(settings.copy(bedtime = (settings.bedtime + 15).coerceAtMost(BED_MAX))) },
        )
    }
    ListRow(minHeight = 60.dp) {
        RowText("السَّحَر قبل الفجر", "إذا بقى للنوم 6 ساعات أو أكثر")
        InkSwitch(settings.qiyamEnabled, "السحر قبل الفجر") { save(settings.copy(qiyamEnabled = it)) }
    }
    ListRow(minHeight = 60.dp, divider = false) {
        RowText("منبّه الكعدة ${clock(p.wake)}", "يتحرك وياك كل ليلة")
        InkSwitch(settings.wakeAlarm, "منبه الكعدة") { save(settings.copy(wakeAlarm = it)) }
    }
    Spacer(Modifier.height(18.dp))
}

/** The night as one line: sleep in ink, then suhur in the lantern's colour, up to Fajr. */
@Composable
private fun NightLine(day: DayView, t: Int) {
    val pal = LocalPalette.current
    val p = day.plan
    val a = p.bedtime
    val z = day.nextFajr
    Canvas(
        Modifier.padding(top = 20.dp).fillMaxWidth().height(20.dp)
            .clearAndSetSemantics { contentDescription = "من ${clock(a)} لحد الفجر ${clock(z)}" },
    ) {
        val w = size.width
        val y = size.height / 2
        val sw = 10.dp.toPx()
        // Right to left: bedtime on the right, Fajr on the left.
        fun x(m: Int) = (w - sw / 2) - (m - a).toFloat() / (z - a) * (w - sw)
        drawLine(pal.faint, Offset(sw / 2, y), Offset(w - sw / 2, y), sw, StrokeCap.Round)
        drawLine(pal.ink, Offset(x(a), y), Offset(x(p.wake), y), sw, StrokeCap.Round)
        if (p.qiyam) drawLine(SuhurColor, Offset(x(p.wake) - 7.dp.toPx(), y), Offset(x(z), y), sw, StrokeCap.Round)
        if (t in a until z) {
            drawCircle(pal.paper, 8.dp.toPx(), Offset(x(t), y))
            drawCircle(pal.ink, 8.dp.toPx(), Offset(x(t), y), style = Stroke(3.dp.toPx()))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Num(clock(a), size = 17.sp, color = pal.ink)
            Txt("تنام", size = 12.5.sp, color = pal.grey, lineHeight = 1.3f)
        }
        Column(horizontalAlignment = Alignment.End) {
            Num(clock(p.wake), size = 17.sp, color = pal.ink)
            Txt((if (p.qiyam) "السَّحَر" else "تكعد") + "، والفجر ${clock(z)}", size = 12.5.sp, color = pal.grey, lineHeight = 1.3f)
        }
    }
}
