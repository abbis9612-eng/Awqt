package com.dcc.yawm.shared

import com.dcc.yawm.shared.alarm.UserAlarm
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.Remind
import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.day.daySpans
import com.dcc.yawm.shared.prayer.PrayerTimesEngine
import com.dcc.yawm.shared.screen.Ask
import com.dcc.yawm.shared.screen.AssistantScope
import com.dcc.yawm.shared.screen.Where
import com.dcc.yawm.shared.screen.card
import com.dcc.yawm.shared.screen.movedTo
import com.dcc.yawm.shared.screen.understand
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** «المساعد»: Iraqi sentences in, one clear request out; nothing it does not understand is guessed. */
class AssistantTest {
    private val s = UserSettings()
    private val spans = daySpans(PrayerTimesEngine.compute(s.place, LocalDate(2026, 11, 2), TimeZone.of(s.zoneId), s.prayer), 21 * 60)
    private val sport = Thing("h1", "الرياضة", at = 16 * 60 + 30, repeat = Repeat.DAILY, remind = Remind.AT)
    private val accountant = Thing("h2", "اتصال بالمحاسب", DayPart.ASR, LocalDate(2026, 11, 2))
    private val work = UserAlarm("a1", "الدوام", 5 * 60 + 30)
    private val now = 15 * 60 + 10

    private fun ask(text: String, at: Int = now) = understand(text, listOf(sport, accountant), listOf(work), spans, at, DayPart.ASR)

    private fun alarmAt(text: String, at: Int = now): Int {
        val a = assertIs<Ask.AddAlarm>(ask(text, at), text)
        return a.draft.at!!
    }

    @Test
    fun spokenTimesForAlarms() {
        assertEquals(5 * 60 + 30, alarmAt("صحّيني باچر خمسة ونص"))
        assertEquals(5 * 60 + 30, alarmAt("صحيني باجر 5 ونص"))
        assertEquals(6 * 60 + 45, alarmAt("صحيني سبعة الا ربع"))
        assertEquals(4 * 60 + 15, alarmAt("گعدني اربعة وربع"))
        assertEquals(6 * 60 + 20, alarmAt("صحيني ستة وثلث"))
        assertEquals(5 * 60 + 45, alarmAt("منبه الساعة 05:45 كل يوم"))
        assertEquals(5 * 60 + 45, alarmAt("منبّه الساعة ٠٥:٤٥"))
        assertEquals(21 * 60, alarmAt("منبه الساعة تسعة بالليل"))
        assertEquals(16 * 60 + 10, alarmAt("منبه بعد ساعة"))
        assertEquals(15 * 60 + 40, alarmAt("منبّه بعد نص ساعة"))
        assertEquals(15 * 60 + 30, alarmAt("منبه بعد 20 دقيقة"))
        // Without a word of the day, the next time this hour comes.
        assertEquals(18 * 60, alarmAt("منبه الساعة ستة"))
        assertEquals(9 * 60, alarmAt("منبه الساعة تسعة", at = 8 * 60))
    }

    @Test
    fun anAlarmIsOnceUnlessTheWordsRepeatIt() {
        val once = assertIs<Ask.AddAlarm>(ask("صحّيني باچر خمسة ونص"))
        assertEquals(Repeat.ONCE, once.draft.repeat)
        assertEquals(Where.Tomorrow, once.draft.where)
        assertEquals("باچر", once.card(spans)!!.facts.first { it.first == "إمتى" }.second)
        assertEquals(Repeat.DAILY, assertIs<Ask.AddAlarm>(ask("منبه الساعة 05:45 كل يوم")).draft.repeat)
        assertEquals(Repeat.WORKDAYS, assertIs<Ask.AddAlarm>(ask("صحيني ستة ايام الدوام")).draft.repeat)
        assertEquals("الدواء", assertIs<Ask.AddAlarm>(ask("منبه الدواء الساعة تسعة بالليل")).draft.title)
        assertIs<Ask.Tell>(ask("صحيني"), "an alarm needs a time")
    }

