package com.dcc.yawm.shared.screen

import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.Role
import com.dcc.yawm.shared.prayer.DayPrayerTimes
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** One day of the week plan, from docs/METHOD.md section 5 as condensed in the prototype. */
data class WeekPlanDay(val focus: String, val morning: String, val afterAsr: String, val evening: String, val icons: List<String>)

/** Saturday first. */
val WeekPlan = listOf(
    WeekPlanDay("التخطيط", "مراجعة أسبوعية 45 د وكتلة عمق", "مشي أو راحة", "جلسة الشورى مع الزوجة", listOf("page", "steps", "house")),
    WeekPlanDay("العمل العميق", "كتلتان تركيز", "تدريب قوة", "الأسرة", listOf("nib", "dumbbell", "house")),
    WeekPlanDay("العمل", "كتلة: قرارات العمل، واجتماع الشركاء", "تعلم تقني", "الأسرة", listOf("case", "laptop", "house")),
    WeekPlanDay("العمل العميق", "كتلتان تركيز", "تدريب قوة", "تعلم شرعي", listOf("nib", "dumbbell", "book")),
    WeekPlanDay("الميدان", "كتلة، ثم زيارات", "مشي سريع 40 دقيقة", "تعلم تقني", listOf("case", "steps", "laptop")),
    WeekPlanDay("الإنجاز", "كتلة إنجاز ومراجعة، وإغلاق الأسبوع", "تدريب قوة", "خروج مع الزوجة", listOf("seal", "dumbbell", "house")),
    WeekPlanDay("الروح والأهل", "غُسل والتبكير للجمعة، وصلة الرحم", "بلا تلفون حتى المغرب", "فراغ مباح", listOf("mosque", "hands", "palm")),
)

/** The five steps of the Saturday review (docs/METHOD.md section 8). */
val ReviewSteps = listOf(
    "مجموع النقاط للأسبوع.",
    "ماذا نجح؟ وماذا فشل؟ وما السبب الحقيقي (نوم؟ تلفون؟ ضغط عمل؟).",
    "تفريغ كل المهام المعلقة.",
    "حجز خانات الأسبوع الجديد: كتل العمق، والاجتماعات، والتدريب، وخروج الزوجين، ونزهة الأبناء.",
    "تعديل واحد فقط على النظام، لا أكثر.",
)

/** Days after the last Saturday: Saturday 0 ... Friday 6. */
fun daysSinceSaturday(date: LocalDate): Int = (date.dayOfWeek.ordinal + 2) % 7

class WeekView(
    val saturday: LocalDate,
    val days: List<DayView>,
    /** Minutes in the week's plan. */
    val deepWork: Int,
    val family: Int,
    val movement: Int,
    val averageSleep: Int,
) {
    val friday: LocalDate get() = saturday.plus(6, DateTimeUnit.DAY)

    /** Ink of the word «الأسبوع»: days gone, plus how far today is between Fajr and bedtime. */
    fun progress(today: LocalDate, t: Int): Float {
        val ix = days.indexOfFirst { it.date == today }
        if (ix < 0) return if (today > friday) 1f else 0f
        val d = days[ix]
        val part = if (d.plan.bedtime <= d.times.fajr) 1f
        else ((t - d.times.fajr).toFloat() / (d.plan.bedtime - d.times.fajr)).coerceIn(0f, 1f)
        return (ix + part) / 7f
    }
}

fun weekView(settings: UserSettings, date: LocalDate): WeekView {
    val saturday = date.minus(daysSinceSaturday(date), DateTimeUnit.DAY)
    val cache = HashMap<LocalDate, DayPrayerTimes>()
    val days = (0..6).map { dayView(settings, saturday.plus(it, DateTimeUnit.DAY), cache) }
    fun sum(pick: (Item) -> Boolean) = days.sumOf { d -> d.items.filter { !it.isSleep && pick(it) }.sumOf { it.end - it.start } }
    return WeekView(
        saturday = saturday,
        days = days,
        deepWork = sum { it.role == Role.DEV },
        family = sum { it.role == Role.SPOUSE || it.role == Role.CHILD },
        movement = sum { it.key == "light" || it.key == "train" },
        averageSleep = days.sumOf { it.plan.total } / days.size,
    )
}

/** The coming Saturday (today if it is Saturday), when the weekly review is due. */
fun nextReview(date: LocalDate): LocalDate = date.plus((7 - daysSinceSaturday(date)) % 7, DateTimeUnit.DAY)
