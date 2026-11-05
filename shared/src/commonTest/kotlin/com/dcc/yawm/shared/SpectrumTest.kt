package com.dcc.yawm.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.screen.Field
import com.dcc.yawm.shared.screen.fieldLine
import com.dcc.yawm.shared.screen.fieldName
import com.dcc.yawm.shared.screen.partsView
import com.dcc.yawm.shared.screen.spectrum
import com.dcc.yawm.shared.ui.FieldColour
import com.dcc.yawm.shared.ui.fieldPalette
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** «الألوان»: the day's spectrum, the field of a minute, the sentence, and the colours' contrast. */
class SpectrumTest {
    private val s = UserSettings()
    private val nov2 = LocalDate(2026, 11, 2)

    private fun view(t: Int) = partsView(s, nov2, nov2, t)

    @Test
    fun theSpectrumRunsFromFajrToTheNextFajrWithNoGaps() {
        val v = view(970)
        val sp = spectrum(v)
        assertEquals(7, sp.segments.size)
        assertEquals(Field.entries.toList(), sp.segments.map { it.field })
        assertEquals(v.day.times.fajr, sp.start)
        assertEquals(v.day.nextFajr, sp.end)
        sp.segments.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start) }
        sp.segments.forEach { assertTrue(it.length > 0, "${it.field} has no length") }
        assertTrue(abs(sp.segments.sumOf { sp.fraction(it).toDouble() } - 1.0) < 1e-4)
        // Each part keeps its real length: the bar is the same day as the parts.
        v.parts.forEach { p -> val seg = sp.segment(Field.of(p.part)); assertEquals(p.span.start, seg.start); assertEquals(p.span.end, seg.end) }
        assertEquals(v.bedtime.coerceAtLeast(v.parts.last().span.end), sp.segment(Field.NIGHT).start)
    }

    @Test
    fun aMinuteFallsInItsField() {
        val v = view(970)
        val sp = spectrum(v)
        val asr = v.parts.first { it.part == DayPart.ASR }.span
        assertEquals(Field.ASR, sp.fieldAt(asr.start))
        assertEquals(Field.ASR, sp.fieldAt(asr.end - 1))
        assertEquals(Field.MAGHRIB, sp.fieldAt(asr.end))
        assertEquals(Field.NIGHT, sp.fieldAt(sp.segment(Field.NIGHT).start + 5))
        assertEquals(Field.NIGHT, sp.fieldAt(1440 + 60))
        assertEquals(Field.NIGHT, sp.fieldAt(sp.end - 1))
    }

    @Test
    fun theNeedleAndATapAgree() {
        val sp = spectrum(view(970))
        assertEquals(0f, sp.needle(sp.start))
        assertEquals(1f, sp.needle(sp.end))
        var last = -1f
        for (m in sp.start..sp.end step 7) { val n = sp.needle(m); assertTrue(n >= last); last = n }
        assertEquals(Field.FAJR, sp.fieldAtFraction(0f))
        assertEquals(Field.NIGHT, sp.fieldAtFraction(0.9999f))
        for (seg in sp.segments) {
            val mid = sp.needle((seg.start + seg.end) / 2)
            assertEquals(seg.field, sp.fieldAtFraction(mid), "tap in the middle of ${seg.field}")
        }
    }

    @Test
    fun theSentenceSaysWhatIsLeftWhatWasDoneOrWhenItBegins() {
        val v = view(970)
        val sp = spectrum(v)
        val asr = v.parts.first { it.part == DayPart.ASR }.span
        val now = fieldLine(v, sp, Field.ASR, 970)
        assertTrue(now.startsWith("باقي ") && "المغرب" in now, now)
        assertTrue(fieldLine(v, sp, Field.FAJR, 970).startsWith("خلص"))
        assertTrue(fieldLine(v, sp, Field.ISHA, 970).startsWith("يبدي "))
        assertTrue(fieldLine(v, sp, Field.NIGHT, 970).startsWith("تنام "))
        assertTrue(asr.end > 970)
        assertEquals(listOf("بعد", "العصر"), fieldName(Field.ASR))
        assertEquals(listOf("الليل"), fieldName(Field.NIGHT))
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return maxOf(la, lb) / minOf(la, lb)
    }

    @Test
    fun everyFieldReadsAtFourAndAHalfToOne() {
        for (f in Field.entries) {
            val c = FieldColour.getValue(f)
            val p = fieldPalette(f)
            assertTrue(contrast(c.fg, c.bg) >= 4.5, "$f text ${contrast(c.fg, c.bg)}")
            assertTrue(contrast(p.grey, p.paper) >= 4.5, "$f grey ${contrast(p.grey, p.paper)}")
        }
    }
}
