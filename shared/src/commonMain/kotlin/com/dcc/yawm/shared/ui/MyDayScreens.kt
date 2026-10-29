package com.dcc.yawm.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.alarm.DeviceStatus
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.data.cleanTitle
import com.dcc.yawm.shared.day.ItemAnchor
import com.dcc.yawm.shared.day.LifeKind
import com.dcc.yawm.shared.day.MyDayCatalog
import com.dcc.yawm.shared.day.MyDayPlan
import com.dcc.yawm.shared.day.MyItem
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.dateLine
import com.dcc.yawm.shared.screen.dayView
import com.dcc.yawm.shared.screen.durText
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

/*
 * The user builds their own day (owner's decision 2026-10-29): what they want every day, when
 * («بعد العصر» moves with Asr all year, or a fixed clock time), how long, and what gives way first.
 * The same pieces serve the first run and «عدّل يومك».
 */

/** Life and work hours. */
@Composable
fun LifeEditor(plan: MyDayPlan, onChange: (MyDayPlan) -> Unit) {
    for (k in LifeKind.entries) {
        OutlineBox(plan.life == k, Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = { onChange(plan.copy(life = k)) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Tile(when (k) { LifeKind.JOB -> "case"; LifeKind.STUDY -> "page"; LifeKind.HOME -> "house"; LifeKind.FREE -> "nib" }, false, TileSize.SM)
                Column(Modifier.weight(1f)) {
                    Txt(k.label, size = 16.5.sp, bold = plan.life == k, lineHeight = 1.5f)
                    Txt(k.hint, size = 13.5.sp, color = LocalPalette.current.grey, lineHeight = 1.5f)
                }
                Check(plan.life == k)
            }
        }
    }
    if (plan.life.workTitle != null) {
        Heading(plan.life.workTitle)
        ListRow {
            RowText("يبدي")
            Stepper(clock(plan.workStart), "أبكر نص ساعة", "أتأخر نص ساعة",
                onMinus = { onChange(plan.copy(workStart = (plan.workStart - 30).coerceAtLeast(4 * 60))) },
                onPlus = { onChange(plan.copy(workStart = (plan.workStart + 30).coerceAtMost(plan.workEnd - 30))) })
        }
        ListRow {
            RowText("يخلص")
            Stepper(clock(plan.workEnd), "أبكر نص ساعة", "أتأخر نص ساعة",
                onMinus = { onChange(plan.copy(workEnd = (plan.workEnd - 30).coerceAtLeast(plan.workStart + 30))) },
                onPlus = { onChange(plan.copy(workEnd = (plan.workEnd + 30).coerceAtMost(23 * 60))) })
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (d in SatFirst) Chip(DayShort[d.ordinal], d in plan.workDays, Modifier.weight(1f)) {
                onChange(plan.copy(workDays = if (d in plan.workDays) plan.workDays - d else plan.workDays + d))
            }
        }
    }
}

