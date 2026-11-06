package com.dcc.yawm.shared.screen

import com.dcc.yawm.shared.alarm.UserAlarm
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.PartSpan
import com.dcc.yawm.shared.day.Remind
import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.day.parseThing
import com.dcc.yawm.shared.day.partAt

/*
 * «المساعد» (owner, 2026-11-06; preview docs/design/yawm-assistant.html): one sentence in, typed or
 * spoken, and one answer out. It knows only «يوم»'s own things: add a thing, set an alarm, move a time,
 * delay or bring forward, delete, and say what is there. Nothing happens until the user says yes; what it
 * does not understand it says plainly, with an example. It runs on the phone, with no network.
 */

sealed interface Ask {
    data class AddThing(val draft: AddDraft) : Ask
    data class AddAlarm(val draft: AddDraft) : Ask
    data class MoveThing(val thing: Thing, val to: Int) : Ask
    data class MoveAlarm(val alarm: UserAlarm, val to: Int) : Ask
    data class DeleteThing(val thing: Thing) : Ask
    data class DeleteAlarm(val alarm: UserAlarm) : Ask
    /** An answer or a question back; nothing to confirm. */
    data class Tell(val text: String) : Ask
}

/** The card the assistant shows before doing anything. */
data class AskCard(val head: String, val facts: List<Pair<String, String>>, val yes: String, val done: String)

const val AssistantScope = "آني أفهم بس شغلات يومك: أضيف شغلة، وأضبط منبّه، وأغيّر وكت، وأمسح. جرّب: «ذكّرني أشتري خبز باچر الصبح»."

// Tashkeel, shadda and tatweel go; Eastern digits become Western. Then a 1:1 «loose» copy for matching,
// so a match's range is also the range in the text the title is cut from.
private val easternDigits = "٠١٢٣٤٥٦٧٨٩"

private fun base(raw: String): String = buildString {
    for (c in raw) {
        val d = easternDigits.indexOf(c)
        when {
            d >= 0 -> append('0' + d)
            c == 'ـ' || c in 'ً'..'ْ' -> {}
            c == '٫' -> append(':')
            c == '؟' || c == '?' || c == '!' || c == '،' || c == ',' -> append(' ')
            else -> append(c)
        }
    }
}

private fun loose(s: String): String = s.map {
    when (it) { 'أ', 'إ', 'آ' -> 'ا'; 'ة' -> 'ه'; 'ى' -> 'ي'; 'گ' -> 'ك'; 'چ' -> 'ج'; else -> it }
}.joinToString("")

/** Spoken numbers 1..12 in Iraqi Arabic (loose spelling). */
private val numberWords: Map<String, Int> = mapOf(
    "واحد" to 1, "واحده" to 1, "وحده" to 1,
    "ثنين" to 2, "اثنين" to 2, "ثنتين" to 2, "اثنتين" to 2,
    "ثلاث" to 3, "ثلاثه" to 3, "تلاث" to 3, "تلاثه" to 3,
    "اربع" to 4, "اربعه" to 4,
    "خمس" to 5, "خمسه" to 5,
    "ست" to 6, "سته" to 6,
    "سبع" to 7, "سبعه" to 7,
    "ثمان" to 8, "ثمانيه" to 8, "ثمنيه" to 8, "ثمن" to 8,
    "تسع" to 9, "تسعه" to 9,
    "عشر" to 10, "عشره" to 10,
    "احدعش" to 11, "حدعش" to 11, "دعش" to 11, "احدعشر" to 11,
    "ثنعش" to 12, "اثنعش" to 12, "ثنعشر" to 12, "اثنعشر" to 12,
)
private val numAlt = numberWords.keys.sortedByDescending { it.length }.joinToString("|")

private val amWords = Regex("\\s(الصبح|صباحا|الصباح|الفجر|بالفجر|صحيني|كعدني|قعدني)\\s")
private val pmWords = Regex("\\s(العصر|بالعصر|المغرب|بالمغرب|بالليل|الليل|المسا|بالمسا|العشا|العشاء|الظهر|بالظهر)\\s")
private val noonWords = Regex("\\s(الظهر|بالظهر)\\s")

private data class TimeHit(val minute: Int, val range: IntRange, val relative: Boolean = false)

/**
 * A time in the sentence. «05:45» as written (with an evening word, the evening); «خمسة ونص»,
 * «سبعة الا ربع», «الساعة 9 بالليل»; «بعد ساعة» from now. A bare spoken hour counts only when
 * [bareOk] (an alarm or a change of time), so «قراءة 10 صفحات» stays a title.
 */
