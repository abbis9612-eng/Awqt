package com.dcc.yawm.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.alarm.DeviceStatus
import com.dcc.yawm.shared.alarm.batteryAdvice
import com.dcc.yawm.shared.data.Theme
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.Role
import com.dcc.yawm.shared.prayer.AsrMadhab
import com.dcc.yawm.shared.prayer.CalcMethod
import com.dcc.yawm.shared.prayer.HighLatitudeRule
import com.dcc.yawm.shared.prayer.MosqueAdjustments
import com.dcc.yawm.shared.screen.Cities
import com.dcc.yawm.shared.screen.highLatitudeName
import com.dcc.yawm.shared.screen.madhabName
import com.dcc.yawm.shared.screen.methodName
import com.dcc.yawm.shared.screen.roleName

/** A choice row: bold with a check when chosen. */
@Composable
fun ChoiceRow(title: String, chosen: Boolean, sub: String? = null, onClick: () -> Unit) {
    ListRow(onClick = onClick) {
        RowText(title, sub, bold = chosen)
        Check(chosen)
    }
}

/** City list plus the phone's approximate location (used on the device only). */
@Composable
fun CityChooser(settings: UserSettings, platform: Platform, save: (UserSettings) -> Unit) {
    var locating by remember { mutableStateOf<String?>(null) }
    for (c in Cities) {
        ChoiceRow(c.name, settings.cityName == c.name && settings.place == c.place, methodName(c.prayer.method)) {
            save(settings.copy(cityName = c.name, place = c.place, zoneId = c.zoneId, prayer = c.prayer.copy(mosqueAdjustments = settings.prayer.mosqueAdjustments)))
        }
    }
    ListRow(onClick = {
        locating = "ندوّر على موقعك التقريبي…"
        platform.locate { p ->
            if (p == null) {
                locating = "ما كدرنا نعرف الموقع. اسمح بالموقع التقريبي وارجع اضغط مرة ثانية، أو اختار مدينة."
            } else {
                locating = null
                save(settings.copy(cityName = "موقعي", place = p, zoneId = platform.systemZoneId))
            }
        }
    }) {
        RowText("موقعي الحالي", locating ?: "تقريبي، يبقى على تلفونك وما يطلع منه", bold = settings.cityName == "موقعي")
        Check(settings.cityName == "موقعي")
    }
}

@Composable
fun RolesChooser(settings: UserSettings, save: (UserSettings) -> Unit) {
    for (r in Role.entries) {
        ListRow {
            RowText(roleName(r))
            InkSwitch(r in settings.roles, roleName(r)) { on ->
                save(settings.copy(roles = if (on) settings.roles + r else settings.roles - r))
            }
        }
    }
}

/** Notifications permission, exact alarms and battery: the self-check of CLAUDE.md. */
@Composable
fun AlarmChecks(status: DeviceStatus, device: com.dcc.yawm.shared.alarm.DeviceControls) {
    val pal = LocalPalette.current
    Txt(
        status.next?.let { "التنبيه الجاي: $it · ${status.scheduledCount} تنبيه مضبوط لـ48 ساعة" } ?: "بعد ما مضبوط أي تنبيه",
        size = 15.sp, color = pal.grey,
    )
    ListRow(onClick = if (status.notificationsAllowed) null else device::requestNotifications) {
        RowText("الإشعارات", if (status.notificationsAllowed) "مسموحة" else "مطفية: اضغط حتى تسمح")
        Check(status.notificationsAllowed)
    }
    ListRow(onClick = if (status.exactAlarmsAllowed) null else device::openExactAlarmSettings) {
        RowText(
            "الأذان بوقته بالدقيقة",
            if (status.exactAlarmsAllowed) "«المنبهات والتذكيرات» مسموحة"
            else "بدون هذا الإذن الأذان ممكن يتأخر دقائق. اضغط وفعّل «السماح بضبط المنبهات والتذكيرات».",
        )
        Check(status.exactAlarmsAllowed)
    }
    ListRow(onClick = if (status.batteryUnrestricted) null else device::openBatterySettings) {
        RowText(
            "البطارية ما توقف التطبيق",
            if (status.batteryUnrestricted) "«يوم» بلا قيود" else "اضغط، واختار «كل التطبيقات» ثم «يوم» ثم «لا تحسّن».",
        )
        Check(status.batteryUnrestricted)
    }
    if (!status.batteryUnrestricted) {
        for (step in batteryAdvice(status.manufacturer)) Txt("• $step", size = 14.5.sp, color = pal.grey, modifier = Modifier.padding(vertical = 2.dp))
    }
    ListRow(onClick = device::openAdhanSound) {
        RowText("صوت الأذان", "اختاره من أصوات تلفونك")
        Txt("‹", bold = true)
    }
}

