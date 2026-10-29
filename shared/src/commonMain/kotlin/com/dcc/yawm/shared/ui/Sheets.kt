package com.dcc.yawm.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.alarm.ItemAlert
import com.dcc.yawm.shared.alarm.PrayerAlert
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.ParsedTask
import com.dcc.yawm.shared.day.TaskItem
import com.dcc.yawm.shared.day.TaskWhen
import com.dcc.yawm.shared.day.isFocusable
import com.dcc.yawm.shared.day.parseTask
import com.dcc.yawm.shared.day.whatIs
import com.dcc.yawm.shared.screen.Item
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.durText
import kotlinx.datetime.LocalDate

/** A bottom sheet over a dimmed screen; it rises with a small spring (none with reduced motion). */
@Composable
fun BoxScope.Sheet(label: String, close: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val pal = LocalPalette.current
    val reduced = LocalReducedMotion.current
    val shown = remember { MutableTransitionState(reduced) }
    shown.targetState = true
    Box(
        Modifier.fillMaxSize().background(Color(0x6B0F0F11))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close),
    )
    AnimatedVisibility(
        shown, Modifier.align(Alignment.BottomCenter),
        enter = if (reduced) fadeIn(tween(0)) else slideInVertically(spring(dampingRatio = 0.78f, stiffness = 420f)) { it } + fadeIn(tween(120)),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 760.dp)
                .background(pal.paper, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(44.dp, 5.dp).background(pal.faint, RoundedCornerShape(3.dp)))
            }
            content()
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 6.dp).clickable(onClick = close).semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) { Txt("سكّر", size = 15.sp, color = pal.grey, align = TextAlign.Center) }
        }
    }
}

/** An item's details: what it is, when, its reminder, and what you can do. */
@Composable
fun ColumnScope.ItemSheetBody(
    it: Item, t: Int, isToday: Boolean, isDone: Boolean, settings: UserSettings,
    onDone: () -> Unit, onFocus: () -> Unit, onEdit: (() -> Unit)?, save: (UserSettings) -> Unit,
) {
    val pal = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Tile(it.icon, it.isPrayer, TileSize.BIG)
        Column(Modifier.weight(1f)) {
            Txt(it.title, size = 21.sp, bold = true, lineHeight = 1.45f)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Num(clock(it.start), size = 18.sp, color = pal.ink)
                Txt("←", size = 14.sp, color = pal.grey)
                Num(clock(it.end), size = 18.sp, color = pal.ink)
                Txt("· ${durText(it.end - it.start)}", size = 15.sp, color = pal.grey)
            }
        }
    }
    whatIs(it.key)?.let { what ->
        Column(Modifier.fillMaxWidth().padding(top = 14.dp).background(lerp(pal.paper, pal.ink, 0.05f), RoundedCornerShape(14.dp)).padding(12.dp)) {
            Txt("شنو هاي؟", size = 14.sp, bold = true, color = pal.grey, lineHeight = 1.4f)
            Txt(what, size = 15.5.sp, lineHeight = 1.75f)
        }
    }
    if (it.shrunkFrom != null) {
        Txt("اليوم صارت ${durText(it.end - it.start)} بدل ${durText(it.shrunkFrom)}، لأن الوقت بين الصلاتين أقصر.", size = 15.sp, color = pal.soft, modifier = Modifier.padding(top = 10.dp))
    }
    if (isToday && it.start <= t && t < it.end) {
        val f = ((t - it.start).toFloat() / (it.end - it.start)).coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().padding(top = 14.dp).heightIn(min = 7.dp).background(pal.faint, RoundedCornerShape(4.dp))) {
            Box(Modifier.fillMaxWidth(f).heightIn(min = 7.dp).background(pal.ink, RoundedCornerShape(4.dp)))
        }
        Txt("باقي ${durText(it.end - t)}", size = 14.sp, color = pal.grey, modifier = Modifier.padding(top = 4.dp))
    }

    val prayer = prayerOf(it.key)
    Row(Modifier.padding(top = 18.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        YIcon("hourglass", 18.dp, pal.ink, pal.ink)
        Txt(if (prayer != null) "تنبيه الصلاة" else "نبّهني", size = 16.sp, bold = true)
    }
    if (prayer != null) {
        val opts = PrayerAlert.entries
        Segmented(opts.map { o -> o.label }, opts.indexOf(settings.alerts.prayerAlert(prayer))) { i ->
            save(settings.copy(alerts = settings.alerts.copy(prayer = settings.alerts.prayer + (prayer to opts[i]))))
        }
    } else {
        val opts = ItemAlert.entries
        Segmented(opts.map { o -> o.label }, opts.indexOf(settings.alerts.itemAlert(it.key))) { i ->
            save(settings.copy(alerts = settings.alerts.copy(itemOverrides = settings.alerts.itemOverrides + (it.key to opts[i]))))
        }
    }

    Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val doneLabel = when { isDone -> "تم، ألغيه"; it.isPrayer -> "صلّيت"; it.isSleep -> "صحيت"; else -> "خلّصت" }
        WideAction("seal", doneLabel, primary = true, modifier = Modifier.weight(1f), onClick = onDone)
        if (isToday && isFocusable(it.key, it.icon)) WideAction("nib", "ركّز", modifier = Modifier.weight(1f), onClick = onFocus)
        else if (onEdit != null && !it.isPrayer) WideAction("week", "عدّلها", modifier = Modifier.weight(1f), onClick = onEdit)
    }
}