private fun timeIn(l: String, now: Int, bareOk: Boolean, wake: Boolean): TimeHit? {
    val am = amWords.containsMatchIn(l) || wake
    val pm = pmWords.containsMatchIn(l)
    fun dayHalf(h0: Int, mi: Int, written: Boolean): Int {
        var h = h0
        if (h > 12 || written && !pm) return (h * 60 + mi) % 1440
        if (h == 12) h = if (am && !pm) 0 else 12
        else if (pm && !(noonWords.containsMatchIn(l) && h >= 11)) h += 12
        else if (!am) {
            // Neither word: the next time this hour comes.
            val a = h * 60 + mi
            if (a <= now && a + 720 > now) h += 12
        }
        return (h * 60 + mi) % 1440
    }
    Regex("\\s(?:(?:ب?الساعه|للساعه|ساعه)\\s*)?(\\d{1,2})[:.](\\d{2})(?=\\s)").find(l)?.let {
        val h = it.groupValues[1].toInt(); val m = it.groupValues[2].toInt()
        if (h < 24 && m < 60) return TimeHit(dayHalf(h, m, written = true), it.range)
    }
    Regex("\\sبعد\\s(ساعه|ساعتين|نص\\sساعه|ربع\\sساعه|(\\d{1,3})\\s(دقيقه|دقايق|دقائق|ساعات))(?=\\s)").find(l)?.let {
        val add = when (it.groupValues[1]) {
            "ساعه" -> 60; "ساعتين" -> 120; "نص ساعه" -> 30; "ربع ساعه" -> 15
            else -> it.groupValues[2].toInt() * if (it.groupValues[3] == "ساعات") 60 else 1
        }
        return TimeHit((now + add) % 1440, it.range, relative = true)
    }
    val re = Regex("\\s((?:ب?الساعه|للساعه|ساعه)\\s)?(\\d{1,2}|$numAlt)(\\s(ونص|وربع|وثلث|الا\\sربع|الا\\sثلث|الا\\sعشره|و(\\d{1,2})\\s?(دقيقه|دقايق|دقائق)))?(?=\\s)")
    for (m in re.findAll(l)) {
        val word = m.groupValues[2]
        val h = word.toIntOrNull() ?: numberWords[word] ?: continue
        if (h !in 1..12 && !(m.groupValues[1].isNotEmpty() && h in 0..23)) continue
        val hasClock = m.groupValues[1].isNotEmpty()
        val frac = m.groupValues[4]
        val after = l.substring(m.range.last + 1).trimStart()
        val dayWordNext = Regex("^(الصبح|صباحا|بالليل|الليل|العصر|بالعصر|المسا|بالمسا|الظهر|بالظهر|الفجر|بالفجر)(\\s|$)").containsMatchIn(after)
        if (!hasClock && frac.isEmpty() && !dayWordNext && !bareOk) continue
        var hh = h
        val mi = when {
            frac == "ونص" -> 30; frac == "وربع" -> 15; frac == "وثلث" -> 20
            frac.startsWith("الا") -> { hh -= 1; when { frac.endsWith("ربع") -> 45; frac.endsWith("ثلث") -> 40; else -> 50 } }
            m.groupValues[5].isNotEmpty() -> m.groupValues[5].toInt().coerceIn(0, 59)
            else -> 0
        }
        if (h > 12) return TimeHit((h * 60 + mi) % 1440, m.range)
        return TimeHit(dayHalf(if (hh == 0) 12 else hh, mi, written = false), m.range)
    }
    return null
}

private fun cut(s: String, r: IntRange): String = (s.substring(0, r.first) + " " + s.substring(r.last + 1)).replace(Regex("\\s+"), " ")

/** A word without «ال», «بال», «لل», «وال» in front, so «بالمحاسب» and «المحاسب» meet. */
private fun stem(w: String): String = w.replace(Regex("^(وبال|بال|وال|لل|ال)"), "").let { if (it.length >= 3) it else w }

/** Does the sentence name [name]? Every word of the name (three letters or more) is in it, loosely. */
private fun names(l: String, name: String): Boolean {
    val n = loose(base(name)).trim().removePrefix("منبه").trim()
    val words = n.split(' ').filter { it.length >= 3 }.map(::stem)
    if (words.isEmpty()) return false
    val said = l.split(' ').filter { it.isNotEmpty() }.map(::stem).toSet()
    return words.all { it in said }
}