    @Test
    fun addingThings() {
        val call = assertIs<Ask.AddThing>(ask("ذكّرني أتصل بأمي بعد المغرب كل يوم")).draft
        assertEquals("أتصل بأمي", call.title)
        assertEquals(Where.In(DayPart.MAGHRIB), call.where)
        assertEquals(Repeat.DAILY, call.repeat)
        assertNull(call.at)
        assertEquals(Remind.START, call.remind)

        val night = assertIs<Ask.AddThing>(ask("رياضة الساعة 9 بالليل")).draft
        assertEquals("رياضة", night.title)
        assertEquals(21 * 60, night.at)
        assertEquals(Remind.AT, night.remind)

        val soon = assertIs<Ask.AddThing>(ask("قراءة بعد ساعة")).draft
        assertEquals("قراءة", soon.title)
        assertEquals(16 * 60 + 10, soon.at)

        val visit = assertIs<Ask.AddThing>(ask("زيارة خالي باچر بعد العصر")).draft
        assertEquals("زيارة خالي", visit.title)
        assertEquals(Where.Tomorrow, visit.where)

        assertTrue(assertIs<Ask.AddThing>(ask("تصليح السيارة بعدين")).draft.where == Where.Later)
        assertEquals("قرآن", assertIs<Ask.AddThing>(ask("قرآن بعد الفجر كل يوم")).draft.title)
        // A number that is not a time stays in the title.
        val pages = assertIs<Ask.AddThing>(ask("قراءة 10 صفحات")).draft
        assertEquals("قراءة 10 صفحات", pages.title)
        assertNull(pages.at)
        // «ستة» with a word of the day is a time.
        assertEquals(6 * 60, assertIs<Ask.AddThing>(ask("مشي ستة الصبح")).draft.at)
    }

    @Test
    fun changingTimes() {
        val m = assertIs<Ask.MoveThing>(ask("خلّي الرياضة الساعة ستة"))
        assertEquals(sport, m.thing)
        assertEquals(18 * 60, m.to)
        assertEquals(17 * 60, assertIs<Ask.MoveThing>(ask("أخّر الرياضة نص ساعة")).to)
        assertEquals(16 * 60 + 15, assertIs<Ask.MoveThing>(ask("قدّم الرياضة ربع ساعة")).to)
        assertEquals(16 * 60, assertIs<Ask.MoveThing>(ask("غيّر اتصال المحاسب للساعة اربعة")).to)
        val alarm = assertIs<Ask.MoveAlarm>(ask("خلي منبه الدوام ستة"))
        assertEquals(6 * 60, alarm.to)
        assertEquals(6 * 60 + 30, assertIs<Ask.MoveAlarm>(ask("أخر منبه الدوام ساعة")).to)
        assertIs<Ask.Tell>(ask("غير الرياضة"), "a change needs a time")
        assertIs<Ask.Tell>(ask("خلي السباحة الساعة ستة"), "nothing by that name")
        // A moved thing keeps its reminder, at its own time.
        assertEquals(Remind.AT, accountant.movedTo(16 * 60).remind)
        assertNull(accountant.movedTo(16 * 60).part)
    }

    @Test
    fun deleting() {
        assertEquals(work, assertIs<Ask.DeleteAlarm>(ask("شيل منبّه الدوام")).alarm)
        assertEquals(sport, assertIs<Ask.DeleteThing>(ask("احذف الرياضة")).thing)
        assertEquals(accountant, assertIs<Ask.DeleteThing>(ask("امسح اتصال المحاسب")).thing)
        assertIs<Ask.Tell>(ask("شيل السباحة"))
    }

    @Test
    fun itSaysWhatItCannotDo() {
        assertEquals(AssistantScope, assertIs<Ask.Tell>(ask("شنو الجو اليوم")).text)
        assertEquals(AssistantScope, assertIs<Ask.Tell>(ask("شلون حالك؟")).text)
        val list = assertIs<Ask.Tell>(ask("شنو عندي اليوم")).text
        assertTrue("الرياضة" in list && "الدوام" in list, list)
        assertIs<Ask.Tell>(ask("   "))
    }

    @Test
    fun theCardsSayWhatWillHappen() {
        val c = ask("ذكّرني أتصل بأمي بعد المغرب كل يوم").card(spans)!!
        assertEquals("أضيف شغلة", c.head)
        assertEquals("إي، ضيفها", c.yes)
        assertTrue(c.facts.contains("وين" to "بعد المغرب"))
        val mv = ask("خلّي الرياضة الساعة ستة").card(spans)!!
        assertTrue(mv.facts.contains("كان" to "16:30"))
        assertTrue(mv.facts.first { it.first == "يصير" }.second.startsWith("18:00"))
        assertNull(ask("شنو الجو").card(spans))
    }
}
