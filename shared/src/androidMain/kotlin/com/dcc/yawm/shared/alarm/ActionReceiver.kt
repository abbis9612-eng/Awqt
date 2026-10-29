package com.dcc.yawm.shared.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dcc.yawm.shared.data.AndroidDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** The buttons on a notification: done, five more minutes, tomorrow, and «صحيت». */
class ActionReceiver : BroadcastReceiver() {
    @OptIn(ExperimentalTime::class)
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val id = intent.getIntExtra(AlarmReceiver.ID, 0)
        app.getSystemService(NotificationManager::class.java).cancel(id)
        when (intent.getStringExtra(WHAT)) {
            SNOOZE -> snooze(app, intent)
            DISMISS -> AlarmActivity.stopRinging()
            DONE, TOMORROW -> {
                val pending = goAsync()
                scope.launch {
                    try {
                        val data = AndroidDatabase.get(app)
                        val s = data.settings.load()
                        val zone = runCatching { TimeZone.of(s.zoneId) }.getOrElse { TimeZone.currentSystemDefault() }
                        val at = Instant.fromEpochMilliseconds(intent.getLongExtra(AlarmReceiver.AT, System.currentTimeMillis()))
                        val date = at.toLocalDateTime(zone).date
                        val key = intent.getStringExtra(AlarmReceiver.KEY).orEmpty()
                        if (key.startsWith("task:")) {
                            val taskId = key.removePrefix("task:")
                            val tasks = s.tasks.map { t ->
                                if (t.id != taskId) t
                                else if (intent.getStringExtra(WHAT) == TOMORROW) t.copy(day = date.plus(1, DateTimeUnit.DAY), pinAt = null)
                                else t.copy(doneOn = date)
                            }
                            data.settings.save(s.copy(tasks = tasks))
                            AlarmScheduler.reschedule(app)
                        } else if (key.isNotEmpty()) {
                            data.dayLog.setBlockDone(date, blockKey(key, date), true)
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun blockKey(key: String, date: LocalDate): String = when (key) {
        "FAJR" -> "fajr"
        "DHUHR" -> if (date.dayOfWeek == DayOfWeek.FRIDAY) "jumua" else "dhuhr"
        "ASR" -> "asr"
        "MAGHRIB" -> "maghrib"
        "ISHA" -> "isha"
        else -> key
    }

    companion object {
        const val WHAT = "what"
        const val DONE = "done"
        const val SNOOZE = "snooze"
        const val TOMORROW = "tomorrow"
        const val DISMISS = "dismiss"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** Rings the same notification again in five minutes. */
        fun snooze(context: Context, extras: Intent) {
            val am = context.getSystemService(AlarmManager::class.java)
            val at = System.currentTimeMillis() + 5 * 60_000L
            val again = Intent(extras).setClass(context, AlarmReceiver::class.java)
                .putExtra(AlarmReceiver.AT, at).putExtra(AlarmReceiver.SILENT, false)
            again.removeExtra(WHAT)
            val pi = PendingIntent.getBroadcast(context, extras.getIntExtra(AlarmReceiver.ID, 0) xor 0x5A5A, again, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            try {
                if (AlarmScheduler.canScheduleExact(am)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        }
    }
}
