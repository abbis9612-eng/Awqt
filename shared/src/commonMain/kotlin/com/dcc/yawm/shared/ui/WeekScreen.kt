package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.screen.DayNames
import com.dcc.yawm.shared.screen.MonthNames
import com.dcc.yawm.shared.screen.ReviewSteps
import com.dcc.yawm.shared.screen.WeekPlan
import com.dcc.yawm.shared.screen.WeekView
import com.dcc.yawm.shared.screen.durShort
import com.dcc.yawm.shared.screen.hm
import com.dcc.yawm.shared.screen.nextReview
import kotlinx.datetime.LocalDate

@Composable
fun WeekScreen(week: WeekView, today: LocalDate, t: Int, openDay: (LocalDate) -> Unit, openReview: () -> Unit) {
    val pal = LocalPalette.current
    val ix = week.days.indexOfFirst { it.date == today }
    InkWord("الأسبوع", week.progress(today, t), 112.dp, "مشى ${ix + 1} من 7 أيام", Modifier.padding(top = 22.dp))
    val f = week.friday
    When(listOf("من السبت " to false, "${week.saturday.day}" to true, " إلى الجمعة " to false, "${f.day}" to true, " ${MonthNames[f.month.ordinal]}" to false))

    Column(Modifier.fillMaxWidth().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Total("nib", durShort(week.deepWork), "تركيز، الهدف 10 إلى 15 س", Modifier.weight(1f))
            Total("house", durShort(week.family), "ويّا الأهل", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Total("steps", durShort(week.movement), "حركة، الهدف 150 د", Modifier.weight(1f))
            Total("bed", hm(week.averageSleep), "نوم بالمعدل", Modifier.weight(1f))
        }
    }

    Heading("خطة الأسبوع")
    week.days.forEachIndexed { i, d ->
        val plan = WeekPlan[i]
        val isToday = d.date == today
        val past = d.date < today
        Row(
            Modifier.fillMaxWidth().clickable { openDay(d.date) }.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(4.dp).height(36.dp).background(if (isToday) pal.ink else pal.paper, RoundedCornerShape(2.dp)))
            Column(Modifier.width(62.dp).alpha(if (past) 0.55f else 1f)) {
                Ruqaa(DayNames[d.date.dayOfWeek.ordinal], 22.sp)
                Num("${d.date.day}", size = 16.sp)
            }
            Column(Modifier.weight(1f).alpha(if (past) 0.55f else 1f)) {
                Txt(plan.focus, size = 16.5.sp, lineHeight = 1.45f)
                Txt("${plan.morning}، والعصر: ${plan.afterAsr}", size = 14.sp, color = pal.grey, lineHeight = 1.5f)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { plan.icons.forEach { Tile(it, false, TileSize.XS) } }
        }
    }

    val sat = nextReview(today)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = openReview).padding(top = 18.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Tile("week", false)
        Column(Modifier.weight(1f)) {
            Txt(
                if (sat == today) "اليوم مراجعة الأسبوع، 45 دقيقة" else "مراجعة الأسبوع: السبت ${sat.day} ${MonthNames[sat.month.ordinal]}",
                size = 16.5.sp, bold = true,
            )
            Txt("خمس خطوات، وتغيّر شي واحد بس على نظامك.", size = 15.5.sp, lineHeight = 1.7f)
        }
    }
}

@Composable
private fun Total(icon: String, value: String, label: String, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Tile(icon, false)
        Column {
            Num(value, size = 22.sp, color = LocalPalette.current.ink, bold = true)
            Txt(label, size = 13.5.sp, color = LocalPalette.current.grey, lineHeight = 1.35f)
        }
    }
}

@Composable
fun Ruqaa(text: String, size: androidx.compose.ui.unit.TextUnit, color: androidx.compose.ui.graphics.Color = LocalPalette.current.ink) {
    androidx.compose.foundation.text.BasicText(
        text,
        style = androidx.compose.ui.text.TextStyle(
            color = color, fontFamily = LocalFonts.current.ruqaa, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = size, lineHeight = size * 1.3f,
        ),
    )
}

/** The Saturday review (docs/METHOD.md section 8): the week's points, then the five steps. */
@Composable
fun ReviewScreen(week: WeekView, pointsDone: Int, pointsPossible: Int, close: () -> Unit) {
    val pal = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) { PillButton("رجوع", close) }
    InkWord("المراجعة", if (pointsPossible == 0) 0f else pointsDone.toFloat() / pointsPossible, 104.dp, "نقاط الأسبوع $pointsDone من $pointsPossible")
    When(listOf("نقاط الأسبوع " to false, "$pointsDone" to true, " من " to false, "$pointsPossible" to true))
    ReviewedQuote("h-ghabn")
    Heading("خمس خطوات، 45 دقيقة")
    ReviewSteps.forEachIndexed { i, s ->
        ListRow(divider = i < ReviewSteps.lastIndex) {
            Num("${i + 1}", size = 20.sp, color = pal.ink, modifier = Modifier.width(28.dp))
            RowText(s)
        }
    }
    Txt(
        "من السبت ${week.saturday.day} إلى الجمعة ${week.friday.day}: تركيز ${durShort(week.deepWork)}، وويّا الأهل ${durShort(week.family)}، ونوم ${hm(week.averageSleep)} بالمعدل.",
        size = 15.sp, color = pal.grey, modifier = Modifier.padding(vertical = 18.dp),
    )
}
