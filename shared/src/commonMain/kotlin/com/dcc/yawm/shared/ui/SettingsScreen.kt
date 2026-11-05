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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role as SemanticRole
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

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
    openNight: () -> Unit,
    openAlerts: () -> Unit,
    openWeek: () -> Unit,
    openPage: () -> Unit,
    close: () -> Unit,
) {
    val pal = LocalPalette.current
    // One short list: each row says what is set now; a tap opens it in place, one at a time (owner, 2026-11-05:
    // the long settings page was «معقد و غير عملي»).
    var open by remember { mutableStateOf<String?>(null) }
    fun toggle(k: String) { open = if (open == k) null else k }

    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) { PillButton("رجوع", close) }
    Display("الإعدادات", 32.sp)
    Txt("اضغط على أي سطر حتى تغيّره.", size = 15.sp, color = pal.grey)

    Heading("يومك")
    CardGroup {
        LinkRow("المنبّهات والتنبيهات", "منبّهاتك، تنبيه الصلاة، الأصوات", openAlerts)
        LinkRow("الليل والنوم", "شوكت تنام، صلاة الليل، القيلولة", openNight)
        LinkRow("الأسبوع", "شلون مشى أسبوعك", openWeek)
        LinkRow("محاسبة المساء", "عشر نقاط وثلاث أسئلة، دقيقتين", openPage, last = true)
    }

    Heading("مكانك وأوقات الصلاة")
    CardGroup {
        Section("مدينتك", settings.cityName, open == "city", { toggle("city") }) {
            CityChooser(settings, platform, save)
        }
        Section("طريقة الحساب", methodName(settings.prayer.method), open == "method", { toggle("method") }) {
            for (m in CalcMethod.entries) {
                ChoiceRow(methodName(m), settings.prayer.method == m) { save(settings.copy(prayer = settings.prayer.copy(method = m))) }
            }
        }
        Section("وقت العصر", madhabName(settings.prayer.madhab), open == "asr", { toggle("asr") }) {
            val order = AsrMadhab.entries
            Seg(order.map { madhabName(it).substringBefore(':') }, order.indexOf(settings.prayer.madhab)) { i ->
                save(settings.copy(prayer = settings.prayer.copy(madhab = order[i])))
            }
            Txt(madhabName(settings.prayer.madhab), size = 14.sp, color = pal.grey)
        }
        val a = settings.prayer.mosqueAdjustments
        val moved = listOf(a.fajr, a.sunrise, a.dhuhr, a.asr, a.maghrib, a.isha).count { it != 0 }
        Section("طابق جامعك", if (moved == 0) "مثل الحساب بالضبط" else "معدّل $moved أوقات", open == "mosque", { toggle("mosque") }) {
            Txt("إذا جامعك يأذّن قبل أو بعد بدقايق، زيد أو نقّص هنا.", size = 14.sp, color = pal.grey)
            fun adj(change: (MosqueAdjustments) -> MosqueAdjustments) = save(settings.copy(prayer = settings.prayer.copy(mosqueAdjustments = change(a))))
            AdjustRow("الفجر", a.fajr) { v -> adj { it.copy(fajr = v) } }
            AdjustRow("الشروق", a.sunrise) { v -> adj { it.copy(sunrise = v) } }
            AdjustRow("الظهر", a.dhuhr) { v -> adj { it.copy(dhuhr = v) } }
            AdjustRow("العصر", a.asr) { v -> adj { it.copy(asr = v) } }
            AdjustRow("المغرب", a.maghrib) { v -> adj { it.copy(maghrib = v) } }
            AdjustRow("العشاء", a.isha) { v -> adj { it.copy(isha = v) } }
        }
        Section("البلدان البعيدة عن خط الاستواء", highLatitudeName(settings.prayer.highLatitudeRule), open == "far", { toggle("far") }, last = true) {
            for (r in listOf<HighLatitudeRule?>(null) + HighLatitudeRule.entries) {
                ChoiceRow(highLatitudeName(r), settings.prayer.highLatitudeRule == r) { save(settings.copy(prayer = settings.prayer.copy(highLatitudeRule = r))) }
            }
        }
    }

    Heading("إنت")
    CardGroup {
        Section("أيام الدوام", daysText(settings.workDays), open == "work", { toggle("work") }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (d in SatFirst) Chip(DayShort[d.ordinal], d in settings.workDays, Modifier.weight(1f)) {
                    save(settings.copy(workDays = if (d in settings.workDays) settings.workDays - d else settings.workDays + d))
                }
            }
        }
        Section("أدوارك", "${settings.roles.size} من ${Role.entries.size} · نقاط المحاسبة تتبعها", open == "roles", { toggle("roles") }) {
            RolesChooser(settings, save)
        }
        Section("عن «يوم»", "بلا إنترنت، بلا حساب، بلا إعلانات", open == "about", { toggle("about") }, last = true) {
            Txt("كل شي يبقى على تلفونك. التطبيق يذكّر وينظّم، وما يفتي.", size = 15.sp, color = pal.soft)
            Txt("الأذكار والآيات تنضاف بس بعد ما يراجعها مراجع شرعي، ووياها مصدرها ودرجتها.", size = 15.sp, color = pal.soft)
            Txt(
                "الخطوط: Lalezar وVazirmatn، ووياهم نوتو كوفي عربي وعارف رقعة ونوتو نسخ عربي وأميري، كلها برخصة SIL Open Font License 1.1. المواقيت تنحسب بمكتبة Adhan.",
                size = 13.5.sp, color = pal.grey,
            )
        }
    }
    Spacer(Modifier.height(24.dp))
}

/** A row that opens another screen. */
@Composable
private fun LinkRow(title: String, sub: String, onClick: () -> Unit, last: Boolean = false) {
    ListRow(onClick = onClick, minHeight = 64.dp, divider = !last) {
        RowText(title, sub, bold = true)
        Txt("‹", size = 22.sp, bold = true)
    }
}

/** A setting: its name and what is set now; a tap opens its choices under it. */
@Composable
private fun Section(title: String, value: String, isOpen: Boolean, onToggle: () -> Unit, last: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val pal = LocalPalette.current
    Column(Modifier.fillMaxWidth().drawBehind {
        if (!last) drawLine(pal.line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
    }) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onToggle)
                .semantics { role = SemanticRole.Button; stateDescription = if (isOpen) "مفتوح" else "مسكّر" }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Txt(title, size = 17.sp, bold = true, lineHeight = 1.4f)
                Txt(value, size = 14.sp, color = pal.grey, lineHeight = 1.45f)
            }
            Txt(if (isOpen) "تم" else "غيّر", size = 15.sp, bold = true, lineHeight = 1.3f)
        }
        if (isOpen) Column(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
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
