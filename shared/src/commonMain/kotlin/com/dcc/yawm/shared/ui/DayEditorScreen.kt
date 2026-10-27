package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.data.cleanTitle
import com.dcc.yawm.shared.day.Anchor
import com.dcc.yawm.shared.day.BlockMinutesRange
import com.dcc.yawm.shared.day.CustomBlock
import com.dcc.yawm.shared.day.DayCustomization
import com.dcc.yawm.shared.day.DayType
import com.dcc.yawm.shared.day.EditorBlock
import com.dcc.yawm.shared.day.FixedAppointment
import com.dcc.yawm.shared.day.Importance
import com.dcc.yawm.shared.day.editorView
import com.dcc.yawm.shared.day.moved
import com.dcc.yawm.shared.day.nextAppointmentId
import com.dcc.yawm.shared.day.reset
import com.dcc.yawm.shared.day.toggledWorkday
import com.dcc.yawm.shared.day.withAppointment
import com.dcc.yawm.shared.day.withChange
import com.dcc.yawm.shared.day.withOwnBlock
import com.dcc.yawm.shared.day.withoutAppointment
import com.dcc.yawm.shared.day.withoutOwnBlock
import com.dcc.yawm.shared.screen.DayNames
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.iconOf
import kotlinx.datetime.DayOfWeek

private fun segmentName(a: Anchor): String = when (a) {
    Anchor.FAJR -> "من الفجر للشروق"
    Anchor.SUNRISE -> "من الشروق للظهر"
    Anchor.DHUHR -> "من الظهر للعصر"
    Anchor.ASR -> "من العصر للمغرب"
    Anchor.MAGHRIB -> "من المغرب للعشاء"
    Anchor.ISHA, Anchor.BEDTIME -> "من العشاء للنوم"
}

private fun typeName(t: DayType): String = when (t) {
    DayType.WORKDAY -> "يوم الدوام"
    DayType.OFFDAY -> "يوم العطلة"
    DayType.FRIDAY -> "الجمعة"
}

private fun importanceName(i: Importance): String = when (i) {
    Importance.MUST -> "مهمة جداً"
    Importance.NORMAL -> "عادية"
    Importance.IF_TIME -> "إذا بقى وقت"
}

/** Saturday first, as the week screen. */
private val WeekOrder = listOf(
    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
)

