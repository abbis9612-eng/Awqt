package com.dcc.yawm.shared.screen

import kotlinx.datetime.LocalDate

// Times and durations in words, as in the prototype (Western digits, DECISIONS.md).

private fun pad(n: Int) = n.toString().padStart(2, '0')

/** "04:22" from minutes; wraps past midnight. */
fun clock(minutes: Int): String {
    val m = minutes.mod(1440)
    return "${pad(m / 60)}:${pad(m % 60)}"
}

/** "7:13" for a length of time. */
fun hm(minutes: Int): String = "${minutes / 60}:${pad(minutes % 60)}"

private fun hoursW(h: Int) = when {
    h == 1 -> "ساعة"
    h == 2 -> "ساعتين"
    h <= 10 -> "$h ساعات"
    else -> "$h ساعة"
}

private fun minsW(m: Int) = when {
    m == 1 -> "دقيقة"
    m == 2 -> "دقيقتين"
    m <= 10 -> "$m دقائق"
    else -> "$m دقيقة"
}

/** "ساعة و3 دقائق". */
fun durText(minutes: Int): String {
    val x = maxOf(0, minutes)
    val h = x / 60
    val m = x % 60
    return when {
        h == 0 -> minsW(m)
        m == 0 -> hoursW(h)
        else -> hoursW(h) + " و" + minsW(m)
    }
}

/** "12 س 30 د". */
fun durShort(minutes: Int): String {
    val x = maxOf(0, minutes)
    val h = x / 60
    val m = x % 60
    return when {
        h > 0 && m > 0 -> "$h س $m د"
        h > 0 -> "$h س"
        else -> "$m د"
    }
}

val MonthNames = listOf(
    "كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران",
    "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول",
)

/** Monday first, as kotlinx-datetime orders DayOfWeek. */
val DayNames = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")

fun dayName(date: LocalDate): String = DayNames[date.dayOfWeek.ordinal]

/** "الثلاثاء 27 تشرين الأول". */
fun dateLine(date: LocalDate): String = "${dayName(date)} ${date.day} ${MonthNames[date.month.ordinal]}"
