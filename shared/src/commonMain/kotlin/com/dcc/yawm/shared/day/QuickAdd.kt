package com.dcc.yawm.shared.day

/*
 * Quick add understands a few everyday words typed with the task, on the phone only (no network):
 * «اتصل بالمحاسب باچر نص ساعة» → «اتصل بالمحاسب», tomorrow, 30 minutes.
 */

enum class TaskWhen { TODAY, TOMORROW, LATER }

data class ParsedTask(val title: String, val whenDay: TaskWhen?, val minutes: Int?)

private val easternDigits = "٠١٢٣٤٥٦٧٨٩"

fun parseTask(raw: String): ParsedTask {
    var s = " " + raw.map { c -> easternDigits.indexOf(c).let { if (it >= 0) ('0' + it) else c } }.joinToString("") + " "
    var whenDay: TaskWhen? = null
    var minutes: Int? = null
    fun take(re: Regex, f: (MatchResult) -> Unit) {
        val m = re.find(s) ?: return
        f(m)
        s = s.replaceRange(m.range, " ")
    }
    take(Regex("\\s(باچر|باجر|بكرة|بكره|غداً|غدا)\\s")) { whenDay = TaskWhen.TOMORROW }
    take(Regex("\\s(اليوم)\\s")) { whenDay = TaskWhen.TODAY }
    take(Regex("\\s(بعدين)\\s")) { whenDay = TaskWhen.LATER }
    take(Regex("\\sساعة ونص\\s")) { minutes = 90 }
    take(Regex("\\sساعتين\\s")) { minutes = 120 }
    take(Regex("\\sنص ساعة\\s")) { minutes = 30 }
    take(Regex("\\sربع ساعة\\s")) { minutes = 15 }
    take(Regex("\\s(\\d{1,3})\\s*(د|دقيقة|دقايق|دقائق)\\s")) { minutes = it.groupValues[1].toInt() }
    take(Regex("\\s(\\d{1,2})\\s*(س|ساعة|ساعات)\\s")) { minutes = it.groupValues[1].toInt() * 60 }
    take(Regex("\\sساعة\\s")) { minutes = 60 }
    return ParsedTask(s.trim().replace(Regex("\\s+"), " "), whenDay, minutes?.takeIf { it in 5..600 })
}

/** What the user typed for a thing («يومك بين الصلوات»). */
data class ParsedThing(
    val title: String,
    val part: DayPart? = null,
    val whenDay: TaskWhen? = null,
    /** Minutes from midnight. */
    val at: Int? = null,
    val repeat: Repeat? = null,
    /** «منبّه» or «صحّيني» with the words: an alarm that rings, not a thing in a part. */
    val alarm: Boolean = false,
)

private val partWords = listOf(
    "بعد الفجر" to DayPart.FAJR, "بعد الظهر" to DayPart.DHUHR, "بعد العصر" to DayPart.ASR,
    "بعد المغرب" to DayPart.MAGHRIB, "بعد العشاء" to DayPart.ISHA,
    "الفجر" to DayPart.FAJR, "الصبح" to DayPart.MORNING, "الصباح" to DayPart.MORNING, "الضحى" to DayPart.MORNING,
    "الظهر" to DayPart.DHUHR, "العصر" to DayPart.ASR, "المغرب" to DayPart.MAGHRIB, "العشاء" to DayPart.ISHA,
    "بالليل" to DayPart.ISHA, "الليل" to DayPart.ISHA,
)

/**
 * «قراءة كتاب بعد العشاء كل يوم» → «قراءة كتاب», after Isha, daily.
 * «منبّه الساعة 05:45» → an alarm at 05:45. «الساعة 9 بالليل» → 21:00. Eastern digits work too.
 */
fun parseThing(raw: String): ParsedThing {
    var s = " " + raw.map { c -> easternDigits.indexOf(c).let { if (it >= 0) ('0' + it) else c } }.joinToString("").replace('٫', ':') + " "
    var whenDay: TaskWhen? = null
    var repeat: Repeat? = null
    var alarm = false
    var at: Int? = null
    var part: DayPart? = null
    fun take(re: Regex, f: (MatchResult) -> Unit) {
        val m = re.find(s) ?: return
        f(m)
        s = s.replaceRange(m.range, " ")
    }
    take(Regex("\\s(كل يوم|يومياً|يوميا)\\s")) { repeat = Repeat.DAILY }
    take(Regex("\\s(أيام الدوام|ايام الدوام)\\s")) { repeat = Repeat.WORKDAYS }
    take(Regex("\\s(منبّه|منبه|صحّيني|صحيني)\\s")) { alarm = true }
    take(Regex("\\s(باچر|باجر|بكرة|بكره|غداً|غدا)\\s")) { whenDay = TaskWhen.TOMORROW }
    take(Regex("\\s(اليوم)\\s")) { whenDay = TaskWhen.TODAY }
    take(Regex("\\s(بعدين)\\s")) { whenDay = TaskWhen.LATER }
    take(Regex("\\s(?:الساعة\\s*)?(\\d{1,2})[:.](\\d{2})\\s")) {
        val h = it.groupValues[1].toInt(); val m = it.groupValues[2].toInt()
        if (h < 24 && m < 60) at = h * 60 + m
    }
    if (at == null) take(Regex("\\sالساعة\\s*(\\d{1,2})\\s")) { val h = it.groupValues[1].toInt(); if (h < 24) at = h * 60 }
    for ((w, p) in partWords) {
        val re = Regex("\\s$w\\s")
        if (re.containsMatchIn(s)) {
            if (part == null) part = p
            s = s.replaceFirst(re, " ")
        }
    }
    // «الساعة 9 بالليل»: a 12-hour clock with a word of the evening means the afternoon or night.
    val t = at
    if (t != null && t < 12 * 60 && (part == DayPart.ISHA || part == DayPart.MAGHRIB || part == DayPart.ASR)) at = t + 12 * 60
    if (at != null && part == DayPart.MORNING) part = null
    val title = s.trim().replace(Regex("\\s+"), " ")
    return ParsedThing(if (alarm && title.isEmpty()) "منبّه" else title, if (at != null) null else part, whenDay, at, repeat, alarm)
}
