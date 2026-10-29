package com.dcc.yawm.shared.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.PlacedTask
import com.dcc.yawm.shared.day.TaskItem
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.durShort
import kotlinx.datetime.LocalDate

/** «المهام»: add in one line; today's drop into free time, then tomorrow's and «بالبال». */
@Composable
fun TasksScreen(
    settings: UserSettings,
    today: LocalDate,
    t: Int,
    placedToday: List<PlacedTask>,
    slotTitle: (String) -> String?,
    save: (UserSettings) -> Unit,
    toast: (String) -> Unit,
) {
    val pal = LocalPalette.current
    val all = settings.tasks
    val doneCount = all.count { it.done }
    InkWord("المهام", if (all.isEmpty()) 0f else doneCount.toFloat() / all.size, 112.dp, "خلصت $doneCount من ${all.size}", Modifier.padding(top = 18.dp))
    Txt(
        if (all.isEmpty()) "صفحتك فاضية. اكتب أول شغلة ببالك وهي تنحط بأول فراغ بيومك." else "خلصت $doneCount من ${all.size}",
        size = 18.sp, align = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
    Column(Modifier.padding(top = 12.dp)) {
        QuickAddForm(today, t) { task, msg -> save(settings.copy(tasks = settings.tasks + task)); toast(msg) }
    }
    fun toggle(x: TaskItem) = save(settings.copy(tasks = settings.tasks.map { if (it.id == x.id) it.copy(doneOn = if (it.done) null else today) else it }))
    fun move(x: TaskItem, day: LocalDate?) = save(settings.copy(tasks = settings.tasks.map { if (it.id == x.id) it.copy(day = day) else it }))
    fun delete(x: TaskItem) = save(settings.copy(tasks = settings.tasks.filter { it.id != x.id }))

    if (placedToday.isNotEmpty()) {
        Heading("اليوم")
        for (p in placedToday) {
            val sub = when {
                p.task.done -> "خلصت"
                p.noRoom -> "${p.task.minutes} د · ما بقى فراغ يكفيها اليوم"
                p.start != null -> "${clock(p.start)} · ${p.slotKey?.let(slotTitle)?.let { "ضمن «$it» · " }.orEmpty()}${p.task.minutes} د" + if (p.pinned) " · وقت ثابت" else ""
                else -> "${p.task.minutes} د"
            }
            TaskRow(p.task, sub, onToggle = { toggle(p.task) }) {
                if (p.noRoom && !p.task.done) PillButton("باچر") { move(p.task, today.plusDays(1)) }
                else PillButton("شيلها") { delete(p.task) }
            }
        }
    }
    val tomorrow = all.filter { it.day == today.plusDays(1) }
    if (tomorrow.isNotEmpty()) {
        Heading("باچر")
        for (x in tomorrow) TaskRow(x, "${x.minutes} د" + (x.pinAt?.let { " · ${clock(it)}" } ?: ""), onToggle = { toggle(x) }) { PillButton("اليوم") { move(x, today) } }
    }
    val later = all.filter { it.day == null }
    if (later.isNotEmpty()) {
        Heading("بالبال")
        Txt("اكتبها هسه وانساها، ورتّبها لما تفضى.", size = 14.5.sp, color = pal.grey)
        for (x in later) TaskRow(x, durShort(x.minutes), onToggle = { toggle(x) }) { PillButton("اليوم") { move(x, today) } }
    }
    val after = all.filter { it.day != null && it.day > today.plusDays(1) }
    if (after.isNotEmpty()) {
        Heading("بعدين")
        for (x in after) TaskRow(x, "${x.day} · ${x.minutes} د", onToggle = { toggle(x) }) { PillButton("اليوم") { move(x, today) } }
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun TaskRow(x: TaskItem, sub: String, onToggle: () -> Unit, trailing: @Composable () -> Unit) {
    val pal = LocalPalette.current
    val reduced = LocalReducedMotion.current
    val strike by animateFloatAsState(if (x.done) 1f else 0f, tween(if (reduced) 0 else 500), label = "strike")
    ListRow {
        Box(
            Modifier.size(48.dp).clickable(onClick = onToggle)
                .semantics { role = Role.Checkbox; contentDescription = x.title; stateDescription = if (x.done) "خلصت" else "باقية" },
            contentAlignment = Alignment.Center,
        ) { Check(x.done) }
        Column(Modifier.weight(1f).padding(vertical = 2.dp)) {
            Txt(
                x.title, size = 16.5.sp, bold = true, color = if (x.done) pal.grey else pal.ink, lineHeight = 1.5f,
                modifier = Modifier.drawWithContent {
                    drawContent()
                    if (strike > 0f) drawLine(pal.grey, Offset(size.width, size.height * 0.55f), Offset(size.width * (1f - strike), size.height * 0.55f), 1.5.dp.toPx())
                },
            )
            Txt(sub, size = 13.5.sp, color = pal.grey, lineHeight = 1.5f)
        }
        trailing()
    }
}
