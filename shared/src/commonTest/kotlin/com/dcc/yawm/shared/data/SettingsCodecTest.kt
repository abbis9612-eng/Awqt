package com.dcc.yawm.shared.data

import com.dcc.yawm.shared.day.Role
import com.dcc.yawm.shared.prayer.AsrMadhab
import com.dcc.yawm.shared.prayer.CalcMethod
import com.dcc.yawm.shared.prayer.GeoPoint
import com.dcc.yawm.shared.prayer.HighLatitudeRule
import com.dcc.yawm.shared.prayer.MosqueAdjustments
import com.dcc.yawm.shared.prayer.PrayerSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsCodecTest {
    @Test
    fun emptyStorageGivesTheDefaults() {
        assertEquals(UserSettings(), decodeSettings(emptyMap()))
    }

    @Test
    fun everyFieldSurvivesARoundTrip() {
        val london = UserSettings(
            cityName = "London",
            place = GeoPoint(51.5072, -0.1276),
            zoneId = "Europe/London",
            prayer = PrayerSettings(
                CalcMethod.MUSLIM_WORLD_LEAGUE,
                AsrMadhab.HANAFI,
                HighLatitudeRule.SEVENTH_OF_THE_NIGHT,
                MosqueAdjustments(fajr = 2, sunrise = -1, dhuhr = 3, asr = 0, maghrib = 4, isha = -5),
            ),
            bedtime = 22 * 60 + 15,
            qiyamEnabled = false,
            roles = setOf(Role.WORSHIP, Role.BODY, Role.CHILD),
            blockReminders = false,
            wakeAlarm = false,
        )
        assertEquals(london, decodeSettings(encodeSettings(london)))
        assertEquals(UserSettings(), decodeSettings(encodeSettings(UserSettings())))
    }

    @Test
    fun noRolesChosenStaysEmpty() {
        val none = UserSettings(roles = emptySet())
        assertEquals(none, decodeSettings(encodeSettings(none)))
    }

    @Test
    fun unreadableValuesFallBackOneByOne() {
        val stored = encodeSettings(UserSettings(cityName = "Erbil", bedtime = 1320)) + mapOf(
            "city.latitude" to "north",
            "prayer.method" to "NOT_A_METHOD",
            "prayer.adjust.fajr" to "900",
            "night.qiyam" to "maybe",
            "roles" to "WORSHIP,UNKNOWN",
        )
        val d = UserSettings()
        val s = decodeSettings(stored)
        assertEquals("Erbil", s.cityName)
        assertEquals(1320, s.bedtime)
        assertEquals(d.place, s.place)
        assertEquals(d.prayer.method, s.prayer.method)
        assertEquals(d.prayer.mosqueAdjustments.fajr, s.prayer.mosqueAdjustments.fajr)
        assertEquals(d.qiyamEnabled, s.qiyamEnabled)
        assertEquals(setOf(Role.WORSHIP), s.roles)
    }
}
