package com.dcc.yawm.shared

import com.dcc.yawm.shared.day.arabicDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArabicDateTest {
    @Test
    fun formatsIraqiMonthWithWesternDigits() {
        assertEquals("27 تشرين الأول 2026", arabicDate(SimpleDate(2026, 10, 27)))
        assertEquals("1 كانون الثاني 2027", arabicDate(SimpleDate(2027, 1, 1)))
    }

    @Test
    fun rejectsImpossibleMonth() {
        assertFailsWith<IllegalArgumentException> { arabicDate(SimpleDate(2026, 13, 1)) }
    }
}
