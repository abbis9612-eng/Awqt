package com.dcc.yawm.shared.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.alarm.AlertPrefs
import com.dcc.yawm.shared.alarm.ItemAlert
import com.dcc.yawm.shared.alarm.PrayerAlert
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.MyDayCatalog
import com.dcc.yawm.shared.day.PlacedTask
import com.dcc.yawm.shared.day.Slot
import com.dcc.yawm.shared.day.TaskItem
import com.dcc.yawm.shared.day.isFocusable
import com.dcc.yawm.shared.day.isFreeKey
import com.dcc.yawm.shared.day.placeTasks
import com.dcc.yawm.shared.day.shortWord
import com.dcc.yawm.shared.day.tasksOn
import com.dcc.yawm.shared.prayer.Prayer
import com.dcc.yawm.shared.screen.DayView
import com.dcc.yawm.shared.screen.Item
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.durShort
import com.dcc.yawm.shared.screen.durText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** The ink word of an item: the catalog's short word for the user's own items. */
fun wordOf(it: Item, settings: UserSettings): String {
    val id = it.key.removePrefix("fixed:").removePrefix("my:")
    val mine = settings.myDay?.items?.firstOrNull { m -> m.id == id }
    return shortWord(it.key, it.title, mine?.let { m -> MyDayCatalog.entry(m.kind)?.word ?: m.title.split(' ').first() })
}

/** The prayer behind a prayer block's key. */
fun prayerOf(key: String): Prayer? = when (key) {
    "fajr" -> Prayer.FAJR; "dhuhr", "jumua" -> Prayer.DHUHR; "asr" -> Prayer.ASR
    "maghrib" -> Prayer.MAGHRIB; "isha" -> Prayer.ISHA; else -> null
}

/** What the item's reminder button says. */
fun alertLabel(it: Item, a: AlertPrefs): String =
    prayerOf(it.key)?.let { p -> a.prayerAlert(p).label } ?: a.itemAlert(it.key).label

/** Tasks of [date] placed into the day's free time (from now on, for today). */
fun placedTasksOf(day: DayView, settings: UserSettings, date: LocalDate, today: LocalDate, t: Int): List<PlacedTask> {
    val mine = tasksOn(date, today, settings.tasks)
    if (mine.isEmpty()) return emptyList()
    val all = day.items.filter { !it.isSleep }.map { Slot(it.key, it.start, it.end) }
    return placeTasks(mine, all.filter { isFreeKey(it.key) }, if (date == today) t else 0, all)
}

@Composable
fun TodayScreen(
    day: DayView,
    t: Int,
    isToday: Boolean,
    done: Set<String>,
    settings: UserSettings,
    placed: List<PlacedTask>,
    onToggle: (String) -> Unit,
    openSheet: (Item) -> Unit,
    startFocus: (Item) -> Unit,
    toggleTask: (TaskItem) -> Unit,
    openNight: () -> Unit,
    openPage: () -> Unit,
    openEditor: () -> Unit,
) {
    val pal = LocalPalette.current
    val cur = if (isToday) day.current(t) else null
    val next = day.items.firstOrNull { it.start > t && !it.isSleep }
    val hero = cur ?: next
    if (hero != null) {
        val p = if (cur != null) ((t - cur.start).toFloat() / (cur.end - cur.start)).coerceIn(0f, 1f) else 0f
        val word = wordOf(hero, settings)
        val label = if (cur != null) "$word، تخلص بعد ${durText(cur.end - t)}" else "$word بعد ${durText(hero.start - t)}"
        InkWord(word, p, wordSize(word), label, Modifier.padding(top = 18.dp))
        Txt(hero.title, size = 20.sp, bold = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth(), lineHeight = 1.5f)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            if (cur != null) {
                Fact("تخلص", clock(cur.end))
                Fact("باقي", durText(cur.end - t))
            } else Fact("تبدي", clock(hero.start))
        }
    } else {
        InkWord("يومك", 1f, wordSize("يومك"), "خلص اليوم", Modifier.padding(top = 18.dp))
    }

    if (cur != null) Actions(cur, cur.key in done, settings, onToggle, openSheet, startFocus)
    NextPrayer(day, t)
    DayRail(day, t, isToday, done, settings, placed, openSheet, toggleTask, openEditor)

    val noRoom = placed.filter { it.noRoom }
    if (noRoom.isNotEmpty()) {
        Txt(
            noRoom.joinToString("، ") { "«${it.task.title}»" } + ": ما بقى فراغ يكفيها اليوم، تبقى بمهامك لباچر.",
            size = 14.5.sp, color = pal.grey, modifier = Modifier.padding(top = 10.dp),
        )
    }
    val pl = day.plan
    InkCard(
        "bed", "الليلة: تنام ${clock(pl.bedtime)} وتصحى ${clock(pl.wake)}" + if (pl.qiyam) " لصلاة الليل" else "",
        if (pl.shortfall > 0) "ينقصك ${durText(pl.shortfall)} عن 7 ساعات" else "نومك يكفي", onClick = openNight,
    )
    InkCard("page", "محاسبة المساء", "عشر نقاط وثلاث أسئلة، دقيقتين", onClick = openPage)
    Spacer(Modifier.height(24.dp))
}

