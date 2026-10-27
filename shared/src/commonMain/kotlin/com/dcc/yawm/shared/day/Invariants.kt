package com.dcc.yawm.shared.day

/*
 * The properties every resolved day must satisfy (CLAUDE.md, Invariants), ported from
 * check_invariants() in docs/reference/resolver_reference.py. An empty list means the day is valid.
 */
private val REQUIRED_PRAYER_KEYS = setOf("fajr", "dhuhr", "jumua", "asr", "maghrib", "isha", "witr")

fun checkInvariants(day: ResolvedDay): List<String> {
    val e = mutableListOf<String>()
    val bl = day.blocks
    for ((x, y) in bl.zipWithNext()) if (x.end > y.start) e += "overlap ${x.key}->${y.key}"
    for (b in bl) if (b.end <= b.start) e += "non-positive ${b.key}"
    val t = day.times
    val prayers = listOf(t.fajr, t.dhuhr, t.asr, t.maghrib, t.isha)
    for (b in bl) for (p in prayers) if (b.start < p && p < b.end) e += "crosses prayer ${b.key}"
    for ((key, at) in listOf("fajr" to t.fajr, "asr" to t.asr, "maghrib" to t.maghrib, "isha" to t.isha)) {
        val blk = bl.firstOrNull { it.key == key }
        if (blk == null || blk.start != at) e += "prayer block $key not at its time"
    }
    val dh = bl.firstOrNull { it.key == "dhuhr" || it.key == "jumua" }
    if (dh == null || dh.start != t.dhuhr) e += "dhuhr/jumua not at its time"
    for (key in listOf("child", "family")) {
        val blk = bl.firstOrNull { it.key == key }
        if (blk != null && blk.end - blk.start < 30) e += "$key < 30 min"
    }
    if (day.type != DayType.FRIDAY && bl.none { it.isDeepWork && it.end - it.start >= 45 }) e += "no deep block >= 45"
    val p = day.plan
    if (p.bedtime < t.isha + ISHA_TO_BED_MIN) e += "bed before isha+30"
    if (p.total < NEED_SLEEP && p.shortfall != NEED_SLEEP - p.total) e += "sleep shortfall not flagged"
    for (b in bl) if (b.isPrayer && b.end - b.start < 10) e += "prayer block ${b.key} < 10 min"
    val shrunkRequired = day.adjustments.any { it.kind == AdjustmentKind.SHRUNK && it.key in REQUIRED_PRAYER_KEYS }
    if (shrunkRequired && day.adjustments.none { it.kind == AdjustmentKind.DROPPED }) {
        e += "required block shrunk while optional blocks were still available"
    }
    if (day.adjustments.any { it.kind == AdjustmentKind.OVERFLOW }) e += "segment overflow"
    return e
}
