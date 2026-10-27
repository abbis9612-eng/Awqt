package com.dcc.yawm.shared.day

/*
 * Night balance (docs/METHOD.md section 4), a line-by-line port of night_plan() in
 * docs/reference/resolver_reference.py. All times are wall-clock minutes from the day's local
 * midnight; values of 1440 and more belong to the next day.
 */

const val NEED_SLEEP = 420            // 7 h
const val ISHA_TO_BED_MIN = 30        // Isha prayer + witr/adhkar before sleeping
const val WAKE_BEFORE_FAJR_QIYAM = 20
const val WAKE_BEFORE_FAJR_PLAIN = 10
const val MIN_NIGHT_FOR_QIYAM = 360   // below 6 h of night sleep: witr before sleep, no waking before Fajr
const val SHORT_NAP = 20
const val LONG_NAP = 90
const val DEFAULT_BEDTIME = 21 * 60

data class NightPlan(
    val bedtime: Int,
    val wake: Int,
    /** Waking before Fajr for night prayer and witr. */
    val qiyam: Boolean,
    val witrBeforeSleep: Boolean,
    val nightSleep: Int,
    val nap: Int,
    val total: Int,
    /** Above 0 means 7 h cannot be reached; the UI says so honestly. */
    val shortfall: Int,
)

/** [isha] in today's minutes; [nextFajr] in minutes from today's midnight (so about 1440 and more). */
fun nightPlan(isha: Int, nextFajr: Int, userBedtime: Int = DEFAULT_BEDTIME, qiyamEnabled: Boolean = true): NightPlan {
    val bedtime = maxOf(userBedtime, isha + ISHA_TO_BED_MIN)
    val qiyam = qiyamEnabled && (nextFajr - WAKE_BEFORE_FAJR_QIYAM) - bedtime >= MIN_NIGHT_FOR_QIYAM
    val wake = nextFajr - if (qiyam) WAKE_BEFORE_FAJR_QIYAM else WAKE_BEFORE_FAJR_PLAIN
    val night = maxOf(0, wake - bedtime)
    val deficit = NEED_SLEEP - night
    val nap = if (deficit <= 30) SHORT_NAP else LONG_NAP
    val total = night + nap
    return NightPlan(bedtime, wake, qiyam, !qiyam, night, nap, total, maxOf(0, NEED_SLEEP - total))
}
