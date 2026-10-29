package com.dcc.yawm.shared.day

import kotlinx.datetime.DayOfWeek

/*
 * The user's own day (owner's decision 2026-10-29, «1أ 2أ 3أ 4ج»): a few quick questions build it,
 * every item moves with a prayer by default («بعد العصر») or sits at a fixed clock time, and when
 * the time after a prayer is short, «عادية» items shorten first, then drop, and only then
 * «مهمة جداً» ones shorten. Prayers never move. The ready template of the method stays as the
 * other choice; with no plan the resolver is exactly the reference (CLAUDE.md rule 7).
 *
 * The fitting is the same tested code as the template: each prayer starts a segment holding the
 * prayer, the user's items for that prayer, and one flex block «وقت فاضي» that takes what is
 * left (tasks drop into it). Fixed items and work hours are appointments cut around prayers.
 */

/** When an item happens: right after a prayer (moves with it all year), or at a fixed time. */
enum class ItemAnchor(val label: String) {
    FAJR("بعد الفجر"), DHUHR("بعد الظهر"), ASR("بعد العصر"), MAGHRIB("بعد المغرب"), ISHA("بعد العشاء"), FIXED("بساعة ثابتة"),
}

enum class LifeKind(val label: String, val hint: String, val workTitle: String?) {
    JOB("عندي دوام", "وظيفة أو محل بساعات ثابتة", "الدوام"),
    STUDY("أدرس", "مدرسة أو جامعة أو دورة", "الدراسة"),
    HOME("أغلب يومي بالبيت", "البيت والأهل", null),
    FREE("شغلي حر", "أرتّب أوقاتي بنفسي", null),
}

data class MyItem(
    val id: String,
    /** A [MyDayCatalog] key, or "own" for something the user typed. */
    val kind: String,
    val title: String,
    val icon: String,
    val anchor: ItemAnchor,
    /** Minutes from midnight; used only when [anchor] is FIXED. */
    val at: Int = 17 * 60,
    val minutes: Int,
    /** «مهمة جداً»: shortens only after every «عادية» item of its segment is gone. */
    val important: Boolean,
)

data class MyDayPlan(
    val life: LifeKind = LifeKind.FREE,
    /** Work or study hours, minutes from midnight; used for JOB and STUDY. */
    val workStart: Int = 8 * 60,
    val workEnd: Int = 14 * 60,
    val workDays: Set<DayOfWeek> = DefaultWorkdays,
    val items: List<MyItem> = emptyList(),
) {
    val hasWork: Boolean get() = life.workTitle != null && workEnd > workStart

    fun appointmentsOn(day: DayOfWeek): List<FixedAppointment> = buildList {
        if (hasWork && day in workDays) add(FixedAppointment(WORK_ID, life.workTitle!!, workStart, workEnd, workDays, Role.BUSINESS))
        for (it in items) if (it.anchor == ItemAnchor.FIXED) {
            val end = minOf(it.at + it.minutes, 1440)
            if (end > it.at) add(FixedAppointment(it.id, it.title, it.at, end, DayOfWeek.entries.toSet(), roleOf(it.kind)))
        }
    }.sortedBy { it.start }

    fun nextId(): String = "m${(items.mapNotNull { it.id.removePrefix("m").toIntOrNull() }.maxOrNull() ?: 0) + 1}"
}

const val WORK_ID = "work"
const val FREE_TITLE = "وقت فاضي"

/** The common things people want in their day; each is only a starting point the user edits. */
object MyDayCatalog {
    data class Entry(val kind: String, val title: String, val word: String, val icon: String, val anchor: ItemAnchor, val minutes: Int, val important: Boolean)

    val entries = listOf(
        Entry("sport", "رياضة", "رياضة", "dumbbell", ItemAnchor.ASR, 45, false),
        Entry("walk", "مشي", "مشي", "steps", ItemAnchor.FAJR, 30, false),
        Entry("deep", "شغل يحتاج تركيز", "تركيز", "nib", ItemAnchor.FAJR, 90, true),
        Entry("study", "دراسة أو قراءة", "دراسة", "book", ItemAnchor.ISHA, 45, false),
        Entry("quran", "قرآن وأذكار", "الأذكار", "rahl", ItemAnchor.FAJR, 20, true),
        Entry("family", "وقت ويّا الأهل", "الأهل", "house", ItemAnchor.MAGHRIB, 60, true),
        Entry("kids", "وقت ويّا الأطفال", "الأطفال", "child", ItemAnchor.ASR, 45, true),
        Entry("nap", "قيلولة", "قيلولة", "nap", ItemAnchor.DHUHR, 20, false),
        Entry("learn", "تعلّم شي جديد", "تعلّم", "laptop", ItemAnchor.ISHA, 30, false),
    )

