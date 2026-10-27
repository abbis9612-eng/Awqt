package com.dcc.yawm.shared.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
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
}

private enum class Tab(val label: String, val icon: String) {
    TODAY("اليوم", "dhuhr"), NIGHT("الليل", "isha"), WEEK("الأسبوع", "week"), PAGE("المحاسبة", "page"),
}

private enum class Overlay { NONE, SETTINGS, REVIEW, DAY_EDITOR }

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
                        FirstRunScreen(settings, status, device, platform, save)
                    }
                } else {
                    MainScreens(data, device, platform, settings, moment, save)
                }
            }
        }
    }
}

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
    var shownDate by remember { mutableStateOf<LocalDate?>(null) }
    platform.BackHandler(overlay != Overlay.NONE || shownDate != null || tab != Tab.TODAY) {
        when {
            overlay != Overlay.NONE -> overlay = Overlay.NONE
            shownDate != null -> shownDate = null
            else -> tab = Tab.TODAY
        }
    }
    val date = shownDate ?: moment.date
    val day = remember(settings, date) { dayView(settings, date) }
    val scroll = rememberScrollState()
    LaunchedEffect(tab, overlay, shownDate) { scroll.scrollTo(0) }

    Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 24.dp)) {
        when (overlay) {
            Overlay.SETTINGS -> SettingsScreen(settings, status, device, platform, save, openDayEditor = { overlay = Overlay.DAY_EDITOR }) { overlay = Overlay.NONE }
            Overlay.DAY_EDITOR -> DayEditorScreen(settings, save) { overlay = Overlay.NONE }
            Overlay.REVIEW -> {
                val week = remember(settings, moment.date) { weekView(settings, moment.date) }
                var points by remember { mutableStateOf(0) }
                LaunchedEffect(week.saturday) {
                    points = data.dayLog.doneBetween(week.saturday, week.friday).values
                        .sumOf { done -> checklistScore(done, settings.roles).done }
                }
                ReviewScreen(week, points, 7 * ChecklistPoint.visibleFor(settings.roles).size) { overlay = Overlay.NONE }
            }
            Overlay.NONE -> {
                TopRow(date, platform.hijri(date), moment.t, shownDate != null, onBack = { shownDate = null }) { overlay = Overlay.SETTINGS }
                when (tab) {
                    Tab.TODAY -> {
                        val done by remember(date) { data.dayLog.blocksDoneOn(date) }.collectAsState(emptySet())
                        TodayScreen(day, moment.t, done, openEditor = { overlay = Overlay.DAY_EDITOR }) { key -> scope.launch { data.dayLog.setBlockDone(date, key, key !in done) } }
                    }
                    Tab.NIGHT -> NightScreen(day, moment.t, settings, save)
                    Tab.WEEK -> {
                        val week = remember(settings, moment.date) { weekView(settings, moment.date) }
                        WeekScreen(week, moment.date, moment.t, openDay = { d ->
                            shownDate = if (d == moment.date) null else d
                            tab = Tab.TODAY
                        }, openReview = { overlay = Overlay.REVIEW })
                    }
                    Tab.PAGE -> {
                        val done by remember(moment.date) { data.dayLog.doneOn(moment.date) }.collectAsState(emptySet())
                        val saved by remember(moment.date) { data.dayLog.observePage(moment.date) }.collectAsState(null)
                        PageScreen(
                            shown = ChecklistPoint.visibleFor(settings.roles),
                            done = done,
                            score = checklistScore(done, settings.roles),
                            saved = saved,
                            onToggle = { p -> scope.launch { data.dayLog.setDone(moment.date, p, p !in done) } },
                            onSave = { page -> scope.launch { data.dayLog.savePage(moment.date, page) } },
                        )
                    }
                }
            }
        }
    }
    if (overlay == Overlay.NONE) TabBar(tab, pal) { tab = it; shownDate = null }
}

@Composable
private fun TopRow(date: LocalDate, hijri: String, t: Int, viewingOtherDay: Boolean, onBack: () -> Unit, openSettings: () -> Unit) {
    val pal = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Txt(dateLine(date), size = 18.sp, bold = true, lineHeight = 1.5f)
            Txt(hijri, size = 14.5.sp, color = pal.grey, lineHeight = 1.5f)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (viewingOtherDay) PillButton("رجوع لليوم", onBack) else Num(clock(t), size = 26.sp, color = pal.ink)
            Box(
                Modifier.heightIn(min = 48.dp).clickable(onClick = openSettings).semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) { Txt("الإعدادات", size = 14.5.sp, color = pal.grey) }
        }
    }
}

@Composable
private fun TabBar(current: Tab, pal: Palette, onSelect: (Tab) -> Unit) {
    val idle = if (pal.isDark) TabIdleDark else TabIdleLight
    Row(
        Modifier.fillMaxWidth().background(pal.paper)
            .drawBehind { drawLine(pal.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(top = 6.dp, bottom = 6.dp),
    ) {
        for (tab in Tab.entries) {
            val on = tab == current
            Column(
                Modifier.weight(1f).heightIn(min = 56.dp).clickable { onSelect(tab) }
                    .semantics { role = Role.Tab; selected = on },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                YIcon(tab.icon, 26.dp, if (on) pal.ink else idle, if (on) accentOf(tab.icon, pal.isDark) else idle)
                Txt(tab.label, size = 11.5.sp, bold = on, color = if (on) pal.ink else idle, lineHeight = 1.3f)
            }
        }
    }
}
