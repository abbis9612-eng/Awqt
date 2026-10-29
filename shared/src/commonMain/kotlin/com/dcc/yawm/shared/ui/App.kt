package com.dcc.yawm.shared.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.day.TaskItem
import com.dcc.yawm.shared.day.tasksOn
import com.dcc.yawm.shared.screen.Item
import com.dcc.yawm.shared.screen.MonthNames
import com.dcc.yawm.shared.screen.dayName
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dcc.yawm.shared.alarm.DeviceControls
import com.dcc.yawm.shared.data.Theme
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.data.YawmData
import com.dcc.yawm.shared.day.ChecklistPoint
import com.dcc.yawm.shared.day.checklistScore
import com.dcc.yawm.shared.prayer.GeoPoint
import com.dcc.yawm.shared.screen.AppMoment
import com.dcc.yawm.shared.screen.appMoment
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.dateLine
import com.dcc.yawm.shared.screen.dayView
import com.dcc.yawm.shared.screen.weekView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** What only the platform can give the screens. */
interface Platform {
    val fonts: YawmFonts
    val reducedMotion: Boolean
    val systemZoneId: String
    /** "6 جمادى الأولى 1448" (Umm al-Qura). */
    fun hijri(date: LocalDate): String
    /** Approximate location, on the device only (CLAUDE.md rule 5); null if not allowed yet. */
    fun locate(onResult: (GeoPoint?) -> Unit)
    @Composable
    fun BackHandler(enabled: Boolean, onBack: () -> Unit)
    /** Light or dark status and navigation bar icons, following the paper. */
    fun systemBars(dark: Boolean)
    /** Plays one of the app's own sounds once (res/raw, by file name). */
    fun playSound(file: String)
    /** Shows the alarm screen now, ringing, to try it. */
    fun testAlarm()
}

private enum class Tab(val label: String, val icon: String) {
    TODAY("اليوم", "dhuhr"), TASKS("المهام", "seal"), ALARMS("المنبّه", "hourglass"), WEEK("الأسبوع", "week"),
}

private enum class Overlay { NONE, SETTINGS, REVIEW, DAY_EDITOR, MY_DAY, NIGHT, PAGE, SOUNDS }

private enum class SheetKind { NONE, ITEM, ADD_MENU, QUICK_ADD }