    fun entry(kind: String): Entry? = entries.firstOrNull { it.kind == kind }

    fun newItem(id: String, e: Entry) = MyItem(id, e.kind, e.title, e.icon, e.anchor, minutes = e.minutes, important = e.important)

    /** Icons the user can give an item of their own. */
    val ownIcons = listOf("nib", "case", "book", "laptop", "dumbbell", "steps", "house", "child", "tea", "phone")
}

fun roleOf(kind: String): Role = when (kind) {
    "sport", "walk", "nap" -> Role.BODY
    "deep" -> Role.DEV
    "study", "learn" -> Role.LEARNING
    "quran" -> Role.WORSHIP
    "family" -> Role.SPOUSE
    "kids" -> Role.CHILD
    else -> Role.SELF
}

/** Items shorten to half (in 5-minute steps), never under 10 minutes. */
fun shortestOf(minutes: Int): Int = minOf(minutes, maxOf(10, minutes / 2 / 5 * 5))

/** Key of an item's block in the resolved day. */
fun myKey(id: String) = "my:$id"

/** Prayer blocks of the user's own day: the prayer alone, without the template's extras. */
private fun prayerSpec(key: String, title: String, minutes: Int, min: Int) =
    BlockSpec(key, title, Role.WORSHIP, minutes, min, 1, droppable = false, isPrayer = true)

private fun freeSpec(anchor: Anchor) = BlockSpec("free:${anchor.name.lowercase()}", FREE_TITLE, Role.SELF, 0, 0, 9, flex = true)

/** The segments of the user's day for [type]; witr closes the evening when there is no qiyam. */
fun myDayTemplate(type: DayType, plan: MyDayPlan, witrBeforeSleep: Boolean): List<SegmentSpec> {
    fun items(a: ItemAnchor) = plan.items.filter { it.anchor == a && it.minutes > 0 }.map { it ->
        val m = it.minutes.coerceIn(BlockMinutesRange)
        BlockSpec(
            myKey(it.id), it.title, roleOf(it.kind), m, shortestOf(m),
            priority = if (it.important) 2 else 4, droppable = !it.important, isDeepWork = it.kind == "deep",
        )
    }
    val noon = if (type == DayType.FRIDAY) prayerSpec("jumua", "صلاة الجمعة", 50, 40) else prayerSpec("dhuhr", "صلاة الظهر", 20, 10)
    val witr = if (witrBeforeSleep) listOf(BlockSpec("witr", "الوتر وأذكار النوم", Role.WORSHIP, 15, 10, 1, droppable = false)) else emptyList()
    return listOf(
        SegmentSpec(Anchor.FAJR, Anchor.DHUHR, listOf(prayerSpec("fajr", "صلاة الفجر", 20, 10)) + items(ItemAnchor.FAJR) + freeSpec(Anchor.FAJR)),
        SegmentSpec(Anchor.DHUHR, Anchor.ASR, listOf(noon) + items(ItemAnchor.DHUHR) + freeSpec(Anchor.DHUHR)),
        SegmentSpec(Anchor.ASR, Anchor.MAGHRIB, listOf(prayerSpec("asr", "صلاة العصر", 15, 10)) + items(ItemAnchor.ASR) + freeSpec(Anchor.ASR)),
        SegmentSpec(Anchor.MAGHRIB, Anchor.ISHA, listOf(prayerSpec("maghrib", "صلاة المغرب", 15, 10)) + items(ItemAnchor.MAGHRIB) + freeSpec(Anchor.MAGHRIB)),
        SegmentSpec(Anchor.ISHA, Anchor.BEDTIME, listOf(prayerSpec("isha", "صلاة العشاء", 15, 10)) + items(ItemAnchor.ISHA) + freeSpec(Anchor.ISHA) + witr),
    )
}
