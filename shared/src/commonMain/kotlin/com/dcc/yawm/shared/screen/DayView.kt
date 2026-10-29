package com.dcc.yawm.shared.screen

import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.AdjustmentKind
import com.dcc.yawm.shared.day.DayMinutes
import com.dcc.yawm.shared.day.DayResolver
import com.dcc.yawm.shared.day.DayType
import com.dcc.yawm.shared.day.LifeKind
import com.dcc.yawm.shared.day.MyDayPlan
import com.dcc.yawm.shared.day.NightPlan
import com.dcc.yawm.shared.day.Role
import com.dcc.yawm.shared.day.customizedTemplate
import com.dcc.yawm.shared.day.Anchor
import com.dcc.yawm.shared.day.myDayTemplate
import com.dcc.yawm.shared.day.plainTitle
import com.dcc.yawm.shared.prayer.DayPrayerTimes
import com.dcc.yawm.shared.prayer.Prayer
import com.dcc.yawm.shared.prayer.PrayerTimesEngine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/*
 * What the screens show, computed from the engine and the resolver (never in the UI), as in the
 * approved prototype docs/design/prototype-src/ink. Times are wall-clock minutes from the app
 * day's midnight. The app day runs from Fajr to the next Fajr, so after midnight the screens
 * still show the night that is going on.
 */

/** The app day and the minute in it ([t] can pass 1440 between midnight and Fajr). */
data class AppMoment(val date: LocalDate, val t: Int)

fun zoneOf(settings: UserSettings): TimeZone =
    runCatching { TimeZone.of(settings.zoneId) }.getOrElse { TimeZone.of(UserSettings().zoneId) }

fun appMoment(now: Instant, settings: UserSettings): AppMoment {
    val zone = zoneOf(settings)
    val local = now.toLocalDateTime(zone)
    val t = local.hour * 60 + local.minute
    val fajr = PrayerTimesEngine.compute(settings.place, local.date, zone, settings.prayer).wallMinutes(Prayer.FAJR)
    return if (t < fajr) AppMoment(local.date.minus(1, DateTimeUnit.DAY), t + 1440) else AppMoment(local.date, t)
}

/** One line of the day: a resolved block, or the night's sleep and suhur. */
data class Item(
    val key: String,
    val title: String,
    val role: Role,
    val start: Int,
    val end: Int,
    val isPrayer: Boolean,
    val icon: String,
    val isSleep: Boolean = false,
    val shrunkFrom: Int? = null,
)

data class PrayerCell(val prayer: Prayer, val name: String, val at: Int, val icon: String, val fill: Float, val isNext: Boolean)

data class InkState(val word: String, val target: Int, val progress: Float)