/** Three clear buttons, each an icon and a word: done, focus, remind me. */
@Composable
private fun Actions(cur: Item, isDone: Boolean, settings: UserSettings, onToggle: (String) -> Unit, openSheet: (Item) -> Unit, startFocus: (Item) -> Unit) {
    val pal = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val reduced = LocalReducedMotion.current
    val press = remember { Animatable(1f) }
    val ring = remember { Animatable(0f) }
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val doneLabel = when { cur.isPrayer -> "صلّيت"; cur.isSleep -> "صحيت"; else -> "خلّصت" }
        ActionTile(
            "seal", if (isDone) "تم" else doneLabel, if (isDone) "اضغط حتى تلغي" else "علّمها", primary = true,
            modifier = Modifier.weight(1f).graphicsLayer { scaleX = press.value; scaleY = press.value }
                .drawBehind {
                    if (ring.value > 0f) {
                        val k = 1f + 0.35f * ring.value
                        drawRoundRect(
                            color = pal.ink.copy(alpha = (1f - ring.value) * 0.8f),
                            topLeft = Offset(size.width * (1 - k) / 2, size.height * (1 - k) / 2),
                            size = androidx.compose.ui.geometry.Size(size.width * k, size.height * k),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()),
                        )
                    }
                },
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (isDone || reduced) onToggle(cur.key)
            else scope.launch {
                // Pressed like a seal, and a ring of ink spreads.
                launch { press.animateTo(0.88f, tween(130)); press.animateTo(1f, tween(260)) }
                launch { ring.snapTo(0f); ring.animateTo(1f, tween(500)); ring.snapTo(0f) }
                delay(380)
                onToggle(cur.key)
            }
        }
        if (isFocusable(cur.key, cur.icon)) {
            ActionTile("nib", "ركّز", "يسكّت التنبيهات", modifier = Modifier.weight(1f)) { startFocus(cur) }
        }
        ActionTile("hourglass", "نبّهني", alertLabel(cur, settings.alerts), modifier = Modifier.weight(1f)) { openSheet(cur) }
    }
}

@Composable
fun ActionTile(icon: String, label: String, sub: String, primary: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val pal = LocalPalette.current
    val fg = if (primary) pal.paper else pal.ink
    Column(
        modifier.heightIn(min = 84.dp)
            .background(if (primary) pal.ink else pal.paper, RoundedCornerShape(20.dp))
            .border(1.5.dp, if (primary) pal.ink else pal.line, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = "$label، $sub" }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        YIcon(icon, 24.dp, fg, fg)
        Txt(label, size = 16.sp, bold = true, color = fg, align = TextAlign.Center, lineHeight = 1.4f)
        Txt(sub, size = 12.5.sp, color = if (primary) fg.copy(alpha = 0.72f) else pal.grey, align = TextAlign.Center, lineHeight = 1.4f)
    }
}

