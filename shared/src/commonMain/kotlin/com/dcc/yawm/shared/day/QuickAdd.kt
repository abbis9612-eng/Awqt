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
