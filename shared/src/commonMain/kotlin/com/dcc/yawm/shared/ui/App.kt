package com.dcc.yawm.shared.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.Remind
import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.day.nextThingId
import com.dcc.yawm.shared.screen.Where
import com.dcc.yawm.shared.screen.partNow
import com.dcc.yawm.shared.screen.partsView
import com.dcc.yawm.shared.screen.savedMessage
import com.dcc.yawm.shared.screen.toAlarm
import com.dcc.yawm.shared.screen.toThing
import com.dcc.yawm.shared.screen.whereName
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
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
import com.dcc.yawm.shared.screen.Field
import com.dcc.yawm.shared.screen.spectrum
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

private enum class Overlay { NONE, SETTINGS, REVIEW, NIGHT, PAGE, SOUNDS, ALERTS, WEEK }

private enum class SheetKind { NONE, ADD, ADD_ALARM, THING }

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
        // «الألوان»: while today's home is shown, the whole screen (status bar included) takes its field's colour.
        var homeField by remember { mutableStateOf<Field?>(null) }
        val fieldColours = homeField?.let { FieldColour.getValue(it) }
        val bg by animateColorAsState(fieldColours?.bg ?: pal.paper, tween(if (platform.reducedMotion) 0 else 260), label = "paper")
        val barsDark = fieldColours?.isDark ?: dark
        androidx.compose.runtime.SideEffect { platform.systemBars(barsDark) }
        CompositionLocalProvider(LocalPalette provides pal) {
            Column(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding()) {
                if (!settings.setupDone) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                        FirstRun(settings, status, device, platform, save)
                    }
                    androidx.compose.runtime.SideEffect { homeField = null }
                } else {
                    MainScreens(data, device, platform, settings, moment, save) { homeField = it }
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
    onHomeField: (Field?) -> Unit,
) {
    val pal = LocalPalette.current
    val scope = rememberCoroutineScope()
    val status by device.status.collectAsState()
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var sheet by remember { mutableStateOf(SheetKind.NONE) }
    var sheetThing by remember { mutableStateOf<String?>(null) }
    var addPart by remember { mutableStateOf<DayPart?>(null) }
    var shownDate by remember { mutableStateOf<LocalDate?>(null) }
    var editingAlarm by remember { mutableStateOf<String?>(null) }
    var toastText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastText) { if (toastText != null) { delay(2400); toastText = null } }
    val toast: (String) -> Unit = { toastText = it }

    platform.BackHandler(sheet != SheetKind.NONE || overlay != Overlay.NONE || shownDate != null) {
        when {
            sheet != SheetKind.NONE -> sheet = SheetKind.NONE
            overlay == Overlay.SOUNDS || overlay == Overlay.REVIEW -> overlay = Overlay.SETTINGS
            overlay != Overlay.NONE -> overlay = Overlay.NONE
            else -> shownDate = null
        }
    }
    val date = shownDate ?: moment.date
    val isToday = date == moment.date
    val day = remember(settings, date) { dayView(settings, date) }
    val todayView = remember(settings, moment.date) { dayView(settings, moment.date) }
    val view = remember(settings, date, moment.date, moment.t) { partsView(settings, date, moment.date, if (isToday) moment.t else null) }
    val done by remember(date) { data.dayLog.blocksDoneOn(date) }.collectAsState(emptySet())
    val scroll = rememberScrollState()
    LaunchedEffect(overlay, shownDate) { scroll.scrollTo(0) }
    // The real clock (after midnight the app day is still yesterday until Fajr).
    val realToday = if (moment.t >= 1440) moment.date.plusDays(1) else moment.date
    val realNow = moment.t % 1440

    fun putThing(id: String, f: (Thing) -> Thing) = save(settings.copy(things = settings.things.map { if (it.id == id) f(it) else it }))
    fun tick(x: Thing) {
        putThing(x.id) { it.toggled(date) }
        if (!x.doneOn(date)) toast("تم ✓")
    }
    fun pray(key: String) { scope.launch { data.dayLog.setBlockDone(date, key, key !in done) } }

    // «الألوان»: today's home. The field follows the time until another is tapped on the spectrum.
    val home = overlay == Overlay.NONE && isToday
    val spec = remember(view) { spectrum(view) }
    var picked by remember(moment.date) { mutableStateOf<Field?>(null) }
    val field = picked ?: spec.fieldAt(moment.t)
    androidx.compose.runtime.SideEffect { onHomeField(if (home) field else null) }

    Box(Modifier.weight(1f)) {
        if (home) ColorsHome(
            view, spec, moment.t, field, done, platform.hijri(date), settings.wakeAlarm,
            onSelect = { f -> picked = if (f == spec.fieldAt(moment.t)) null else f },
            onTick = ::tick,
            onOpen = { sheetThing = it.id; sheet = SheetKind.THING },
            onPray = ::pray,
            onAdd = { addPart = it; sheet = SheetKind.ADD },
            onWake = { save(settings.copy(wakeAlarm = it)) },
            openSettings = { overlay = Overlay.SETTINGS },
            openAlarms = { overlay = Overlay.ALERTS },
        ) else Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 24.dp)) {
            when (overlay) {
                Overlay.SETTINGS -> SettingsScreen(
                    settings, status, device, platform, save,
                    openNight = { overlay = Overlay.NIGHT },
                    openAlerts = { overlay = Overlay.ALERTS },
                    openWeek = { overlay = Overlay.WEEK },
                    openPage = { overlay = Overlay.PAGE },
                ) { overlay = Overlay.NONE }
                Overlay.SOUNDS -> SoundsScreen(settings, todayView, moment.t, platform) { overlay = Overlay.ALERTS }
                Overlay.ALERTS -> {
                    SideTop(moment.date, platform.hijri(moment.date)) { overlay = Overlay.NONE }
                    AlarmsScreen(settings, todayView, moment.t, status, device, save, editingAlarm, { editingAlarm = it }) { overlay = Overlay.SOUNDS }
                }
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
                Overlay.WEEK -> {
                    SideTop(moment.date, platform.hijri(moment.date)) { overlay = Overlay.NONE }
                    val week = remember(settings, moment.date) { weekView(settings, moment.date) }
                    WeekScreen(week, moment.date, moment.t, openDay = { d ->
                        shownDate = if (d == moment.date) null else d
                        overlay = Overlay.NONE
                    }, openReview = { overlay = Overlay.REVIEW })
                }
                Overlay.REVIEW -> {
                    val week = remember(settings, moment.date) { weekView(settings, moment.date) }
                    var points by remember { mutableStateOf(0) }
                    LaunchedEffect(week.saturday) {
                        points = data.dayLog.doneBetween(week.saturday, week.friday).values
                            .sumOf { d -> checklistScore(d, settings.roles).done }
                    }
                    ReviewScreen(week, points, 7 * ChecklistPoint.visibleFor(settings.roles).size) { overlay = Overlay.WEEK }
                }
                Overlay.NONE -> DayPage(
                    view, moment.t, isToday, done, platform.hijri(date), settings.wakeAlarm,
                    onTick = ::tick,
                    onOpen = { sheetThing = it.id; sheet = SheetKind.THING },
                    onPray = ::pray,
                    onAdd = { addPart = it; sheet = SheetKind.ADD },
                    onAddAlarm = { sheet = SheetKind.ADD_ALARM },
                    onAlarm = { a, on -> save(settings.copy(alerts = settings.alerts.copy(alarms = settings.alerts.alarms.map { if (it.id == a.id) it.copy(on = on) else it }))) },
                    onWake = { save(settings.copy(wakeAlarm = it)) },
                    openSettings = { overlay = Overlay.SETTINGS },
                    backToToday = if (isToday) null else ({ shownDate = null }),
                )
            }
        }
        when (sheet) {
            SheetKind.ADD, SheetKind.ADD_ALARM -> AddSheet(
                alarmMode = sheet == SheetKind.ADD_ALARM,
                defaultPart = addPart ?: partNow(view, if (isToday) moment.t else null),
                spans = view.parts.map { it.span },
                nowMinute = realNow,
                fallback = field,
                close = { sheet = SheetKind.NONE },
            ) { d ->
                if (d.alarm) {
                    val a = d.toAlarm(settings.alerts.nextAlarmId(), settings.workDays, realToday, realNow)
                    save(settings.copy(alerts = settings.alerts.copy(alarms = settings.alerts.alarms + a)))
                } else {
                    save(settings.copy(things = settings.things + d.toThing(nextThingId(settings.things), date)))
                }
                toast(d.savedMessage())
                sheet = SheetKind.NONE
                addPart = null
            }
            SheetKind.THING -> settings.things.firstOrNull { it.id == sheetThing }?.let { x ->
                val inPart = view.parts.firstOrNull { p -> p.things.any { it.id == x.id } }?.part
                val where: Where? = when {
                    x.isLater -> Where.Later
                    x.repeat == Repeat.ONCE && x.date != null && x.date > moment.date -> Where.Tomorrow
                    else -> inPart?.let { Where.In(it) }
                }
                ThingSheet(
                    x, date, where, where?.let { whereName(it) } ?: "اليوم",
                    field = inPart?.let { Field.of(it) } ?: field,
                    close = { sheet = SheetKind.NONE },
                    onToggle = { tick(x); sheet = SheetKind.NONE },
                    onMove = { w ->
                        putThing(x.id) {
                            when (w) {
                                Where.Later -> it.copy(date = null, part = null, at = null, remind = Remind.NONE)
                                Where.Tomorrow -> it.copy(date = moment.date.plusDays(1))
                                is Where.In -> it.copy(date = moment.date, part = w.part, at = null, remind = if (it.remind == Remind.AT) Remind.START else it.remind)
                            }
                        }
                        // The sheet stays open and takes the colour of the part it moved to.
                        toast("انتقلت لـ" + whereName(w))
                    },
                    onRemind = { r -> putThing(x.id) { it.copy(remind = r) } },
                    onRepeat = { r -> putThing(x.id) { it.copy(repeat = r, date = if (r == Repeat.ONCE) (it.date ?: moment.date) else it.date) } },
                    onDelete = { save(settings.copy(things = settings.things.filter { it.id != x.id })); toast("انحذفت"); sheet = SheetKind.NONE },
                )
            }
            SheetKind.NONE -> {}
        }
        toastText?.let { msg ->
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = if (home) 100.dp else 90.dp).background(pal.ink, RoundedCornerShape(999.dp)).padding(horizontal = 18.dp, vertical = 10.dp)) {
                Txt(msg, size = 15.sp, color = pal.paper, lineHeight = 1.4f)
            }
        }
        if (overlay == Overlay.NONE && sheet == SheetKind.NONE && !home) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(18.dp).heightIn(min = 56.dp)
                    .background(pal.ink, RoundedCornerShape(999.dp)).clickable { addPart = null; sheet = SheetKind.ADD }
                    .semantics { role = Role.Button; contentDescription = "أضف" }.padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) { Txt("+ أضف", size = 18.sp, bold = true, color = pal.paper, lineHeight = 1.3f) }
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
