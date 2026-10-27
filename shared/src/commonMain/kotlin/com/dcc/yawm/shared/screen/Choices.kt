package com.dcc.yawm.shared.screen

import com.dcc.yawm.shared.day.Role
import com.dcc.yawm.shared.prayer.AsrMadhab
import com.dcc.yawm.shared.prayer.CalcMethod
import com.dcc.yawm.shared.prayer.GeoPoint
import com.dcc.yawm.shared.prayer.HighLatitudeRule
import com.dcc.yawm.shared.prayer.PrayerSettings

/** A city the user can pick without location. Coordinates are city centres (rule 5: city only). */
data class City(val name: String, val place: GeoPoint, val zoneId: String, val prayer: PrayerSettings)

private val iraq = PrayerSettings(CalcMethod.EGYPTIAN, AsrMadhab.SHAFI)

val Cities = listOf(
    City("السليمانية", GeoPoint(35.5613, 45.4375), "Asia/Baghdad", iraq),
    City("أربيل", GeoPoint(36.1911, 44.0092), "Asia/Baghdad", iraq),
    City("دهوك", GeoPoint(36.8669, 42.9503), "Asia/Baghdad", iraq),
    City("كركوك", GeoPoint(35.4681, 44.3922), "Asia/Baghdad", iraq),
    City("الموصل", GeoPoint(36.3350, 43.1189), "Asia/Baghdad", iraq),
    City("بغداد", GeoPoint(33.3152, 44.3661), "Asia/Baghdad", iraq),
    City("كربلاء", GeoPoint(32.6160, 44.0249), "Asia/Baghdad", iraq),
    City("النجف", GeoPoint(32.0000, 44.3354), "Asia/Baghdad", iraq),
    City("البصرة", GeoPoint(30.5085, 47.7804), "Asia/Baghdad", iraq),
    City(
        "لندن", GeoPoint(51.5074, -0.1278), "Europe/London",
        PrayerSettings(CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.SHAFI, HighLatitudeRule.SEVENTH_OF_THE_NIGHT),
    ),
)

fun methodName(m: CalcMethod): String = when (m) {
    CalcMethod.MUSLIM_WORLD_LEAGUE -> "رابطة العالم الإسلامي"
    CalcMethod.EGYPTIAN -> "الهيئة المصرية العامة للمساحة"
    CalcMethod.KARACHI -> "جامعة العلوم الإسلامية بكراتشي"
    CalcMethod.UMM_AL_QURA -> "أم القرى"
    CalcMethod.DUBAI -> "دبي"
    CalcMethod.MOON_SIGHTING_COMMITTEE -> "لجنة رؤية الهلال"
    CalcMethod.NORTH_AMERICA -> "الجمعية الإسلامية لأمريكا الشمالية"
    CalcMethod.KUWAIT -> "الكويت"
    CalcMethod.QATAR -> "قطر"
    CalcMethod.SINGAPORE -> "سنغافورة"
    CalcMethod.TURKEY -> "رئاسة الشؤون الدينية التركية"
}

fun madhabName(m: AsrMadhab): String = when (m) {
    AsrMadhab.SHAFI -> "الجمهور: الظل مثل الشي"
    AsrMadhab.HANAFI -> "الحنفي: الظل مثلين"
}

fun highLatitudeName(r: HighLatitudeRule?): String = when (r) {
    null -> "تلقائي حسب المدينة"
    HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> "نص الليل"
    HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> "سُبع الليل"
    HighLatitudeRule.TWILIGHT_ANGLE -> "زاوية الشفق"
}

fun roleName(r: Role): String = when (r) {
    Role.WORSHIP -> "العبادة"
    Role.BODY -> "الجسم والنوم"
    Role.SPOUSE -> "الزوج أو الزوجة"
    Role.CHILD -> "الأبناء"
    Role.BUSINESS -> "العمل"
    Role.DEV -> "التركيز والتطوير"
    Role.LEARNING -> "التعلم"
    Role.SELF -> "وقتك لنفسك"
}

/** Icon of each checklist point (prototype POINTS). */
fun pointIcon(number: Int): String = listOf("mosque", "rahl", "phone", "nib", "steps", "nap", "house", "child", "book", "bed")[number - 1]

/** The three evening questions (docs/METHOD.md section 7). */
val EveningQuestions = listOf(
    "ماذا فعلت اليوم لله، ولأهلي، ولبدني، ولعملي؟",
    "أين ضاع وقت بلا فائدة، ولماذا؟",
    "ما أهم 3 مهام غداً؟",
)
