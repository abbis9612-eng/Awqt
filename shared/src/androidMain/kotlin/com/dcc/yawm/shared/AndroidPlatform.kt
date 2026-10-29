package com.dcc.yawm.shared

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.IslamicCalendar
import android.icu.util.ULocale
import android.location.Location
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.dcc.yawm.shared.alarm.AlarmActivity
import com.dcc.yawm.shared.alarm.AlarmReceiver
import com.dcc.yawm.shared.alarm.Notifications
import com.dcc.yawm.shared.alarm.ReminderKind
import com.dcc.yawm.shared.prayer.GeoPoint
import com.dcc.yawm.shared.ui.Platform
import com.dcc.yawm.shared.ui.YawmFonts
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

private val HijriMonths = listOf(
    "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
    "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
)

internal class AndroidPlatform(private val activity: ComponentActivity) : Platform {
    // The OFL fonts ship in androidApp/src/main/assets/fonts (IDENTITY.md; no network in v1).
    // Noto Naskh is a variable font: each weight sets its own wght axis.
    override val fonts = YawmFonts(
        ruqaa = FontFamily(Font("fonts/ArefRuqaa-Bold.ttf", activity.assets, FontWeight.Bold)),
        naskh = FontFamily(
            Font("fonts/NotoNaskhArabic-wght.ttf", activity.assets, FontWeight.Normal),
            Font("fonts/NotoNaskhArabic-wght.ttf", activity.assets, FontWeight.Bold),
        ),
        amiri = FontFamily(Font("fonts/Amiri-Regular.ttf", activity.assets, FontWeight.Normal)),
        // Variable as well: ExtraBold sets wght 800, Bold 700.
        display = FontFamily(
            Font("fonts/NotoKufiArabic-wght.ttf", activity.assets, FontWeight.ExtraBold),
            Font("fonts/NotoKufiArabic-wght.ttf", activity.assets, FontWeight.Bold),
        ),
    )

    override val reducedMotion: Boolean
        get() = Settings.Global.getFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    override val systemZoneId: String get() = java.util.TimeZone.getDefault().id

    override fun hijri(date: LocalDate): String {
        val cal = IslamicCalendar(android.icu.util.TimeZone.GMT_ZONE, ULocale.ROOT)
        cal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        cal.timeInMillis = date.toEpochDays().toLong() * 86_400_000L + 12 * 3_600_000L
        return "${cal.get(android.icu.util.Calendar.DAY_OF_MONTH)} ${HijriMonths[cal.get(android.icu.util.Calendar.MONTH)]} ${cal.get(android.icu.util.Calendar.EXTENDED_YEAR)}"
    }

    override fun locate(onResult: (GeoPoint?) -> Unit) {
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 2)
            onResult(null)
            return
        }
        val lm = activity.getSystemService(LocationManager::class.java)
        try {
            val last = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
            when {
                last != null -> onResult(cityLevel(last))
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, activity.mainExecutor) { onResult(it?.let(::cityLevel)) }
                else -> onResult(null)
            }
        } catch (e: SecurityException) {
            onResult(null)
        }
    }

    /** Two decimals, about a kilometre: city level only (CLAUDE.md rule 5). */
    private fun cityLevel(l: Location) = GeoPoint((l.latitude * 100).roundToInt() / 100.0, (l.longitude * 100).roundToInt() / 100.0)

    @Composable
    override fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
        androidx.activity.compose.BackHandler(enabled, onBack)
    }

    @Suppress("DEPRECATION")
    override fun systemBars(dark: Boolean) {
        val window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (dark) 0 else light, light)
        } else {
            val flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            val v = window.decorView
            v.systemUiVisibility = if (dark) v.systemUiVisibility and flags.inv() else v.systemUiVisibility or flags
        }
    }

    private var player: MediaPlayer? = null

    /** «اسمع»: plays one of the app's sounds (res/raw of the app) once. */
    override fun playSound(file: String) {
        @Suppress("DiscouragedApi")
        val id = activity.resources.getIdentifier(file, "raw", activity.packageName)
        if (id == 0) return
        player?.release()
        player = MediaPlayer.create(activity, id)?.apply {
            setOnCompletionListener { it.release(); if (player === it) player = null }
            start()
        }
    }

    /** «جرّبها»: a real alarm now, through the same notification and screen the alarm uses. */
    override fun testAlarm() {
        val now = System.currentTimeMillis()
        val extras = Intent()
            .putExtra(AlarmReceiver.KIND, ReminderKind.ALARM.name)
            .putExtra(AlarmReceiver.ID, TEST_ALARM_ID)
            .putExtra(AlarmReceiver.TITLE, "تجربة المنبّه")
            .putExtra(AlarmReceiver.BODY, "هيچ يطلع منبّهك")
            .putExtra(AlarmReceiver.AT, now)
        Notifications.show(activity, extras)
        activity.startActivity(Intent(activity, AlarmActivity::class.java).putExtras(extras))
    }

    private companion object {
        const val TEST_ALARM_ID = 7_000_001
    }
}