@OptIn(ExperimentalTime::class)
@Composable
fun App(data: YawmData, device: DeviceControls, platform: Platform) {
    val stored by remember { data.settings.settings }.collectAsState(null)
    val status by device.status.collectAsState()
    val scope = rememberCoroutineScope()
    val save: (UserSettings) -> Unit = { s ->
        scope.launch {
            data.settings.save(s)
            device.reschedule()
        }
    }

    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val s = Clock.System.now().epochSeconds % 60
            delay((60 - s) * 1000L)
            now = Clock.System.now()
        }
    }

    val settings = stored
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalFonts provides platform.fonts,
        LocalReducedMotion provides platform.reducedMotion,
    ) {
        if (settings == null) {
            Box(Modifier.fillMaxSize().background(LightPalette.paper))
        } else {
            Screens(data, device, platform, settings, status, save, now)
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun Screens(
    data: YawmData,
    device: DeviceControls,
    platform: Platform,
    settings: UserSettings,
    status: com.dcc.yawm.shared.alarm.DeviceStatus,
    save: (UserSettings) -> Unit,
    now: kotlin.time.Instant,
) {
    run {
        val moment = appMoment(now, settings)
        val today = remember(settings, moment.date) { dayView(settings, moment.date) }
        val dark = when (settings.theme) {
            Theme.AUTO -> moment.t >= today.times.isha
            Theme.LIGHT -> false
            Theme.DARK -> true
        }
        val pal = if (dark) DarkPalette else LightPalette
        val bg by animateColorAsState(pal.paper, tween(if (platform.reducedMotion) 0 else 400), label = "paper")
        androidx.compose.runtime.SideEffect { platform.systemBars(dark) }
        CompositionLocalProvider(LocalPalette provides pal) {
            Column(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding()) {
                if (!settings.setupDone) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                        FirstRunBuilder(settings, status, device, platform, moment.date, save)
                    }
                } else {
                    MainScreens(data, device, platform, settings, moment, save)
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.MainScreens(
    data: YawmData,
    device: DeviceControls,
    platform: Platform,
    settings: UserSettings,
    moment: AppMoment,
    save: (UserSettings) -> Unit,
) {
    val pal = LocalPalette.current
    val scope = rememberCoroutineScope()
    val status by device.status.collectAsState()
    var tab by remember { mutableStateOf(Tab.TODAY) }
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var sheet by remember { mutableStateOf(SheetKind.NONE) }
    var sheetKey by remember { mutableStateOf<String?>(null) }
    var shownDate by remember { mutableStateOf<LocalDate?>(null) }
    var editingAlarm by remember { mutableStateOf<String?>(null) }
    var toastText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastText) { if (toastText != null) { delay(2400); toastText = null } }
    val toast: (String) -> Unit = { toastText = it }

    platform.BackHandler(sheet != SheetKind.NONE || overlay != Overlay.NONE || shownDate != null || tab != Tab.TODAY) {
        when {
            sheet != SheetKind.NONE -> sheet = SheetKind.NONE
            overlay != Overlay.NONE -> overlay = Overlay.NONE
            shownDate != null -> shownDate = null
            else -> tab = Tab.TODAY
        }
    }
    val date = shownDate ?: moment.date
    val isToday = date == moment.date
    val day = remember(settings, date) { dayView(settings, date) }
    val todayView = remember(settings, moment.date) { dayView(settings, moment.date) }
    val done by remember(date) { data.dayLog.blocksDoneOn(date) }.collectAsState(emptySet())
    val placed = remember(settings, date, moment.t) { placedTasksOf(day, settings, date, moment.date, moment.t) }
    val placedToday = remember(settings, moment.date, moment.t) { placedTasksOf(todayView, settings, moment.date, moment.date, moment.t) }
    val scroll = rememberScrollState()
    LaunchedEffect(tab, overlay, shownDate) { scroll.scrollTo(0) }
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val focusUntil = settings.focusUntil?.takeIf { it > nowMs }

    fun toggleTask(x: TaskItem) = save(settings.copy(tasks = settings.tasks.map { if (it.id == x.id) it.copy(doneOn = if (it.done) null else moment.date) else it }))
    fun startFocus(it: Item) {
        val ms = Clock.System.now().toEpochMilliseconds()
        save(settings.copy(focusFrom = ms, focusUntil = ms + (it.end - moment.t).coerceAtLeast(5) * 60_000L))
        sheet = SheetKind.NONE
    }
    fun toggleDone(key: String) { scope.launch { data.dayLog.setBlockDone(date, key, key !in done) } }

    Box(Modifier.weight(1f)) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 24.dp)) {
            when (overlay) {
                Overlay.SETTINGS -> SettingsScreen(
                    settings, status, device, platform, save,
                    openMyDay = { overlay = Overlay.MY_DAY },
                    openDayEditor = { overlay = Overlay.DAY_EDITOR },
                ) { overlay = Overlay.NONE }
                Overlay.DAY_EDITOR -> DayEditorScreen(settings, save) { overlay = Overlay.NONE }
                Overlay.MY_DAY -> MyDayEditorScreen(settings, moment.date, save) { overlay = Overlay.NONE }
                Overlay.SOUNDS -> SoundsScreen(settings, todayView, moment.t, platform) { overlay = Overlay.NONE }
                Overlay.NIGHT -> {
                    SideTop(date, platform.hijri(date)) { overlay = Overlay.NONE }
                    NightScreen(day, moment.t, settings, save)
                }
                Overlay.PAGE -> {
                    SideTop(moment.date, platform.hijri(moment.date)) { overlay = Overlay.NONE }
                    val pdone by remember(moment.date) { data.dayLog.doneOn(moment.date) }.collectAsState(emptySet())
                    val saved by remember(moment.date) { data.dayLog.observePage(moment.date) }.collectAsState(null)
                    PageScreen(
                        shown = ChecklistPoint.visibleFor(settings.roles),
                        done = pdone,
                        score = checklistScore(pdone, settings.roles),
                        saved = saved,
                        onToggle = { p -> scope.launch { data.dayLog.setDone(moment.date, p, p !in pdone) } },
                        onSave = { page -> scope.launch { data.dayLog.savePage(moment.date, page) } },
                    )
                }
                Overlay.REVIEW -> {
                    val week = remember(settings, moment.date) { weekView(settings, moment.date) }
                    var points by remember { mutableStateOf(0) }
                    LaunchedEffect(week.saturday) {
                        points = data.dayLog.doneBetween(week.saturday, week.friday).values
                            .sumOf { d -> checklistScore(d, settings.roles).done }
                    }
                    ReviewScreen(week, points, 7 * ChecklistPoint.visibleFor(settings.roles).size) { overlay = Overlay.NONE }
                }
                Overlay.NONE -> {
                    TopRow(date, platform.hijri(date), moment.t) { overlay = Overlay.SETTINGS }
                    when (tab) {
                        Tab.TODAY -> {
                            WeekStrip(moment.date, date, { d -> tasksOn(d, moment.date, settings.tasks).any { !it.done } }) { d ->
                                shownDate = if (d == moment.date) null else d
                            }
                            TodayScreen(
                                day, moment.t, isToday, done, settings, placed,
                                onToggle = ::toggleDone,
                                openSheet = { sheetKey = it.key; sheet = SheetKind.ITEM },
                                startFocus = ::startFocus,
                                toggleTask = ::toggleTask,
                                openNight = { overlay = Overlay.NIGHT },
                                openPage = { overlay = Overlay.PAGE },
                                openEditor = { overlay = Overlay.MY_DAY },
                            )
                        }
                        Tab.TASKS -> TasksScreen(settings, moment.date, moment.t, placedToday, { k -> todayView.items.firstOrNull { it.key == k }?.title }, save, toast)
                        Tab.ALARMS -> AlarmsScreen(settings, todayView, moment.t, status, device, save, editingAlarm, { editingAlarm = it }) { overlay = Overlay.SOUNDS }
                        Tab.WEEK -> {
                            val week = remember(settings, moment.date) { weekView(settings, moment.date) }
                            WeekScreen(week, moment.date, moment.t, openDay = { d ->
                                shownDate = if (d == moment.date) null else d
                                tab = Tab.TODAY
                            }, openReview = { overlay = Overlay.REVIEW })
                        }
                    }
                }
            }
        }
        when (sheet) {
            SheetKind.ITEM -> day.items.firstOrNull { it.key == sheetKey }?.let { item ->
                Sheet(item.title, { sheet = SheetKind.NONE }) {
                    ItemSheetBody(
                        item, moment.t, isToday, item.key in done, settings,
                        onDone = { toggleDone(item.key) },
                        onFocus = { startFocus(item) },
                        onEdit = { sheet = SheetKind.NONE; overlay = if (settings.myDay != null) Overlay.MY_DAY else Overlay.DAY_EDITOR },
                        save = save,
                    )
                }
            }
            SheetKind.ADD_MENU -> Sheet("أضف", { sheet = SheetKind.NONE }) {
                AddMenuBody(
                    onTask = { sheet = SheetKind.QUICK_ADD },
                    onAlarm = {
                        val id = settings.alerts.nextAlarmId()
                        save(settings.copy(alerts = settings.alerts.copy(alarms = settings.alerts.alarms + com.dcc.yawm.shared.alarm.UserAlarm(id, "منبّه", 7 * 60))))
                        editingAlarm = id; tab = Tab.ALARMS; overlay = Overlay.NONE; sheet = SheetKind.NONE
                    },
                    onItem = { sheet = SheetKind.NONE; overlay = if (settings.myDay != null) Overlay.MY_DAY else Overlay.DAY_EDITOR },
                )
            }
            SheetKind.QUICK_ADD -> Sheet("مهمة جديدة", { sheet = SheetKind.NONE }) {
                Txt("مهمة جديدة", size = 21.sp, bold = true, modifier = Modifier.padding(bottom = 6.dp))
                QuickAddForm(moment.date, moment.t) { task, msg ->
                    save(settings.copy(tasks = settings.tasks + task)); toast(msg); sheet = SheetKind.NONE
                }
            }
            SheetKind.NONE -> {}
        }
        if (focusUntil != null) {
            FocusScreen(
                todayView.current(moment.t)?.title ?: "تركيز", settings.focusFrom ?: nowMs, focusUntil,
                onStop = { save(settings.copy(focusUntil = null, focusFrom = null)) },
                onMore = { save(settings.copy(focusUntil = focusUntil + 5 * 60_000L)) },
            )
        }
        toastText?.let { msg ->
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).background(pal.ink, RoundedCornerShape(999.dp)).padding(horizontal = 18.dp, vertical = 10.dp)) {
                Txt(msg, size = 15.sp, color = pal.paper, lineHeight = 1.4f)
            }
        }
    }
    if (overlay == Overlay.NONE && focusUntil == null) TabBar(tab, pal, onAdd = { sheet = SheetKind.ADD_MENU }) { tab = it; shownDate = null }
}

