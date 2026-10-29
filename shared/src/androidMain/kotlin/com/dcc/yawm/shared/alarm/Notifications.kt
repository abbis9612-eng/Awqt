package com.dcc.yawm.shared.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build

/*
 * One channel per kind, each with the app's own sound (tools/sounds.py) and its own vibration, so
 * the user knows what it is before looking (owner, 2026-10-29). A channel's sound cannot change
 * after it is created (developer.android.com, checked 2026-10-29); the user changes it in the
 * phone's settings. The adhan channel has no sound of ours: the user picks it on the phone.
 */
internal object Notifications {
    const val ADHAN = "adhan"
    const val ALARM = "alarm2"
    const val PRAYER = "bell2"
    const val START = "drop2"
    const val BEFORE = "tap2"
    const val END = "seal2"
    const val TASK = "paper2"
    const val VIBRATE = "vibrate2"
    const val QUIET = "quiet"
    private val legacy = listOf("blocks", "wake", "prayer")

    private fun sound(context: Context, name: String): Uri = Uri.parse("android.resource://${context.packageName}/raw/$name")

    private fun channel(id: String, name: String, about: String, importance: Int, sound: Uri?, alarm: Boolean, vibration: LongArray?) =
        NotificationChannel(id, name, importance).apply {
            description = about
            if (sound == null) setSound(null, null)
            else setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(if (alarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
            if (vibration != null) { enableVibration(true); vibrationPattern = vibration } else enableVibration(false)
        }

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        for (id in legacy) nm.deleteNotificationChannel(id)
        val high = NotificationManager.IMPORTANCE_HIGH
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(ADHAN, "الأذان", high).apply { description = "للصلاة اللي تختار إلها «أذان». الصوت تختاره من هنا." },
                channel(ALARM, "المنبّه والصحيان", "منبّهاتك ومنبّه الصحيان. صوت «صحيان».", high, sound(context, "yawm_wake"), true, longArrayOf(0, 400, 200, 400, 200, 400)),
                channel(PRAYER, "تنبيه الصلاة", "للصلاة اللي تختار إلها «تنبيه». صوت «جرس هادي».", high, sound(context, "yawm_bell"), false, longArrayOf(0, 80, 140, 80)),
                channel(START, "تبدي فقرة", "لما تبدي فقرة، وملخص الصبح ومحاسبة الليل. صوت «قطرة».", high, sound(context, "yawm_drop"), false, longArrayOf(0, 40)),
                channel(BEFORE, "قبل الفقرة", "قبل الفقرة بـ5 أو 10 دقايق. صوت «نقرتين».", high, sound(context, "yawm_tap"), false, longArrayOf(0, 20, 90, 20)),
                channel(END, "تخلص فقرة", "لما تخلص فقرة. صوت «ختم».", NotificationManager.IMPORTANCE_DEFAULT, sound(context, "yawm_seal"), false, longArrayOf(0, 70)),
                channel(TASK, "المهام", "وقت مهمة. صوت «ورقة».", high, sound(context, "yawm_paper"), false, longArrayOf(0, 30, 60, 30)),
                channel(VIBRATE, "هزّة بس", "للي يختار «هزّة» بدل الصوت.", high, null, false, longArrayOf(0, 60, 120, 60)),
                channel(QUIET, "بلا صوت", "تبين بلا صوت: بوقت نومك، وبالتركيز، ولما يرن شي قبل أقل من دقيقتين، أو إذا اخترت «إشعار بس».", NotificationManager.IMPORTANCE_LOW, null, false, null),
            ),
        )
    }

    fun openApp(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().setPackage(context.packageName)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun channelFor(kind: ReminderKind, silent: Boolean, style: AlertStyle): String = when {
        kind.isAlarm -> ALARM
        kind == ReminderKind.ADHAN -> ADHAN
        silent || style == AlertStyle.SILENT -> QUIET
        style == AlertStyle.VIBRATE -> VIBRATE
        kind == ReminderKind.PRAYER -> PRAYER
        kind == ReminderKind.BEFORE -> BEFORE
        kind == ReminderKind.END -> END
        kind == ReminderKind.TASK -> TASK
        else -> START
    }

    private fun action(context: Context, label: String, what: String, extras: Intent, code: Int): Notification.Action {
        val i = Intent(extras).setClass(context, ActionReceiver::class.java).putExtra(ActionReceiver.WHAT, what)
        val pi = PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Action.Builder(Icon.createWithResource(context, android.R.drawable.ic_menu_agenda), label, pi).build()
    }

    /** [extras] is the intent that rang (kind, id, key, title, body, at); actions reuse it. */
    fun show(context: Context, extras: Intent) {
        ensureChannels(context)
        val kind = extras.getStringExtra(AlarmReceiver.KIND)?.let { k -> ReminderKind.entries.firstOrNull { it.name == k } } ?: return
        val id = extras.getIntExtra(AlarmReceiver.ID, 0)
        val title = extras.getStringExtra(AlarmReceiver.TITLE).orEmpty()
        val body = extras.getStringExtra(AlarmReceiver.BODY).orEmpty()
        val atMs = extras.getLongExtra(AlarmReceiver.AT, System.currentTimeMillis())
        val silent = extras.getBooleanExtra(AlarmReceiver.SILENT, false)
        val style = extras.getStringExtra(AlarmReceiver.STYLE)?.let { s -> AlertStyle.entries.firstOrNull { it.name == s } } ?: AlertStyle.SOUND
        val key = extras.getStringExtra(AlarmReceiver.KEY).orEmpty()
        val n = Notification.Builder(context, channelFor(kind, silent, style))
            .setSmallIcon(if (kind.isAlarm || kind == ReminderKind.ADHAN) android.R.drawable.ic_lock_idle_alarm else android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body.lineSequence().firstOrNull().orEmpty())
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setWhen(atMs)
            .setShowWhen(true)
            .setCategory(if (kind.isAlarm) Notification.CATEGORY_ALARM else Notification.CATEGORY_REMINDER)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
        when {
            kind.isAlarm -> {
                val full = Intent(context, AlarmActivity::class.java).putExtras(extras).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                val fpi = PendingIntent.getActivity(context, id, full, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                val nm = context.getSystemService(NotificationManager::class.java)
                // Android 14+: only when the phone allows full-screen alarms for this app; otherwise
                // the alarm still rings on its channel and the screen opens from the notification.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || nm.canUseFullScreenIntent()) n.setFullScreenIntent(fpi, true)
                n.setContentIntent(fpi).setOngoing(true).setAutoCancel(false)
                n.addAction(action(context, "صحيت", ActionReceiver.DISMISS, extras, id * 4 + 1))
                n.addAction(action(context, "بعد 5 دقايق", ActionReceiver.SNOOZE, extras, id * 4 + 2))
            }
            kind == ReminderKind.TASK -> {
                n.addAction(action(context, "خلّصت", ActionReceiver.DONE, extras, id * 4 + 1))
                n.addAction(action(context, "أجّلها لباچر", ActionReceiver.TOMORROW, extras, id * 4 + 2))
            }
            kind == ReminderKind.ADHAN || kind == ReminderKind.PRAYER -> n.addAction(action(context, "صلّيت", ActionReceiver.DONE, extras, id * 4 + 1))
            kind == ReminderKind.START || kind == ReminderKind.BEFORE || kind == ReminderKind.END -> if (key.isNotEmpty()) {
                n.addAction(action(context, "خلّصت", ActionReceiver.DONE, extras, id * 4 + 1))
                n.addAction(action(context, "بعد 5 دقايق", ActionReceiver.SNOOZE, extras, id * 4 + 2))
            }
            else -> {}
        }
        val built = n.build()
        // The alarm repeats its sound until the user answers (the sound rises inside the file).
        if (kind.isAlarm) built.flags = built.flags or Notification.FLAG_INSISTENT
        context.getSystemService(NotificationManager::class.java).notify(id, built)
    }
}