class DayView(
    val date: LocalDate,
    val type: DayType,
    val times: DayMinutes,
    /** Yesterday's Isha, in this day's minutes (negative). */
    val prevIsha: Int,
    /** Tomorrow's Fajr, in this day's minutes (about 1440 and more). */
    val nextFajr: Int,
    val plan: NightPlan,
    val items: List<Item>,
    /** Why the day differs from the template today, one sentence per change. */
    val changes: List<String>,
) {
    fun current(t: Int): Item? = items.firstOrNull { it.start <= t && t < it.end }
    fun upcoming(t: Int, n: Int): List<Item> = items.filter { it.start > t }.take(n)

    private val prayerList: List<Triple<Prayer, String, Int>> get() = listOf(
        Triple(Prayer.FAJR, "الفجر", times.fajr),
        Triple(Prayer.DHUHR, if (type == DayType.FRIDAY) "الجمعة" else "الظهر", times.dhuhr),
        Triple(Prayer.ASR, "العصر", times.asr),
        Triple(Prayer.MAGHRIB, "المغرب", times.maghrib),
        Triple(Prayer.ISHA, "العشاء", times.isha),
    )

    /** The ink word: the next prayer, or the waking time while asleep. */
    fun ink(t: Int): InkState {
        val cur = current(t)
        if (cur != null && cur.isSleep) {
            return InkState(if (plan.qiyam) "صلاة الليل" else "الصحيان", plan.wake, fraction(t, plan.bedtime, plan.wake))
        }
        val all = listOf("العشاء" to prevIsha) + prayerList.map { it.second to it.third } + listOf("الفجر" to nextFajr)
        var i = all.indexOfFirst { it.second > t }
        if (i < 1) i = if (i == 0) 1 else all.lastIndex
        return InkState(all[i].first, all[i].second, fraction(t, all[i - 1].second, all[i].second))
    }

    /** The five prayers along the bottom; passed ones are full of ink. */
    fun prayers(t: Int): List<PrayerCell> {
        val next = ink(t).target
        return prayerList.map { (p, name, at) ->
            val isNext = at == next && current(t)?.isSleep != true
            val fill = when {
                at <= t -> 1f
                isNext -> ink(t).progress
                else -> 0f
            }
            PrayerCell(p, name, at, if (p == Prayer.DHUHR && type == DayType.FRIDAY) "mosque" else prayerIcon(p), fill, isNext)
        }
    }

    /** The night in order (prototype «ليلتك خطوة بخطوة»). */
    fun nightSteps(): List<Item> {
        val prep = items.firstOrNull { it.key == "prep" }
        val witr = items.firstOrNull { it.key == "witr" }
        return buildList {
            add(Item("isha", "صلاة العشاء", Role.WORSHIP, times.isha, times.isha, true, "isha"))
            if (prep != null) add(prep.copy())
            if (!plan.qiyam && witr != null) add(witr.copy())
            add(Item("sleep", "النوم، ${hm(plan.nightSleep)} ساعات", Role.BODY, plan.bedtime, plan.wake, false, "bed", isSleep = true))
            if (plan.qiyam) add(Item("qiyam2", QIYAM_TITLE, Role.WORSHIP, plan.wake, nextFajr, false, "lantern"))
            add(Item("fajr2", "صلاة الفجر", Role.WORSHIP, nextFajr, nextFajr, true, "fajr"))
        }
    }

    /** Before bedtime the night word is «النوم»; after it, the waking word. */
    fun nightInk(t: Int): InkState =
        if (t >= plan.bedtime) ink(t) else InkState("النوم", plan.bedtime, fraction(t, times.isha, plan.bedtime))
}

private const val QIYAM_TITLE = "صلاة الليل والوتر"

private fun fraction(t: Int, from: Int, to: Int): Float =
    if (to <= from) 1f else ((t - from).toFloat() / (to - from)).coerceIn(0f, 1f)

fun prayerIcon(p: Prayer): String = when (p) {
    Prayer.FAJR, Prayer.SUNRISE -> "fajr"
    Prayer.DHUHR -> "dhuhr"
    Prayer.ASR -> "asr"
    Prayer.MAGHRIB -> "maghrib"
    Prayer.ISHA -> "isha"
}

/** The training block changes with the weekday (prototype TRAIN, from METHOD.md section 5). */
fun trainingTitle(date: LocalDate): String? = when (date.dayOfWeek.ordinal) {
    5 -> "مشي أو راحة" // Saturday
    6 -> "تدريب قوة" // Sunday
    0 -> "تعلم تقني" // Monday
    1 -> "تدريب قوة" // Tuesday
    2 -> "مشي سريع 40 دقيقة" // Wednesday
    3 -> "تدريب قوة" // Thursday
    else -> null
}

/** Icon of each block (prototype iconOf). */
fun iconOf(key: String, title: String): String = when (key) {
    "fajr" -> "fajr"; "dhuhr" -> "dhuhr"; "asr" -> "asr"; "maghrib" -> "maghrib"; "isha" -> "isha"
    "jumua" -> "mosque"; "ghadwa" -> "rahl"; "light" -> "steps"; "deep1", "deep2" -> "nib"; "duha" -> "tea"
    "msgs1", "close" -> "mail"; "work1" -> "phone"; "work2" -> "case"; "nap" -> "nap"; "child" -> "child"
    "family" -> "house"; "self" -> "book"; "prep" -> "page"; "witr" -> "beads"; "sleep" -> "bed"
    "qiyam", "qiyam2" -> "lantern"; "jumua_prep" -> "ghusl"; "lunch" -> "meal"; "visit" -> "visit"
    "free" -> "palm"; "dua" -> "hands"; "self_am" -> "tea"
    "fixed" -> "case"
    "train" -> when {
        "مشي" in title -> "steps"
        "تعلم" in title -> "laptop"
        else -> "dumbbell"
    }
    else -> "page"
}