private val deleteVerb = Regex("^\\s(شيل|احذف|امسح|الغي|الغ|بطل|طفي)\\s")
private val changeVerb = Regex("^\\s(خلي|غير|بدل|حول|اخر|قدم|رجع)(ها|ه|لي)?\\s")
private val delayVerb = Regex("^\\s(اخر|قدم|رجع)\\S*\\s")
private val addVerb = Regex("^\\s(ذكرني|نبهني|حطلي|حط|ضيف|ضيفلي|اضف|سجل|سجلي|اريد|ابي|لازم)(\\s(ب|ان|اني))?\\s")
private val alarmWords = Regex("\\s(منبه|صحيني|كعدني|قعدني|صحني)\\s")
private val question = Regex("^\\s(شنو|شلون|ليش|منو|كم|هل|شكد|اشكد|وين|اكو)\\s")

/**
 * What one sentence asks for. [things] and [alarms] are the user's, [spans] today's parts,
 * [nowMinute] the clock now (0..1439), [defaultPart] the part of now.
 */
fun understand(raw: String, things: List<Thing>, alarms: List<UserAlarm>, spans: List<PartSpan>, nowMinute: Int, defaultPart: DayPart): Ask {
    val b = " " + base(raw).trim().replace(Regex("\\s+"), " ") + " "
    val l = loose(b)
    if (l.isBlank()) return Ask.Tell("گول شنو تريد، مثلاً: «صحّيني باچر خمسة ونص».")

    if (Regex("^\\s(شنو|شكو)\\s(عندي|اكو)").containsMatchIn(l)) {
        val list = things.filter { !it.isLater }.map { x -> x.title + " · " + (x.at?.let { clock(it) } ?: x.part?.label ?: "اليوم") } +
            alarms.filter { it.on }.map { "منبّه ${it.label} · ${clock(it.minute)}" }
        return Ask.Tell(if (list.isEmpty()) "ما عندك شي بعد. گول «ضيف رياضة بعد العصر» وأضيفها." else "عندك: " + list.joinToString("، ") + ".")
    }
    if (question.containsMatchIn(l)) return Ask.Tell(AssistantScope)

    // Delete.
    if (deleteVerb.containsMatchIn(l)) {
        val a = alarms.firstOrNull { names(l, it.label) }
        val t = things.firstOrNull { names(l, it.title) }
        val wantsAlarm = alarmWords.containsMatchIn(l)
        return when {
            a != null && (wantsAlarm || t == null) -> Ask.DeleteAlarm(a)
            t != null -> Ask.DeleteThing(t)
            wantsAlarm && alarms.size == 1 -> Ask.DeleteAlarm(alarms.first())
            else -> Ask.Tell("ما لكيت هالشي بيومك. گول اسمه مثل ما هو، مثلاً: «شيل منبّه الدوام».")
        }
    }

    // Change a time, or delay / bring forward.
    if (changeVerb.containsMatchIn(l)) {
        val a = alarms.firstOrNull { names(l, it.label) }
        val t = things.firstOrNull { names(l, it.title) }
        val wantsAlarm = alarmWords.containsMatchIn(l)
        val alarm = if (a != null && (wantsAlarm || t == null)) a else null
        val thing = if (alarm == null) t else null
        if (alarm == null && thing == null) return Ask.Tell("شنو اللي تريد أغيّره؟ گول اسمه، مثلاً: «خلّي الرياضة الساعة ستة».")
        val cur = alarm?.minute ?: thing?.at
        val by = Regex("\\s(ساعه|ساعتين|نص\\sساعه|ربع\\sساعه|(\\d{1,3})\\s(دقيقه|دقايق|دقائق))(?=\\s)").find(l)
        val to: Int? = if (delayVerb.containsMatchIn(l) && by != null && cur != null) {
            val d = when (by.groupValues[1]) { "ساعه" -> 60; "ساعتين" -> 120; "نص ساعه" -> 30; "ربع ساعه" -> 15; else -> by.groupValues[2].toInt() }
            val later = Regex("^\\s(اخر|رجع)").containsMatchIn(l)
            ((cur + if (later) d else -d) % 1440 + 1440) % 1440
        } else timeIn(l, nowMinute, bareOk = true, wake = alarm != null && alarmWords.containsMatchIn(l))?.minute
        val name = alarm?.label ?: thing!!.title
        if (to == null) return Ask.Tell("لأي ساعة أغيّر «$name»؟ مثلاً: «خلّيها الساعة ستة ونص».")
        return if (alarm != null) Ask.MoveAlarm(alarm, to) else Ask.MoveThing(thing!!, to)
    }

    // Add a thing or an alarm.
    val alarm = alarmWords.containsMatchIn(l)
    val wake = Regex("\\s(صحيني|كعدني|قعدني|صحني)\\s").containsMatchIn(l)
    var text = b
    var lt = l
    addVerb.find(lt)?.let { text = cut(text, it.range); lt = loose(text) }
    val hit = timeIn(lt, nowMinute, bareOk = alarm, wake = wake)
    if (hit != null) { text = cut(text, hit.range); lt = loose(text) }
    // The alarm words and «ذكّرني»-like verbs in the middle are not part of the title.
    text = text.replace(Regex("\\s(صحّيني|صحيني|گعدني|كعدني|قعدني|صحني)\\s"), " ")
    val repeatWords = parseThing(text).repeat
    val taps = AddTaps(at = hit?.minute, repeat = if (alarm && repeatWords == null) Repeat.ONCE else null)
    val draft = addDraft(text.trim(), alarm, taps, defaultPart, spans, nowMinute)
    if (alarm) {
        if (hit == null) return Ask.Tell("بيا ساعة؟ مثلاً: «صحّيني باچر خمسة ونص» أو «منبّه الساعة 05:45 كل يوم».")
        return Ask.AddAlarm(draft)
    }
    if (draft.title.isBlank() || draft.title.length < 2) return Ask.Tell("شنو تريد أضيف؟ مثلاً: «ذكّرني أتصل بأمي بعد المغرب كل يوم».")
    return Ask.AddThing(draft)
}