/** One line for the next prayer: prayers are items of the day, not the centre of the screen. */
@Composable
private fun NextPrayer(day: DayView, t: Int) {
    val pal = LocalPalette.current
    val cells = day.prayers(t)
    val nextCell = cells.firstOrNull { it.at > t }
    val (name, at, icon) = if (nextCell != null) Triple(nextCell.name, nextCell.at, nextCell.icon) else Triple("الفجر", day.nextFajr, "fajr")
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp)
            .background(lerp(pal.paper, Color(0xFF1E8768), if (pal.isDark) 0.12f else 0.07f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tile(icon, false, TileSize.XS)
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt("الصلاة الجاية: $name", size = 15.sp, lineHeight = 1.5f)
                Num(clock(at), size = 17.sp, color = pal.ink)
            }
            Txt("بعد ${durText(at - t)}", size = 13.5.sp, color = pal.grey, lineHeight = 1.4f)
        }
    }
}

/**
 * The whole day as one list («يومك»): a line runs down the icons and is ink up to now, like the
 * big word. Tasks sit under the item they were placed in. What passed folds into one line.
 */
@Composable
private fun DayRail(
    day: DayView, t: Int, isToday: Boolean, done: Set<String>, settings: UserSettings, placed: List<PlacedTask>,
    openSheet: (Item) -> Unit, toggleTask: (TaskItem) -> Unit, openEditor: () -> Unit,
) {
    val pal = LocalPalette.current
    val items = day.items.filter { !it.isSleep }
    var showPast by rememberSaveable(day.date) { mutableStateOf(false) }
    val firstOpen = if (isToday) items.indexOfFirst { it.end > t }.let { if (it < 0) items.size else it } else 0
    val tasksIn = placed.filter { it.slotKey != null && it.start != null }.groupBy { it.slotKey!! }
    val inGaps = placed.count { it.slotKey != null && !it.task.done }

    Row(Modifier.fillMaxWidth().padding(top = 26.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Display("يومك", 20.sp)
        Txt("${items.size} فقرة" + if (inGaps > 0) " · $inGaps مهام بالفراغات" else "", size = 13.5.sp, color = pal.grey, modifier = Modifier.weight(1f))
        if (settings.myDay != null) PillButton("عدّل يومك", openEditor)
    }
    if (day.changes.isNotEmpty()) {
        Column(
            Modifier.fillMaxWidth().padding(top = 10.dp)
                .background(lerp(pal.paper, pal.ink, 0.05f), RoundedCornerShape(14.dp))
                .drawBehind { drawLine(pal.ink, Offset(size.width - 1.5.dp.toPx(), 0f), Offset(size.width - 1.5.dp.toPx(), size.height), 3.dp.toPx()) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Txt("شنو تغيّر اليوم", size = 14.sp, bold = true, lineHeight = 1.5f)
            for (c in day.changes) Txt(c, size = 14.5.sp, lineHeight = 1.7f)
        }
    }
    if (firstOpen > 0) {
        val n = firstOpen
        Box(
            Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp)
                .border(1.5.dp, pal.line, RoundedCornerShape(14.dp))
                .clickable { showPast = !showPast }.semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Txt(
                if (showPast) "خفي اللي فات" else when (n) { 1 -> "فاتت فقرة وحدة، شوفها"; 2 -> "فاتت فقرتين، شوفهن"; else -> "فاتت $n فقرات، شوفها" },
                size = 15.sp, bold = true, color = pal.grey, align = TextAlign.Center,
            )
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        items.forEachIndexed { i, it ->
            if (i < firstOpen && !showPast) return@forEachIndexed
            val past = isToday && it.end <= t
            val now = isToday && it.start <= t && t < it.end
            val fill = when { past -> 1f; now -> ((t - it.start).toFloat() / (it.end - it.start)).coerceIn(0f, 1f); else -> 0f }
            RailRow(it, past, now, fill, it.key in done, t, settings.alerts, first = i == 0, last = i == items.lastIndex) { openSheet(it) }
            for (p in tasksIn[it.key].orEmpty().sortedBy { x -> x.start }) TaskLine(p, fill) { toggleTask(p.task) }
        }
    }
}

@Composable
private fun RailRow(it: Item, past: Boolean, now: Boolean, fill: Float, isDone: Boolean, t: Int, alerts: AlertPrefs, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val pal = LocalPalette.current
    val reduced = LocalReducedMotion.current
    val mark = reminderMark(it, alerts)
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = if (now) 4.dp else 0.dp)
            .background(if (now) lerp(pal.paper, pal.ink, 0.05f) else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = "${it.title}، ${clock(it.start)} إلى ${clock(it.end)}" + if (isDone) "، خلصت" else "" }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(50.dp), contentAlignment = Alignment.Center) { Num(clock(it.start), size = 17.sp, color = if (now) pal.ink else pal.grey) }
        Box(
            Modifier.width(34.dp).fillMaxHeight().drawBehind {
                val x = size.width / 2
                val top = if (first) size.height / 2 else -8.dp.toPx()
                val bottom = if (last) size.height / 2 else size.height + 8.dp.toPx()
                val split = top + (bottom - top) * fill
                if (split > top) drawLine(pal.ink, Offset(x, top), Offset(x, split), 2.dp.toPx())
                if (bottom > split) drawLine(pal.faint, Offset(x, split), Offset(x, bottom), 2.dp.toPx())
            },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.background(pal.paper, RoundedCornerShape(12.dp)).graphicsLayer { alpha = if (past) 0.55f else 1f }) {
                Tile(it.icon, it.isPrayer, TileSize.SM, done = past)
            }
            if (now) NowDot(reduced, Modifier.align(Alignment.BottomCenter))
        }
        Column(Modifier.weight(1f)) {
            Txt(it.title, size = if (now) 18.sp else 16.5.sp, bold = now || it.isPrayer, color = if (past) pal.grey else pal.ink, lineHeight = 1.5f)
            if (now) {
                Box(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp).height(5.dp).background(pal.faint, CircleShape)) {
                    Box(Modifier.fillMaxWidth(fill).height(5.dp).background(pal.ink, CircleShape))
                }
                Txt("باقي ${durText(it.end - t)} · تخلص ${clock(it.end)}", size = 13.5.sp, color = pal.grey, lineHeight = 1.5f)
            } else if (it.shrunkFrom != null) {
                Txt("قصرت اليوم من ${durShort(it.shrunkFrom)}", size = 13.5.sp, color = pal.grey, lineHeight = 1.5f)
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 10.dp)) {
            if (isDone) Check(true) else Num(durShort(it.end - it.start), size = 16.sp)
            if (mark != null) {
                Box(Modifier.padding(top = 2.dp).border(1.dp, pal.line, CircleShape).padding(horizontal = 7.dp)) { Txt(mark, size = 12.sp, color = pal.grey, lineHeight = 1.4f) }
            }
        }
    }
}

