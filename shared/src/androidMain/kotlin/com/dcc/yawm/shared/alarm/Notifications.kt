package com.dcc.yawm.shared.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager

internal object Notifications {
    const val ADHAN = "adhan"
    const val BLOCKS = "blocks"
    const val WAKE = "wake"

    /** Channel settings belong to the user after creation; creating again changes only the name. */
    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(ADHAN, "الأذان", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "عند دخول وقت كل صلاة. تكدر تختار الصوت من هنا."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(BLOCKS, "بداية الفقرات", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "تذكير خفيف لما تبدي كل فقرة من اليوم."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(WAKE, "الكعدة قبل الفجر", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "منبّه الكعدة للسَّحَر أو قبل الفجر."
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
                )
            },
        )
    }

    fun openApp(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().setPackage(context.packageName)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun show(context: Context, kind: ReminderKind, id: Int, title: String, body: String, atMs: Long) {
        ensureChannels(context)
        val channel = when (kind) {
            ReminderKind.ADHAN -> ADHAN
            ReminderKind.BLOCK -> BLOCKS
            ReminderKind.WAKE -> WAKE
        }
        val n = Notification.Builder(context, channel)
            .setSmallIcon(if (kind == ReminderKind.BLOCK) android.R.drawable.ic_popup_reminder else android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setWhen(atMs)
            .setShowWhen(true)
            .setCategory(if (kind == ReminderKind.BLOCK) Notification.CATEGORY_REMINDER else Notification.CATEGORY_ALARM)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, n)
    }
}
