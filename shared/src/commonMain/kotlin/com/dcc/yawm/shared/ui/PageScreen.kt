package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.data.DayPage
import com.dcc.yawm.shared.day.ChecklistPoint
import com.dcc.yawm.shared.day.ChecklistScore
import com.dcc.yawm.shared.screen.EveningQuestions
import com.dcc.yawm.shared.screen.pointIcon
import kotlinx.coroutines.delay

@Composable
fun PageScreen(
    shown: List<ChecklistPoint>,
    done: Set<ChecklistPoint>,
    score: ChecklistScore,
    saved: DayPage?,
    onToggle: (ChecklistPoint) -> Unit,
    onSave: (DayPage) -> Unit,
) {
    val pal = LocalPalette.current
    InkWord("المحاسبة", if (score.shown == 0) 0f else score.done.toFloat() / score.shown, 92.dp, "${score.done} من ${score.shown}", Modifier.padding(top = 22.dp))
    When(listOf("${score.done}" to true, " من " to false, "${score.shown}" to true, " · الهدف 7 بالمعدل، والفوت مرة مقبول" to false))

    Heading("نقاط اليوم")
    if (shown.isEmpty()) {
        Txt("ما اخترت أي دور، فما في نقاط. تكدر تختار أدوارك من الإعدادات.", color = pal.grey)
    }
    shown.forEachIndexed { i, p ->
        ListRow(onClick = { onToggle(p) }, divider = i < shown.lastIndex) {
            Tile(pointIcon(p.number), false, TileSize.SM)
            RowText(p.label)
            Check(p in done)
        }
    }

    ReviewedQuote("h-qadamaa")
    Heading("ثلاث أسئلة قبل النوم")
    // Filled from what is saved once it loads; typing keeps its own state until «احفظ».
    var answers by remember(saved) { mutableStateOf(listOf(saved?.didToday.orEmpty(), saved?.lostTime.orEmpty(), saved?.tomorrowTopThree.orEmpty())) }
    EveningQuestions.forEachIndexed { i, q ->
        Txt(q, bold = true, modifier = Modifier.padding(top = 18.dp))
        BasicTextField(
            value = answers[i],
            onValueChange = { v -> answers = answers.toMutableList().also { it[i] = v } },
            textStyle = TextStyle(color = pal.ink, fontSize = 16.5.sp, fontFamily = LocalFonts.current.naskh, lineHeight = 28.sp),
            cursorBrush = SolidColor(pal.ink),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).heightIn(min = 76.dp)
                .border(1.5.dp, pal.line, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp)
                .semantics { contentDescription = q },
        )
    }
    var toast by remember { mutableStateOf(false) }
    InkButton("احفظ صحيفة اليوم", Modifier.padding(top = 18.dp, bottom = 12.dp)) {
        onSave(DayPage(answers[0], answers[1], answers[2]))
        toast = true
    }
    if (toast) {
        LaunchedEffect(Unit) { delay(2200); toast = false }
        Box(Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.background(pal.ink, CircleShape).padding(horizontal = 18.dp, vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
                Txt("انحفظت صحيفة اليوم على جهازك", size = 15.sp, color = pal.paper, align = TextAlign.Center)
            }
        }
    }
}