/** The reminder mark shows only when it differs from the usual choice. */
private fun reminderMark(it: Item, a: AlertPrefs): String? {
    prayerOf(it.key)?.let { p ->
        return when (a.prayerAlert(p)) { PrayerAlert.NOTIFY -> null; PrayerAlert.ADHAN -> "أذان"; PrayerAlert.NONE -> "بلا تنبيه" }
    }
    if (isFreeKey(it.key)) return null
    val r = a.itemAlert(it.key)
    return if (r == a.itemDefault) null else if (r == ItemAlert.OFF) "بلا تنبيه" else r.label
}

/** The now-dot on the day line breathes slowly. */
@Composable
private fun NowDot(reduced: Boolean, modifier: Modifier) {
    val pal = LocalPalette.current
    val k = if (reduced) 0f else {
        val inf = rememberInfiniteTransition(label = "now")
        inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Restart), label = "breath").value
    }
    Box(modifier.size(10.dp).drawBehind {
        if (k > 0f) drawCircle(pal.ink.copy(alpha = 0.5f * (1f - k)), radius = size.minDimension / 2 * (1f + 1.6f * k))
        drawCircle(pal.ink)
    })
}

/** A task under its item: a round check, and an ink line drawn through it when done. */
@Composable
private fun TaskLine(p: PlacedTask, fill: Float, onToggle: () -> Unit) {
    val pal = LocalPalette.current
    val reduced = LocalReducedMotion.current
    val strike by animateFloatAsState(if (p.task.done) 1f else 0f, tween(if (reduced) 0 else 500), label = "strike")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 108.dp)
            .clickable(onClick = onToggle)
            .semantics { role = Role.Checkbox; contentDescription = p.task.title; stateDescription = if (p.task.done) "خلصت" else "باقية" },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Check(p.task.done)
        Column(Modifier.weight(1f)) {
            Txt(
                p.task.title, size = 15.5.sp, bold = true, color = if (p.task.done) pal.grey else pal.ink, lineHeight = 1.45f,
                modifier = Modifier.drawWithContent {
                    drawContent()
                    if (strike > 0f) drawLine(pal.grey, Offset(size.width, size.height * 0.55f), Offset(size.width * (1f - strike), size.height * 0.55f), 1.5.dp.toPx())
                },
            )
            Num("${clock(p.start ?: 0)} · ${p.task.minutes} د" + if (p.pinned) " · وقت ثابت" else "", size = 14.sp)
        }
    }
}

