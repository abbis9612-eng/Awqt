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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.screen.DayView
import com.dcc.yawm.shared.screen.Item
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.durText

/** Prototype doneLabel: what the stamp says for the current block. */
private fun stampLabel(b: Item): String = when {
    b.isPrayer -> "صلّيت"
    b.isSleep -> "صحيت"
    else -> "خلّصت"
}

@Composable
fun TodayScreen(day: DayView, t: Int, done: Set<String>, onToggle: (String) -> Unit) {
    val pal = LocalPalette.current
    val ink = day.ink(t)
    val left = ink.target - t
    InkWord(ink.word, ink.progress, wordSize(ink.word), "${ink.word} بعد ${durText(left)}", Modifier.padding(top = 22.dp))
    When(listOf("بعد ${durText(left)}، الساعة " to false, clock(ink.target) to true), Modifier.padding(top = 2.dp))

    val cur = day.current(t)
    if (cur != null) {
        Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Tile(cur.icon, solid = cur.isPrayer, size = TileSize.BIG)
            Column(Modifier.weight(1f)) {
                Txt("هسه", size = 15.sp, color = pal.grey, lineHeight = 1.4f)
                Txt(cur.title, size = 23.sp, bold = true, lineHeight = 1.5f)
                Txt("لحد ${clock(cur.end)}", size = 15.sp, color = pal.grey, lineHeight = 1.4f)
            }
            Stamp(stampLabel(cur), cur.key in done) { onToggle(cur.key) }
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        for (b in day.upcoming(t, 2)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Tile(b.icon, solid = b.isPrayer, size = TileSize.SM)
                Num(clock(b.start), modifier = Modifier.width(48.dp))
                Txt(b.title, color = pal.soft, modifier = Modifier.weight(1f), lineHeight = 1.5f)
            }
        }
    }

    var showAll by remember { mutableStateOf(false) }
    ListRow(onClick = { showAll = !showAll }, minHeight = 48.dp) {
        RowText(if (showAll) "خفي اليوم كامل" else "شوف اليوم كامل", bold = true)
    }
    if (showAll) DayList(day, t, done)
    if (day.changes.isNotEmpty()) {
        Heading("شنو تغيّر اليوم")
        for (c in day.changes) Txt(c, size = 15.5.sp, color = pal.soft, modifier = Modifier.padding(vertical = 4.dp))
    }

    Spacer(Modifier.height(18.dp))
    FivePrayers(day, t)
}

/** The whole day, past rows grey, the current one bold (prototype `.row`). */
@Composable
fun DayList(day: DayView, t: Int, done: Set<String>) {
    val pal = LocalPalette.current
    for (b in day.items) {
        val past = b.end <= t
        val now = b.start <= t && t < b.end
        ListRow {
            Tile(b.icon, solid = b.isPrayer, size = TileSize.SM, done = past)
            Num(clock(b.start), modifier = Modifier.width(48.dp))
            RowText(
                b.title + if (b.key in done) " ✓" else "",
                sub = b.shrunkFrom?.let { "صارت ${durText(b.end - b.start)} بدل ${durText(it)}" },
                bold = now,
                color = if (past) pal.grey else pal.ink,
            )
        }
    }
}

/** The five prayers along the bottom; the names fill with ink like the big word. */
@Composable
fun FivePrayers(day: DayView, t: Int) {
    val pal = LocalPalette.current
    val fonts = LocalFonts.current
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        for (c in day.prayers(t)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                YIcon(c.icon, if (c.isNext) 30.dp else 26.dp, pal.ink, accentOf(c.icon, pal.isDark))
                val a = (1f - c.fill).coerceIn(0f, 1f)
                BasicText(
                    c.name,
                    style = TextStyle(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(0f to pal.faint, a to pal.faint, a to pal.ink, 1f to pal.ink),
                        fontFamily = fonts.ruqaa, fontWeight = FontWeight.Bold, fontSize = 25.sp, textAlign = TextAlign.Center,
                    ),
                )
                Num(clock(c.at), size = 17.sp)
            }
        }
    }
}

/** The round stamp («خلّصت»): presses a little and gives a light buzz. */
@Composable
fun Stamp(label: String, isDone: Boolean, onClick: () -> Unit) {
    val pal = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier.size(80.dp)
            .background(if (isDone) pal.paper else pal.ink, CircleShape)
            .border(if (isDone) 2.dp else 1.5.dp, pal.ink, CircleShape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics { role = Role.Button; contentDescription = if (isDone) "تم، اضغط حتى تلغي" else label },
        contentAlignment = Alignment.Center,
    ) {
        if (!isDone) Box(Modifier.size(73.dp).border(1.5.dp, pal.paper, CircleShape))
        Txt(if (isDone) "✓ تم" else label, size = 18.sp, bold = true, color = if (isDone) pal.ink else pal.paper, align = TextAlign.Center, lineHeight = 1.3f)
    }
}