private val AfterPrayer = mapOf(
    Anchor.FAJR to "الفجر", Anchor.SUNRISE to "الشروق", Anchor.DHUHR to "الظهر", Anchor.ASR to "العصر",
    Anchor.MAGHRIB to "المغرب", Anchor.ISHA to "العشاء", Anchor.BEDTIME to "النوم",
)

/** An item's icon: the user's own choice for their items, the work icon for work hours. */
fun iconFor(key: String, title: String, my: MyDayPlan?): String {
    val id = key.removePrefix("fixed:").removePrefix("my:")
    my?.items?.firstOrNull { it.id == id }?.let { return it.icon }
    if (key == "fixed:work") return if (my?.life == LifeKind.STUDY) "page" else "case"
    return iconOf(key.substringBefore(':'), title)
}

/** Builds the day for [date]; [cache] avoids computing the same prayer times twice. */
fun dayView(settings: UserSettings, date: LocalDate, cache: MutableMap<LocalDate, DayPrayerTimes> = HashMap()): DayView {
    val zone = zoneOf(settings)
    fun times(d: LocalDate) = cache.getOrPut(d) { PrayerTimesEngine.compute(settings.place, d, zone, settings.prayer) }
    val yesterday = times(date.minus(1, DateTimeUnit.DAY))
    val today = times(date)
    val tomorrow = times(date.plus(1, DateTimeUnit.DAY))
    val my = settings.myDay
    val day = DayResolver.resolve(
        yesterday, today, tomorrow,
        userBedtime = settings.bedtime, qiyamEnabled = settings.qiyamEnabled, custom = settings.day, myDay = my,
    )
    val nextFajr = tomorrow.wallMinutes(Prayer.FAJR) + 1440
    // The weekday's training title, unless the user named the block themselves.
    val train = if (my != null || settings.day.changes[day.type]?.get("train")?.title != null) null else trainingTitle(date)

    val items = day.blocks.filter { it.key != "qiyam" }.map { b ->
        val title = if (b.key == "train" && train != null) train else plainTitle(b.key, b.title)
        Item(b.key, title, b.role, b.start, b.end, b.isPrayer, iconFor(b.key, title, my), shrunkFrom = b.shrunkFromMinutes)
    }.toMutableList()
    items += Item("sleep", "النوم", Role.BODY, day.plan.bedtime, day.plan.wake, false, "bed", isSleep = true)
    if (day.plan.qiyam) items += Item("qiyam2", QIYAM_TITLE, Role.WORSHIP, day.plan.wake, nextFajr, false, "lantern")

    val segments = if (my != null) myDayTemplate(day.type, my, day.plan.witrBeforeSleep) else customizedTemplate(day.type, day.plan.nap, settings.day)
    val specs = segments.flatMap { it.blocks }.associateBy { it.key }
    // In the user's own day each change says after which prayer it happened.
    val after = if (my == null) emptyMap() else segments.flatMap { seg -> seg.blocks.map { it.key to AfterPrayer.getValue(seg.from) } }.toMap()
    val changes = day.adjustments.mapNotNull { a ->
        val key = a.key ?: return@mapNotNull null
        if (key.startsWith("free:")) return@mapNotNull null
        val why = after[key]?.let { "لأن الوقت بعد $it قصير اليوم" } ?: "لأن الوقت بين الصلاتين قصير اليوم"
        when (a.kind) {
            AdjustmentKind.SHRUNK -> items.firstOrNull { it.key == key && it.shrunkFrom != null }?.let {
                "«${it.title}» صارت ${durText(it.end - it.start)} بدل ${durText(it.shrunkFrom!!)}، $why."
            }
            AdjustmentKind.DROPPED -> specs[key]?.let { spec ->
                val where = after[key]?.let { " بعد $it" }.orEmpty()
                val hint = if (my != null && !spec.droppable) " إذا تريدها، قصّر غيرها أو غيّر وقتها من «عدّل يومك»." else ""
                "ما بقى وقت اليوم لـ«${plainTitle(spec.key, spec.title)}»$where، فشلناها من الجدول.$hint"
            }
            AdjustmentKind.OVERFLOW -> null
        }
    }.distinct()

    return DayView(
        date, day.type, day.times, yesterday.wallMinutes(Prayer.ISHA) - 1440, nextFajr, day.plan,
        items.sortedBy { it.start }, changes,
    )
}