@Composable
fun SettingsScreen(
    settings: UserSettings,
    status: DeviceStatus,
    device: com.dcc.yawm.shared.alarm.DeviceControls,
    platform: Platform,
    save: (UserSettings) -> Unit,
    openMyDay: () -> Unit,
    openDayEditor: () -> Unit,
    close: () -> Unit,
) {
    val pal = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Display("الإعدادات", 32.sp)
        PillButton("رجوع", close)
    }

    Heading("يومك")
    ListRow(onClick = openMyDay) {
        Tile("week", false, TileSize.SM)
        RowText(
            if (settings.myDay != null) "يومك" else "ابنِ يومك",
            if (settings.myDay != null) "حياتك، وشغلاتك، ووين تجي ويّا الصلاة" else "تبدي من الصلاة بس، وتضيف شغلاتك إنت",
            bold = true,
        )
        Txt("‹", bold = true)
    }
    if (settings.myDay == null) ListRow(onClick = openDayEditor) {
        RowText("عدّل الترتيب الجاهز", "الفقرات الجاهزة، وأيام الدوام، والمواعيد الثابتة")
        Txt("‹", bold = true)
    }

    Heading("المدينة")
    CityChooser(settings, platform, save)

    Heading("طريقة الحساب")
    for (m in CalcMethod.entries) {
        ChoiceRow(methodName(m), settings.prayer.method == m) { save(settings.copy(prayer = settings.prayer.copy(method = m))) }
    }
    Heading("وقت العصر")
    for (m in AsrMadhab.entries) {
        ChoiceRow(madhabName(m), settings.prayer.madhab == m) { save(settings.copy(prayer = settings.prayer.copy(madhab = m))) }
    }
    Heading("البلدان البعيدة عن خط الاستواء")
    for (r in listOf<HighLatitudeRule?>(null) + HighLatitudeRule.entries) {
        ChoiceRow(highLatitudeName(r), settings.prayer.highLatitudeRule == r) { save(settings.copy(prayer = settings.prayer.copy(highLatitudeRule = r))) }
    }

    Heading("طابق جامعك (دقائق تنضاف لكل وقت)")
    val a = settings.prayer.mosqueAdjustments
    fun adj(change: (MosqueAdjustments) -> MosqueAdjustments) = save(settings.copy(prayer = settings.prayer.copy(mosqueAdjustments = change(a))))
    AdjustRow("الفجر", a.fajr) { v -> adj { it.copy(fajr = v) } }
    AdjustRow("الشروق", a.sunrise) { v -> adj { it.copy(sunrise = v) } }
    AdjustRow("الظهر", a.dhuhr) { v -> adj { it.copy(dhuhr = v) } }
    AdjustRow("العصر", a.asr) { v -> adj { it.copy(asr = v) } }
    AdjustRow("المغرب", a.maghrib) { v -> adj { it.copy(maghrib = v) } }
    AdjustRow("العشاء", a.isha) { v -> adj { it.copy(isha = v) } }

    Heading("فحص التنبيهات")
    Txt("المنبّهات وتنبيهات الفقرات والصلاة صارت بصفحة «المنبّه» تحت.", size = 14.sp, color = pal.grey)
    AlarmChecks(status, device)

    Heading("أدوارك (النقاط تتبعها)")
    RolesChooser(settings, save)

    Heading("الشكل")
    ChoiceRow("تلقائي", settings.theme == Theme.AUTO, "داكن من العشاء لحد الفجر") { save(settings.copy(theme = Theme.AUTO)) }
    ChoiceRow("فاتح دايماً", settings.theme == Theme.LIGHT) { save(settings.copy(theme = Theme.LIGHT)) }
    ChoiceRow("داكن دايماً", settings.theme == Theme.DARK) { save(settings.copy(theme = Theme.DARK)) }

    Heading("عن «يوم»")
    Txt(
        "بلا إنترنت وبلا حساب وبلا إعلانات. كل شي يبقى على تلفونك. التطبيق يذكّر وينظّم، وما يفتي.",
        size = 15.sp, color = pal.soft,
    )
    Txt(
        "الأذكار والآيات تنضاف بس بعد ما يراجعها مراجع شرعي، ووياها مصدرها ودرجتها.",
        size = 15.sp, color = pal.soft, modifier = Modifier.padding(top = 6.dp),
    )
    Txt(
        "الخطوط: نوتو كوفي عربي، وعارف رقعة، ونوتو نسخ عربي، وأميري، كلها برخصة SIL Open Font License 1.1. المواقيت تنحسب بمكتبة Adhan.",
        size = 13.5.sp, color = pal.grey, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
    )
}

@Composable
private fun AdjustRow(name: String, value: Int, set: (Int) -> Unit) {
    ListRow {
        RowText(name)
        Stepper(
            if (value > 0) "+$value" else "$value", "دقيقة أبكر", "دقيقة أتأخر",
            onMinus = { set((value - 1).coerceAtLeast(-30)) },
            onPlus = { set((value + 1).coerceAtMost(30)) },
        )
    }
}