/** What the user wants every day, and for each one: when, how long, what gives way first. */
@Composable
fun WantsEditor(plan: MyDayPlan, onChange: (MyDayPlan) -> Unit) {
    val pal = LocalPalette.current
    val entries = MyDayCatalog.entries
    for (row in entries.chunked(2)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (e in row) {
                val on = plan.items.any { it.kind == e.kind }
                OutlineBox(on, Modifier.weight(1f), onClick = {
                    onChange(if (on) plan.copy(items = plan.items.filter { it.kind != e.kind }) else plan.copy(items = plan.items + MyDayCatalog.newItem(plan.nextId(), e)))
                }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Tile(e.icon, false, TileSize.XS)
                        Txt(e.title, size = 15.sp, bold = true, modifier = Modifier.weight(1f), lineHeight = 1.4f)
                        if (on) Txt("✓", size = 16.sp, bold = true)
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    var own by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { Field(own, "شي ثاني؟ مثلاً: درس خصوصي") { own = it } }
        PillButton("+ أضيفه") {
            val t = cleanTitle(own)
            if (t.isNotEmpty()) {
                onChange(plan.copy(items = plan.items + MyItem(plan.nextId(), "own", t, "nib", ItemAnchor.ASR, minutes = 30, important = false)))
                own = ""
            }
        }
    }
    if (plan.items.isEmpty()) {
        Txt("إذا ما اخترت شي، يومك يبقى الصلوات والنوم والدوام، والباقي وقت فاضي لمهامك.", size = 14.5.sp, color = pal.grey, modifier = Modifier.padding(top = 8.dp))
    } else Heading("رتّبها")
    for (it in plan.items) ItemCard(it, onChange = { n -> onChange(plan.copy(items = plan.items.map { x -> if (x.id == it.id) n else x })) }) {
        onChange(plan.copy(items = plan.items.filter { x -> x.id != it.id }))
    }
}

@Composable
private fun ItemCard(it: MyItem, onChange: (MyItem) -> Unit, onDelete: () -> Unit) {
    val pal = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .background(pal.paper, RoundedCornerShape(18.dp))
            .background(lerp(pal.paper, pal.ink, 0.03f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Tile(it.icon, false, TileSize.SM)
            Txt(it.title, size = 17.sp, bold = true, modifier = Modifier.weight(1f))
            PillButton("شيلها", onDelete)
        }
        Txt("شوكت؟", size = 13.5.sp, color = pal.grey, modifier = Modifier.padding(top = 10.dp))
        for (row in ItemAnchor.entries.chunked(3)) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (a in row) Chip(a.label, it.anchor == a, Modifier.weight(1f)) { onChange(it.copy(anchor = a)) }
            }
        }
        if (it.anchor == ItemAnchor.FIXED) {
            ListRow(divider = false) {
                RowText("الساعة")
                Stepper(clock(it.at), "أبكر ربع ساعة", "أتأخر ربع ساعة",
                    onMinus = { onChange(it.copy(at = (it.at - 15).coerceAtLeast(4 * 60))) },
                    onPlus = { onChange(it.copy(at = (it.at + 15).coerceAtMost(23 * 60))) })
            }
        }
        Txt("كم تاخذ؟", size = 13.5.sp, color = pal.grey, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (m in listOf(15, 30, 45, 60, 90)) Chip("$m د", it.minutes == m, Modifier.weight(1f)) { onChange(it.copy(minutes = m)) }
        }
        Txt("إذا ضاك الوقت", size = 13.5.sp, color = pal.grey, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
        Segmented(listOf("مهمة جداً", "عادية، تقصر أول"), if (it.important) 0 else 1) { i -> onChange(it.copy(important = i == 0)) }
    }
}

/** The same day today, in winter and in summer, so the user sees it move with the prayers. */
@Composable
fun DayPreview(settings: UserSettings, plan: MyDayPlan, today: LocalDate, save: (UserSettings) -> Unit) {
    val pal = LocalPalette.current
    var which by remember { mutableIntStateOf(0) }
    val date = when (which) { 0 -> today; 1 -> LocalDate(today.year, 1, 15); else -> LocalDate(today.year, 6, 21) }
    Segmented(listOf("اليوم", "بالشتا", "بالصيف"), which) { which = it }
    val view = remember(settings, plan, date) { dayView(settings.copy(myDay = plan), date) }
    Txt("${dateLine(date)}: الفجر ${clock(view.times.fajr)}، المغرب ${clock(view.times.maghrib)}.", size = 14.5.sp, color = pal.grey, modifier = Modifier.padding(top = 8.dp))
    if (view.changes.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp).background(lerp(pal.paper, pal.ink, 0.05f), RoundedCornerShape(14.dp)).padding(12.dp)) {
            Txt("شنو تغيّر هذا اليوم", size = 14.sp, bold = true)
            for (c in view.changes) Txt(c, size = 14.5.sp, lineHeight = 1.7f)
        }
    }
    for (it in view.items.filter { x -> !x.key.startsWith("free:") }) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(46.dp)) { Num(clock(it.start), size = 15.sp) }
            Tile(it.icon, it.isPrayer, TileSize.XS)
            Txt(it.title, size = 15.sp, bold = it.isPrayer, modifier = Modifier.weight(1f), lineHeight = 1.45f)
            Txt(durText(it.end - it.start), size = 12.5.sp, color = pal.grey)
        }
    }
    ListRow {
        RowText("ساعة نومك", "تصحى ${clock(view.plan.wake)}" + if (view.plan.qiyam) " لصلاة الليل" else "")
        Stepper(clock(settings.bedtime), "أبكر ربع ساعة", "أتأخر ربع ساعة",
            onMinus = { save(settings.copy(bedtime = (settings.bedtime - 15).coerceAtLeast(19 * 60 + 30))) },
            onPlus = { save(settings.copy(bedtime = (settings.bedtime + 15).coerceAtMost(23 * 60 + 30))) })
    }
}

