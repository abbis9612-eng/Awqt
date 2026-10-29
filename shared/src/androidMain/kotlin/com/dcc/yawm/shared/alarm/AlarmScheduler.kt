package com.dcc.yawm.shared.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dcc.yawm.shared.data.AndroidDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Sets every alarm of the next 48 hours (CLAUDE.md notifications): exact while-idle alarms for
 * the adhan and block starts, setAlarmClock for the wake alarm, inexact ones without permission.
 */
internal object AlarmScheduler {
    private const val PREFS = "yawm_alarms"
    private const val IDS = "ids"
    private const val NEXT = "next"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()

    fun rescheduleAsync(context: Context, done: () -> Unit = {}) {
        val app = context.applicationContext
        scope.launch {
            try { reschedule(app) } finally { done() }
        }
    }

    fun canScheduleExact(am: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @OptIn(ExperimentalTime::class)
    suspend fun reschedule(app: Context) = lock.withLock {
        val settings = AndroidDatabase.get(app).settings.load()
        val zone = runCatching { TimeZone.of(settings.zoneId) }.getOrElse { TimeZone.currentSystemDefault() }
        val reminders = upcomingReminders(settings, Clock.System.now())
        val am = app.getSystemService(AlarmManager::class.java)
        val prefs = prefs(app)

        prefs.getStringSet(IDS, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.forEach { id ->
            val old = PendingIntent.getBroadcast(app, id, Intent(app, AlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
            if (old != null) { am.cancel(old); old.cancel() }
        }

        Notifications.ensureChannels(app)
        val exact = canScheduleExact(am)
        for (r in reminders) {
            val intent = Intent(app, AlarmReceiver::class.java)
                .putExtra(AlarmReceiver.KIND, r.kind.name)
                .putExtra(AlarmReceiver.ID, r.id)
                .putExtra(AlarmReceiver.TITLE, r.title)
                .putExtra(AlarmReceiver.BODY, r.body)
                .putExtra(AlarmReceiver.AT, r.at.toEpochMilliseconds())
                .putExtra(AlarmReceiver.SILENT, r.silent)
            val pi = PendingIntent.getBroadcast(app, r.id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val t = r.at.toEpochMilliseconds()
            try {
                when {
                    exact && r.kind.isAlarm ->
                        am.setAlarmClock(AlarmManager.AlarmClockInfo(t, Notifications.openApp(app)), pi)
                    exact -> am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
                    else -> am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
                }
            } catch (e: SecurityException) {
                // The permission was taken away between the check and the call.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
            }
        }
        val next = reminders.firstOrNull { it.kind.isAlarm || it.prayer != null }?.let { r ->
            val what = r.prayer?.let { prayerName(it) } ?: r.title
            "$what ${clockText(r.at, zone)}"
        }
        prefs.edit()
            .putStringSet(IDS, reminders.map { it.id.toString() }.toSet())
            .putString(NEXT, next)
            .apply()
    }

    fun scheduledCount(context: Context) = prefs(context).getStringSet(IDS, emptySet()).orEmpty().size
    fun next(context: Context): String? = prefs(context).getString(NEXT, null)
}