@Composable
fun DayEditorScreen(settings: UserSettings, save: (UserSettings) -> Unit, close: () -> Unit) {
    val pal = LocalPalette.current
    val c = settings.day
    fun put(n: DayCustomization) = save(settings.copy(day = n))
    var type by remember { mutableStateOf(DayType.WORKDAY) }
    var open by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf<Anchor?>(null) }

    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Ruqaa("رتّب يومك", 34.sp)
        PillButton("رجوع", close)
    }
    Txt("الصلوات ثابتة بوقتها. الباقي ينضبط وحده ويّا الشمس كل يوم حسب ترتيبك.", size = 15.sp, color = pal.grey)

    Heading("أيام الدوام")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (d in WeekOrder) {
            val on = d == DayOfWeek.FRIDAY || d in c.workdays
            Chip(DayNames[d.ordinal].take(if (d == DayOfWeek.FRIDAY) 5 else 3), on && d != DayOfWeek.FRIDAY, Modifier.weight(1f), enabled = d != DayOfWeek.FRIDAY) {
                put(c.toggledWorkday(d))
            }
        }
    }
    Txt("الأيام المعلّمة دوام، والباقي عطلة. الجمعة إلها جدولها.", size = 13.5.sp, color = pal.grey, modifier = Modifier.padding(top = 4.dp))

    Heading("أي يوم ترتّب؟")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (t in listOf(DayType.WORKDAY, DayType.OFFDAY, DayType.FRIDAY)) {
            Chip(typeName(t), type == t, Modifier.weight(1f)) { type = t; open = null; adding = null }
        }
    }

    for ((seg, blocks) in c.editorView(type)) {
        Heading(segmentName(seg.from))
        blocks.forEachIndexed { i, b ->
            BlockRow(b) { open = if (open == b.spec.key) null else b.spec.key; adding = null }
            if (open == b.spec.key) {
                BlockEditor(
                    b = b,
                    canUp = !b.spec.isPrayer && i > 0 && !blocks[i - 1].spec.isPrayer,
                    canDown = !b.spec.isPrayer && i < blocks.lastIndex,
                    onTitle = { t -> put(c.withChange(type, b.spec.key) { it.copy(title = cleanTitle(t).ifEmpty { null }) }) },
                    onMinutes = { m -> put(c.withChange(type, b.spec.key) { it.copy(minutes = m) }) },
                    onHidden = { h -> put(c.withChange(type, b.spec.key) { it.copy(hidden = h) }) },
                    onMove = { d -> put(c.moved(type, seg.from, b.spec.key, d)) },
                    onDelete = if (b.isOwn) ({ put(c.withoutOwnBlock(type, b.spec.key)); open = null }) else null,
                )
            }
        }
        if (adding == seg.from) {
            NewBlockForm(
                onAdd = { title, minutes, importance ->
                    put(c.withOwnBlock(type, CustomBlock(c.nextCustomKey(), seg.from, title, minutes, importance)))
                    adding = null
                },
                onCancel = { adding = null },
            )
        } else {
            ListRow(onClick = { adding = seg.from; open = null }, divider = false, minHeight = 48.dp) {
                RowText("+ أضف فقرة هنا", bold = true)
            }
        }
    }
    ListRow(onClick = { put(c.reset(type)); open = null }, divider = false) {
        RowText("رجّع ${typeName(type)} مثل ما كان", color = pal.grey)
    }

    Heading("مواعيد بساعة ثابتة")
    Txt("مثل الدوام أو درس. اليوم يترتّب حواليها، والصلاة اللي تجي بنصها تبقى بوقتها.", size = 14.5.sp, color = pal.grey)
    for (a in c.appointments.sortedBy { it.start }) {
        AppointmentEditor(a, onChange = { put(c.withAppointment(it)) }, onDelete = { put(c.withoutAppointment(a.id)) })
    }
    ListRow(onClick = {
        put(c.withAppointment(FixedAppointment(c.nextAppointmentId(), "الدوام", 8 * 60, 14 * 60, c.workdays)))
    }, divider = false, minHeight = 48.dp) {
        RowText("+ أضف موعد", bold = true)
    }
    Box(Modifier.padding(bottom = 24.dp))
}

@Composable
private fun BlockRow(b: EditorBlock, onClick: () -> Unit) {
    val pal = LocalPalette.current
    val minutes = when {
        b.spec.isPrayer -> "صلاة · ${b.spec.preferredMinutes} د"
        b.spec.flex -> "ياخذ الباقي"
        b.spec.key == "nap" -> "حسب نومك"
        else -> "${b.spec.preferredMinutes} د"
    }
    ListRow(onClick = onClick) {
        Tile(iconOf(b.spec.key, b.spec.title), b.spec.isPrayer, TileSize.SM, done = b.hidden)
        RowText(b.spec.title, if (b.hidden) "مخفية" else minutes, color = if (b.hidden) pal.grey else pal.ink)
    }
}

@Composable
private fun BlockEditor(
    b: EditorBlock,
    canUp: Boolean,
    canDown: Boolean,
    onTitle: (String) -> Unit,
    onMinutes: (Int) -> Unit,
    onHidden: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val pal = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp)) {
        var title by remember(b.spec.key) { mutableStateOf(b.spec.title) }
        Field(title, "اسم الفقرة") { title = it }
        if (title != b.spec.title) PillButton("احفظ الاسم") { onTitle(title) }
        if (!b.spec.flex && b.spec.key != "nap") {
            ListRow(divider = false) {
                RowText("المدة المفضلة", "إذا الوقت قليل تتقلص وحدها")
                val m = b.spec.preferredMinutes
                val low = if (b.spec.isPrayer) 10 else BlockMinutesRange.first
                Stepper("$m د", "أقصر 5 دقائق", "أطول 5 دقائق",
                    onMinus = { onMinutes((m - 5).coerceAtLeast(low)) },
                    onPlus = { onMinutes((m + 5).coerceAtMost(BlockMinutesRange.last)) })
            }
        }
        if (!b.spec.isPrayer) {
            ListRow(divider = false) {
                RowText("تظهر بيومي")
                InkSwitch(!b.hidden, "تظهر بيومي") { onHidden(!it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canUp) PillButton("قدّمها") { onMove(-1) }
                if (canDown) PillButton("أخّرها") { onMove(1) }
                if (onDelete != null) PillButton("احذفها", onDelete)
            }
        } else {
            Txt("الصلاة ما تنحذف ولا تتحرك من وقتها.", size = 13.5.sp, color = pal.grey)
        }
    }
}