/** «عدّل يومك». */
@Composable
fun MyDayEditorScreen(settings: UserSettings, today: LocalDate, save: (UserSettings) -> Unit, close: () -> Unit) {
    val plan = settings.myDay ?: MyDayPlan()
    fun put(p: MyDayPlan) = save(settings.copy(myDay = p))
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Display("يومك", 32.sp)
        PillButton("رجوع", close)
    }
    Txt("هنا تغيّر شنو يصير بيومك. التغيير يبين باليوم فوراً.", size = 15.sp, color = LocalPalette.current.grey)
    Heading("شنو تريد بيومك؟")
    WantsEditor(plan, ::put)
    Heading("شلون يومك؟")
    LifeEditor(plan, ::put)
    Heading("شلون يطلع")
    DayPreview(settings, plan, today, save)
    var sure by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        WideAction("week", if (sure) "اضغط مرة ثانية حتى ترجع اليوم الجاهز" else "رجّع اليوم الجاهز بداله", modifier = Modifier.fillMaxWidth()) {
            if (sure) { save(settings.copy(myDay = null)); close() } else sure = true
        }
    }
    Spacer(Modifier.height(24.dp))
}

/**
 * First run: six short steps with a progress line, back and skip. It ends with the user's own
 * day, seen today, in winter and in summer, or with the ready day if they skip.
 */
