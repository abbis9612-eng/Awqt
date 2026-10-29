package com.dcc.yawm.shared.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** An alarm rang: show it, then set the next 48 hours again so the chain never runs dry. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(KIND)?.let { k -> ReminderKind.entries.firstOrNull { it.name == k } }
        if (kind != null) {
            Notifications.show(
                context, kind, intent.getIntExtra(ID, 0),
                intent.getStringExtra(TITLE).orEmpty(), intent.getStringExtra(BODY).orEmpty(),
                intent.getLongExtra(AT, System.currentTimeMillis()),
                intent.getBooleanExtra(SILENT, false),
            )
        }
        val pending = goAsync()
        AlarmScheduler.rescheduleAsync(context) { pending.finish() }
    }

    companion object {
        const val KIND = "kind"
        const val ID = "id"
        const val TITLE = "title"
        const val BODY = "body"
        const val AT = "at"
        const val SILENT = "silent"
    }
}

/**
 * Alarms vanish on reboot and must follow clock, zone and permission changes
 * (CLAUDE.md notifications). The manifest lists the broadcasts.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        AlarmScheduler.rescheduleAsync(context) { pending.finish() }
    }
}