/** Seven days from Saturday; a dot marks a day with open tasks. */
@Composable
fun WeekStrip(today: LocalDate, shown: LocalDate, hasTasks: (LocalDate) -> Boolean, onPick: (LocalDate) -> Unit) {
    val pal = LocalPalette.current
    val back = (today.dayOfWeek.ordinal + 2) % 7 // Monday=0 … Saturday=5 → days since Saturday
    val saturday = today.minusDays(back)
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0..6) {
            val d = saturday.plusDays(i)
            val sel = d == shown
            val isToday = d == today
            Column(
                Modifier.weight(1f).heightIn(min = 62.dp).clickable { onPick(d) }
                    .semantics { role = Role.Button; contentDescription = "${DayShort[d.dayOfWeek.ordinal]} ${d.day}" + if (isToday) "، اليوم" else "" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Txt(DayShort[d.dayOfWeek.ordinal], size = 12.sp, color = pal.grey, align = TextAlign.Center, lineHeight = 1.4f)
                Box(
                    Modifier.size(34.dp)
                        .background(if (sel) pal.ink else Color.Transparent, CircleShape)
                        .border(if (isToday && !sel) 1.5.dp else 0.dp, if (isToday && !sel) pal.ink else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Num(d.day.toString(), size = 19.sp, color = if (sel) pal.paper else if (d < today) pal.grey else pal.ink) }
                Box(Modifier.padding(top = 2.dp).size(5.dp).background(if (hasTasks(d)) pal.ink.copy(alpha = 0.55f) else Color.Transparent, CircleShape))
            }
        }
    }
}

/** Monday first, as kotlinx-datetime orders DayOfWeek. */
val DayShort = listOf("اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت", "أحد")

fun LocalDate.plusDays(n: Int): LocalDate = kotlinx.datetime.LocalDate.fromEpochDays(toEpochDays() + n)
fun LocalDate.minusDays(n: Int): LocalDate = plusDays(-n)
