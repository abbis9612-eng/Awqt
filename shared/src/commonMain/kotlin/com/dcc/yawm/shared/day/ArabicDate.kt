package com.dcc.yawm.shared.day

import com.dcc.yawm.shared.SimpleDate

/** Iraqi month names, as used across the app. Western digits everywhere (docs/DECISIONS.md). */
private val MONTHS = listOf(
    "كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران",
    "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول",
)

/** "27 تشرين الأول 2026" */
fun arabicDate(date: SimpleDate): String {
    require(date.month in 1..12) { "month out of range: ${date.month}" }
    return "${date.day} ${MONTHS[date.month - 1]} ${date.year}"
}