@Composable
private fun NewBlockForm(onAdd: (String, Int, Importance) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf(30) }
    var importance by remember { mutableStateOf(Importance.NORMAL) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Field(title, "شنو الفقرة؟ مثلاً: دراسة") { title = it }
        ListRow(divider = false) {
            RowText("المدة")
            Stepper("$minutes د", "أقصر 5 دقائق", "أطول 5 دقائق",
                onMinus = { minutes = (minutes - 5).coerceAtLeast(BlockMinutesRange.first) },
                onPlus = { minutes = (minutes + 5).coerceAtMost(BlockMinutesRange.last) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in Importance.entries) Chip(importanceName(i), importance == i, Modifier.weight(1f)) { importance = i }
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val t = cleanTitle(title)
            if (t.isNotEmpty()) PillButton("أضفها") { onAdd(t, minutes, importance) }
            PillButton("إلغاء", onCancel)
        }
    }
}

@Composable
private fun AppointmentEditor(a: FixedAppointment, onChange: (FixedAppointment) -> Unit, onDelete: () -> Unit) {
    val pal = LocalPalette.current
    var openEdit by remember(a.id) { mutableStateOf(false) }
    ListRow(onClick = { openEdit = !openEdit }) {
        Tile("case", false, TileSize.SM)
        RowText(a.title, "${clock(a.start)} إلى ${clock(a.end)} · " + WeekOrder.filter { it in a.days }.joinToString("، ") { DayNames[it.ordinal] })
    }
    if (!openEdit) return
    Column(Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp)) {
        var title by remember(a.id) { mutableStateOf(a.title) }
        Field(title, "اسم الموعد") { title = it }
        if (cleanTitle(title).isNotEmpty() && title != a.title) PillButton("احفظ الاسم") { onChange(a.copy(title = cleanTitle(title))) }
        ListRow(divider = false) {
            RowText("يبدي")
            Stepper(clock(a.start), "أبكر ربع ساعة", "أتأخر ربع ساعة",
                onMinus = { onChange(a.copy(start = (a.start - 15).coerceAtLeast(0))) },
                onPlus = { onChange(a.copy(start = (a.start + 15).coerceAtMost(a.end - 15))) })
        }
        ListRow(divider = false) {
            RowText("يخلص")
            Stepper(clock(a.end), "أبكر ربع ساعة", "أتأخر ربع ساعة",
                onMinus = { onChange(a.copy(end = (a.end - 15).coerceAtLeast(a.start + 15))) },
                onPlus = { onChange(a.copy(end = (a.end + 15).coerceAtMost(24 * 60))) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (d in WeekOrder) {
                Chip(DayNames[d.ordinal].take(3), d in a.days, Modifier.weight(1f)) {
                    onChange(a.copy(days = if (d in a.days) a.days - d else a.days + d))
                }
            }
        }
        Row(Modifier.padding(top = 10.dp)) { PillButton("احذف الموعد", onDelete) }
        if (a.days.isEmpty()) Txt("اختار يوم واحد على الأقل حتى يظهر.", size = 13.5.sp, color = pal.grey)
    }
}

/** A choice pill: ink when chosen, outlined otherwise. */
@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val pal = LocalPalette.current
    Box(
        modifier.heightIn(min = 48.dp)
            .background(if (on) pal.ink else pal.paper, RoundedCornerShape(999.dp))
            .border(1.5.dp, if (enabled) pal.ink else pal.line, RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Checkbox; selected = on; contentDescription = text }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(text, size = 14.sp, bold = on, color = if (on) pal.paper else if (enabled) pal.ink else pal.grey, align = TextAlign.Center, lineHeight = 1.3f) }
}

/** A one-line text field in the identity's style. */
@Composable
fun Field(value: String, hint: String, onChange: (String) -> Unit) {
    val pal = LocalPalette.current
    Box(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).heightIn(min = 48.dp)
            .border(1.5.dp, pal.line, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Txt(hint, size = 16.sp, color = pal.grey)
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(60)) },
            singleLine = true,
            textStyle = TextStyle(color = pal.ink, fontSize = 16.sp, fontFamily = LocalFonts.current.naskh),
            cursorBrush = SolidColor(pal.ink),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint },
        )
    }
}
