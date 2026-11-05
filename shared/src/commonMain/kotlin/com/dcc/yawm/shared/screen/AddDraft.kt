package com.dcc.yawm.shared.screen

import com.dcc.yawm.shared.alarm.UserAlarm
import com.dcc.yawm.shared.alarm.newAlarm
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.PartSpan
import com.dcc.yawm.shared.day.Remind
import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.day.TaskWhen
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.day.parseThing
import com.dcc.yawm.shared.day.partAt
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/*
 * The add sheet of v11: what the words say, unless the user tapped another choice. «وين؟» is a
 * part, «باچر» or «بعدين»; an alarm has a time, a repeat and nothing else.
 */

sealed interface Where {
    data class In(val part: DayPart) : Where
    data object Tomorrow : Where
    data object Later : Where
}

fun whereName(w: Where): String = when (w) {
    is Where.In -> w.part.label
    Where.Tomorrow -> "باچر"
    Where.Later -> "بعدين"
}

val AllWheres: List<Where> = DayPart.entries.map { Where.In(it) } + Where.Tomorrow + Where.Later

/** What the user tapped in the sheet; null = follow the words. */
data class AddTaps(val where: Where? = null, val repeat: Repeat? = null, val remind: Remind? = null, val at: Int? = null)

data class AddDraft(
    val title: String,
    val alarm: Boolean,
    val where: Where,
    val at: Int?,
    val repeat: Repeat,
    val remind: Remind,
    /** For an alarm: today's time has passed, so a one-time alarm first rings tomorrow. */
    val passed: Boolean,
)

fun addDraft(raw: String, alarmMode: Boolean, taps: AddTaps, defaultPart: DayPart, spans: List<PartSpan>, nowMinute: Int): AddDraft {
    val p = parseThing(raw)
    val alarm = alarmMode || p.alarm
    val at = taps.at ?: p.at ?: if (alarm) 7 * 60 else null
    val repeat = taps.repeat ?: p.repeat ?: if (alarm) Repeat.DAILY else Repeat.ONCE
    val where = taps.where ?: when (p.whenDay) {
        TaskWhen.TOMORROW -> Where.Tomorrow
        TaskWhen.LATER -> Where.Later
        else -> p.part?.let { Where.In(it) } ?: at?.let { a -> partAt(a, spans)?.part?.let { Where.In(it) } } ?: Where.In(defaultPart)
    }
    val remind = taps.remind ?: if (at != null) Remind.AT else Remind.START
    val title = if (alarm) p.title.ifEmpty { "منبّه" } else p.title
    return AddDraft(title, alarm, where, at, if (where == Where.Later) Repeat.ONCE else repeat, if (where == Where.Later) Remind.NONE else remind, at != null && at <= nowMinute % 1440)
}

fun AddDraft.toThing(id: String, today: LocalDate): Thing = Thing(
    id = id, title = title,
    part = (where as? Where.In)?.part?.takeIf { at == null },
    date = when (where) { Where.Later -> null; Where.Tomorrow -> today.plus(1, DateTimeUnit.DAY); is Where.In -> today },
    at = at, repeat = repeat, remind = remind,
)

fun AddDraft.toAlarm(id: String, workDays: Set<DayOfWeek>, today: LocalDate, nowMinute: Int): UserAlarm =
    newAlarm(id, title, at ?: 7 * 60, repeat, workDays, today, nowMinute % 1440)

/** The short line after saving: «انحطّت: بعد العشاء · كل يوم», «المنبّه 05:45 · كل يوم · أول مرة باچر». */
fun AddDraft.savedMessage(): String =
    if (alarm) "المنبّه ${clock(at ?: 0)} · ${repeat.label}" + if (passed && repeat == Repeat.ONCE) " · يرن باچر" else ""
    else "انحطّت: ${whereName(where)}" + (at?.let { " · ${clock(it)}" } ?: "") + if (repeat != Repeat.ONCE) " · ${repeat.label}" else ""

/** What the alarm sheet says before saving. */
fun AddDraft.ringsLine(): String = when {
    !alarm -> ""
    repeat == Repeat.ONCE && passed -> "الساعة ${clock(at ?: 0)} فاتت اليوم، فيرن باچر الصبح مرة وحدة."
    repeat == Repeat.ONCE -> "يرن اليوم الساعة ${clock(at ?: 0)} مرة وحدة."
    passed -> "يرن ${repeat.label} الساعة ${clock(at ?: 0)}. أول مرة باچر."
    else -> "يرن ${repeat.label} الساعة ${clock(at ?: 0)}. أول مرة اليوم."
}

/*
 * «الألوان» sheets (approved 2026-11-05, docs/design/yawm-colors-add.html): one sentence says what the
 * words decided, and the button names what will happen.
 */

/** «بعد المغرب، مرة وحدة، ينبّهك بأول القسم.» */
fun AddDraft.saysLine(): String = when {
    alarm -> ringsLine()
    where == Where.Later -> "بعدين، بدون وكت. تلكاها تحت يومك."
    else -> whereName(where) + (at?.let { "، الساعة ${clock(it)}" } ?: "") + "، ${repeat.label}" + remindWords(remind) + "."
}

/** «حطّها بعد المغرب», «حطّها باچر», «احفظ المنبّه». */
fun AddDraft.saveLabel(): String = if (alarm) "احفظ المنبّه" else "حطّها ${whereName(where)}"

fun remindWords(r: Remind): String = when (r) {
    Remind.NONE -> ""
    Remind.START -> "، ينبّهك بأول القسم"
    Remind.AT -> "، ينبّهك بوكتها"
}

/** The small line of a thing's card: how often, until when, and its reminder. */
fun Thing.metaLine(): String =
    repeat.label + (end?.let { " · إلى ${clock(it)}" } ?: "") + when (remind) {
        Remind.NONE -> ""
        Remind.START -> " · ينبّهك بأول القسم"
        Remind.AT -> " · ينبّهك بوكتها"
    }