/** What the card says for each kind of request. */
fun Ask.card(spans: List<PartSpan>): AskCard? = when (this) {
    is Ask.AddThing -> AskCard(
        "أضيف شغلة",
        buildList {
            add("الشغلة" to draft.title)
            add("وين" to if (draft.where == Where.Later) "بعدين، بدون وكت" else whereName(draft.where))
            draft.at?.let { add("الساعة" to clock(it)) }
            add("يتكرر" to draft.repeat.label)
            if (draft.where != Where.Later) add("يذكّرك" to when (draft.remind) { Remind.NONE -> "لا"; Remind.START -> "بأول القسم"; Remind.AT -> "بوكتها بالضبط" })
        },
        "إي، ضيفها", "انضافت ليومك.",
    )
    is Ask.AddAlarm -> AskCard(
        "منبّه جديد",
        listOf("يرن" to clock(draft.at ?: 0), "إمتى" to (if (draft.repeat != Repeat.ONCE) draft.repeat.label else if (draft.where == Where.Tomorrow || draft.passed) "باچر" else "اليوم"), "يتكرر" to draft.repeat.label) +
            (if (draft.title != "منبّه") listOf("اسمه" to draft.title) else emptyList()),
        "إي، اضبطه", "انضبط المنبّه ${clock(draft.at ?: 0)}.",
    )
    is Ask.MoveThing -> AskCard(
        "أغيّر وكت ${thing.title}",
        listOf("كان" to (thing.at?.let { clock(it) } ?: thing.part?.label ?: "بدون وكت"), "يصير" to (clock(to) + (partAt(to, spans)?.let { " · " + it.part.label } ?: ""))),
        "إي، غيّرها", "صارت ${clock(to)}.",
    )
    is Ask.MoveAlarm -> AskCard("أغيّر منبّه ${alarm.label}", listOf("كان" to clock(alarm.minute), "يصير" to clock(to)), "إي، غيّره", "صار المنبّه ${clock(to)}.")
    is Ask.DeleteThing -> AskCard("أمسح الشغلة", listOf("الشغلة" to thing.title, "وكتها" to (thing.at?.let { clock(it) } ?: thing.part?.label ?: "بعدين")), "إي، امسحها", "انمسحت.")
    is Ask.DeleteAlarm -> AskCard("أمسح المنبّه", listOf("المنبّه" to alarm.label, "الساعة" to clock(alarm.minute)), "إي، امسحه", "انمسح المنبّه.")
    is Ask.Tell -> null
}

/** A moved thing keeps its reminder, now at its own time. */
fun Thing.movedTo(minute: Int): Thing = copy(at = minute, part = null, end = end?.let { e -> at?.let { minute + (e - it) } }, remind = if (remind == Remind.NONE) Remind.NONE else Remind.AT)