@Composable
fun WideAction(icon: String, label: String, primary: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val pal = LocalPalette.current
    val fg = if (primary) pal.paper else pal.ink
    Row(
        modifier.heightIn(min = 56.dp)
            .background(if (primary) pal.ink else pal.paper, RoundedCornerShape(999.dp))
            .border(1.5.dp, if (primary) pal.ink else pal.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).semantics { role = Role.Button }
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        YIcon(icon, 22.dp, fg, fg)
        Txt(label, size = 16.sp, bold = true, color = fg)
    }
}

/** The quick add form, used in the sheet from + and at the top of «المهام». */
@Composable
fun QuickAddForm(today: LocalDate, t: Int, onAdd: (TaskItem, String) -> Unit) {
    val pal = LocalPalette.current
    var text by remember { mutableStateOf("") }
    var whenTap by remember { mutableStateOf<TaskWhen?>(null) }
    var minutesTap by remember { mutableStateOf<Int?>(null) }
    var pin by remember { mutableStateOf<Int?>(null) }
    val parsed: ParsedTask = parseTask(text)
    val whenDay = whenTap ?: parsed.whenDay ?: TaskWhen.TODAY
    val minutes = minutesTap ?: parsed.minutes ?: 30
    Field(text, "شنو عندك تسوي؟") { text = it }
    Txt(
        if (parsed.title.isBlank()) "تكدر تكتب: اتصل بالمحاسب باچر نص ساعة"
        else "«${parsed.title}» · ${whenName(whenDay)} · $minutes د" + if ((parsed.whenDay != null && whenTap == null) || (parsed.minutes != null && minutesTap == null)) " · فهمتها من كلامك" else "",
        size = 13.5.sp, color = pal.grey, lineHeight = 1.6f,
    )
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (w in TaskWhen.entries) Chip(whenName(w), w == whenDay, Modifier.weight(1f)) { whenTap = w }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (m in listOf(15, 30, 45, 60, 90)) Chip("$m د", m == minutes, Modifier.weight(1f)) { minutesTap = m }
    }
    if (whenDay != TaskWhen.LATER) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("وحدها بالفراغ", pin == null, Modifier.weight(1f)) { pin = null }
            Chip("أختار وقتها", pin != null, Modifier.weight(1f)) { pin = pin ?: ((t.mod(1440) + 30 + 14) / 15 * 15).mod(1440) }
        }
        pin?.let { p ->
            ListRow {
                RowText("الساعة")
                Stepper(clock(p), "أبكر ربع ساعة", "أتأخر ربع ساعة", onMinus = { pin = (p - 15).mod(1440) }, onPlus = { pin = (p + 15).mod(1440) })
            }
        }
    }
    InkButton("حطّها", Modifier.padding(top = 12.dp)) {
        val title = parsed.title.trim()
        if (title.isEmpty()) return@InkButton
        val day = when (whenDay) { TaskWhen.TODAY -> today; TaskWhen.TOMORROW -> today.plusDays(1); TaskWhen.LATER -> null }
        val id = "t${kotlin.random.Random.nextInt(1, Int.MAX_VALUE)}"
        val p = if (whenDay == TaskWhen.LATER) null else pin
        onAdd(
            TaskItem(id, title, minutes, day, p),
            when {
                p != null -> "انحطّت الساعة ${clock(p)}"
                whenDay == TaskWhen.TODAY -> "انحطّت بأول فراغ اليوم"
                whenDay == TaskWhen.TOMORROW -> "انحطّت لباچر"
                else -> "انحطّت بالبال"
            },
        )
        text = ""; whenTap = null; minutesTap = null; pin = null
    }
}

fun whenName(w: TaskWhen) = when (w) { TaskWhen.TODAY -> "اليوم"; TaskWhen.TOMORROW -> "باچر"; TaskWhen.LATER -> "بالبال" }

/** The + menu: a task, an alarm, or an item in your day. */
@Composable
fun ColumnScope.AddMenuBody(onTask: () -> Unit, onAlarm: () -> Unit, onItem: () -> Unit) {
    Txt("شتريد تضيف؟", size = 21.sp, bold = true, modifier = Modifier.padding(bottom = 6.dp))
    ListRow(onClick = onTask) { Tile("seal", false, TileSize.SM); RowText("مهمة", "تنحط بأول فراغ بيومك، أو بالساعة اللي تختارها"); Txt("‹", bold = true) }
    ListRow(onClick = onAlarm) { Tile("hourglass", false, TileSize.SM); RowText("منبّه", "بساعة تختارها، ويتكرر بالأيام"); Txt("‹", bold = true) }
    ListRow(onClick = onItem, divider = false) { Tile("week", false, TileSize.SM); RowText("فقرة بيومك", "مثل «دراسة» كل يوم بعد العصر"); Txt("‹", bold = true) }
}
