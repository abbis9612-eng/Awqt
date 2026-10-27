package com.dcc.yawm.shared.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class AndroidDeviceControls(private val activity: ComponentActivity) : DeviceControls {
    private val state = MutableStateFlow(read())
    override val status: StateFlow<DeviceStatus> = state

    fun refresh() { state.value = read() }

    private fun read(): DeviceStatus {
        val app = activity.applicationContext
        return DeviceStatus(
            notificationsAllowed = app.getSystemService(NotificationManager::class.java).areNotificationsEnabled(),
            exactAlarmsAllowed = AlarmScheduler.canScheduleExact(app.getSystemService(AlarmManager::class.java)),
            batteryUnrestricted = app.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(app.packageName),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            scheduledCount = AlarmScheduler.scheduledCount(app),
            next = AlarmScheduler.next(app),
        )
    }

    override fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else {
            open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName))
        }
    }

    override fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${activity.packageName}")))
        }
    }

    override fun openBatterySettings() {
        if (!open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) appDetails()
    }

    override fun openAdhanSound() {
        Notifications.ensureChannels(activity)
        open(
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, Notifications.ADHAN),
        )
    }

    override fun reschedule() {
        AlarmScheduler.rescheduleAsync(activity) { activity.runOnUiThread { refresh() } }
    }

    private fun appDetails() =
        open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}")))

    private fun open(intent: Intent): Boolean = try {
        activity.startActivity(intent); true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