@Composable
private fun TopRow(date: LocalDate, hijri: String, t: Int, openSettings: () -> Unit) {
    val pal = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Display(dayName(date), 26.sp)
            Txt("${date.day} ${MonthNames[date.month.ordinal]} · $hijri", size = 14.5.sp, color = pal.grey, lineHeight = 1.5f)
        }
        Column(horizontalAlignment = Alignment.End) {
            Num(clock(t), size = 24.sp, color = pal.ink)
            Box(
                Modifier.heightIn(min = 48.dp).clickable(onClick = openSettings).semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) { Txt("الإعدادات", size = 14.5.sp, bold = true, color = pal.grey) }
        }
    }
}

/** The header of Night and Page, which open from Today's cards. */
@Composable
private fun SideTop(date: LocalDate, hijri: String, back: () -> Unit) {
    val pal = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Txt(dateLine(date), size = 18.sp, bold = true, lineHeight = 1.5f)
            Txt(hijri, size = 14.5.sp, color = pal.grey, lineHeight = 1.5f)
        }
        PillButton("رجوع لليوم", back)
    }
}

@Composable
private fun TabBar(current: Tab, pal: Palette, onAdd: () -> Unit, onSelect: (Tab) -> Unit) {
    val idle = if (pal.isDark) TabIdleDark else TabIdleLight
    Row(
        Modifier.fillMaxWidth().background(pal.paper)
            .drawBehind { drawLine(pal.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        @Composable
        fun item(tab: Tab) {
            val on = tab == current
            Column(
                Modifier.weight(1f).heightIn(min = 56.dp).clickable { onSelect(tab) }
                    .semantics { role = Role.Tab; selected = on; contentDescription = tab.label },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.background(if (on) pal.ink.copy(alpha = 0.09f) else Color.Transparent, RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                ) { YIcon(tab.icon, 26.dp, if (on) pal.ink else idle, if (on) accentOf(tab.icon, pal.isDark) else idle) }
                Txt(tab.label, size = 11.5.sp, bold = on, color = if (on) pal.ink else idle, lineHeight = 1.3f)
            }
        }
        item(Tab.TODAY)
        item(Tab.TASKS)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(52.dp).background(pal.ink, CircleShape).clickable(onClick = onAdd)
                    .semantics { role = Role.Button; contentDescription = "أضف" },
                contentAlignment = Alignment.Center,
            ) { Txt("+", size = 30.sp, color = pal.paper, align = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 1.0f) }
        }
        item(Tab.ALARMS)
        item(Tab.WEEK)
    }
}
