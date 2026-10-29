package com.dcc.yawm.shared.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** An alarm rang: show it, then set the next 48 hours again so the chain never runs dry. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getStringExtra(KIND) != null) Notifications.show(context, intent)
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
        const val STYLE = "style"
        const val KEY = "key"
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