@Composable
fun FirstRunBuilder(
    settings: UserSettings,
    status: DeviceStatus,
    device: com.dcc.yawm.shared.alarm.DeviceControls,
    platform: Platform,
    today: LocalDate,
    save: (UserSettings) -> Unit,
) {
    val pal = LocalPalette.current
    var step by remember { mutableIntStateOf(0) }
    var plan by remember { mutableStateOf(settings.myDay ?: MyDayPlan()) }
    val n = 6
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until n) Box(Modifier.weight(1f).height(4.dp).background(if (i <= step) pal.ink else pal.faint, RoundedCornerShape(2.dp)))
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        if (step > 0) PillButton("رجوع") { step-- } else Spacer(Modifier.width(1.dp))
        if (step < n - 1) PillButton("تخطّى") { save(settings.copy(setupDone = true, myDay = null)) }
    }
    when (step) {
        0 -> {
            Display("هلا بيك بـ«يوم»", 30.sp)
            val lines = listOf("يرتّب يومك من الفجر للنوم", "ينبّهك بوكت كل شغلة", "يحسبلك نومك كل ليلة")
            val reduced = LocalReducedMotion.current
            var shown by remember { mutableIntStateOf(if (reduced) 3 else 0) }
            LaunchedEffect(Unit) { while (shown < 3) { delay(650); shown++ } }
            Column(Modifier.fillMaxWidth().padding(top = 16.dp).background(lerp(pal.paper, pal.ink, 0.03f), RoundedCornerShape(18.dp)).padding(16.dp)) {
                Txt("«يوم» يساعدك…", size = 15.sp, color = pal.grey)
                lines.forEachIndexed { i, l ->
                    Row(Modifier.heightIn(min = 46.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Check(i < shown)
                        Txt(l, size = 17.sp, color = if (i < shown) pal.grey else pal.ink)
                    }
                }
                AnimatedVisibility(shown >= 3, enter = fadeIn(tween(400))) {
                    Row(Modifier.heightIn(min = 46.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Check(false)
                        Txt("وهسه دورك", size = 17.sp, bold = true)
                    }
                }
            }
            InkButton("يلا نبني يومك", Modifier.padding(vertical = 22.dp)) { step = 1 }
        }
        1 -> {
            Display("شلون يومك؟", 30.sp)
            Txt("حتى نرتّب يومك من أول يوم. تكدر تغيّره بعدين.", size = 15.sp, color = pal.grey)
            LifeEditor(plan) { plan = it }
            InkButton("التالي", Modifier.padding(vertical = 22.dp)) { step = 2 }
        }
        2 -> {
            Display("شنو تريد بيومك؟", 30.sp)
            Txt("اختار اللي تريده يصير كل يوم، وقلّي شوكت وكم ياخذ.", size = 15.sp, color = pal.grey)
            WantsEditor(plan) { plan = it }
            InkButton("التالي", Modifier.padding(vertical = 22.dp)) { step = 3 }
        }
        3 -> {
            Display("وين تسكن؟", 30.sp)
            Txt("حتى نعرف أوقات الصلاة ونرتّب يومك عليها. موقعك يبقى بتلفونك وما يطلع لأي مكان.", size = 15.sp, color = pal.grey)
            CityChooser(settings, platform, save)
            InkButton("التالي", Modifier.padding(vertical = 22.dp)) { step = 4 }
        }
        4 -> {
            Display("آخر شي", 30.sp)
            Txt("حتى ينبّهك بوكت كل شغلة لازم تسمحله. هيچ راح يبين:", size = 15.sp, color = pal.grey)
            val view = remember(settings, plan) { dayView(settings.copy(myDay = plan), today) }
            NotificationMock(
                "يومك", clock(view.times.sunrise),
                "صباح الخير. أول شغلة: " + (view.items.firstOrNull { !it.isPrayer && !it.key.startsWith("free:") && it.start >= view.times.sunrise }?.title ?: "—") + ".",
            )
            ListRow {
                RowText("ملخص الصبح", "وية الشروق")
                InkSwitch(settings.alerts.morningSummary, "ملخص الصبح") { save(settings.copy(alerts = settings.alerts.copy(morningSummary = it))) }
            }
            ListRow {
                RowText("محاسبة الليل", "قبل النوم بنص ساعة")
                InkSwitch(settings.alerts.eveningReview, "محاسبة الليل") { save(settings.copy(alerts = settings.alerts.copy(eveningReview = it))) }
            }
            AlarmChecks(status, device)
            InkButton("التالي", Modifier.padding(vertical = 22.dp)) { step = 5 }
        }
        else -> {
            Display("هذا يومك", 30.sp)
            Txt("رتّبته من أجوبتك. بدّل بين اليوم والشتا والصيف حتى تشوف شلون يمشي ويّا الصلاة.", size = 15.sp, color = pal.grey)
            DayPreview(settings, plan, today, save)
            InkButton("تمام، هذا يومي", Modifier.padding(vertical = 22.dp)) { save(settings.copy(setupDone = true, myDay = plan)) }
        }
    }
}

/** What a notification looks like on the phone. */
@Composable
fun NotificationMock(title: String, at: String, body: String, icon: String = "page", sound: String? = null, onPlay: (() -> Unit)? = null, actions: List<String> = emptyList(), silent: Boolean = false) {
    val pal = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).background(lerp(pal.paper, pal.ink, 0.05f), RoundedCornerShape(20.dp)).padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.background(pal.ink, RoundedCornerShape(999.dp)).padding(horizontal = 6.dp)) { Txt("ي", size = 12.sp, bold = true, color = pal.paper, lineHeight = 1.3f) }
            Txt("يوم · $at", size = 12.5.sp, color = pal.grey, modifier = Modifier.weight(1f))
            if (silent) Txt("بلا صوت", size = 11.5.sp, color = pal.grey)
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(icon, false, TileSize.SM)
            Column(Modifier.weight(1f)) {
                Txt(title, size = 15.5.sp, bold = true, lineHeight = 1.5f)
                Txt(body, size = 14.sp, color = pal.grey, lineHeight = 1.6f)
            }
        }
        if (actions.isNotEmpty()) {
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (a in actions) Box(Modifier.background(lerp(pal.paper, pal.ink, 0.08f), RoundedCornerShape(999.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) { Txt(a, size = 14.sp, bold = true) }
            }
        }
        if (sound != null && onPlay != null) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt("الصوت: $sound", size = 13.sp, color = pal.grey, modifier = Modifier.weight(1f))
                PillButton("▶ اسمع", onPlay)
            }
        }
    }
}
