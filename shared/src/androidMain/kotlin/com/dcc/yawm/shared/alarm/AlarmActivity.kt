package com.dcc.yawm.shared.alarm

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.AndroidPlatform
import com.dcc.yawm.shared.ui.DarkPalette
import com.dcc.yawm.shared.ui.InkWord
import com.dcc.yawm.shared.ui.LocalFonts
import com.dcc.yawm.shared.ui.LocalPalette
import com.dcc.yawm.shared.ui.LocalReducedMotion
import com.dcc.yawm.shared.ui.Num
import com.dcc.yawm.shared.ui.Txt
import com.dcc.yawm.shared.ui.WideAction
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The alarm screen: shows over the lock screen and turns the screen on. The sound comes from the
 * notification itself (insistent, on the alarm channel), so the alarm rings even when the phone
 * does not let this screen open; closing either one stops it.
 */
class AlarmActivity : ComponentActivity() {
    @OptIn(ExperimentalTime::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        current = this
        val extras = Intent(intent)
        val title = intent.getStringExtra(AlarmReceiver.TITLE).orEmpty().ifBlank { "منبّه" }
        val atMs = intent.getLongExtra(AlarmReceiver.AT, System.currentTimeMillis())
        val time = clockText(Instant.fromEpochMilliseconds(atMs), TimeZone.currentSystemDefault())
        val platform = AndroidPlatform(this)
        platform.systemBars(true)
        setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalFonts provides platform.fonts,
                LocalReducedMotion provides platform.reducedMotion,
                LocalPalette provides DarkPalette,
            ) {
                Column(
                    Modifier.fillMaxSize().background(DarkPalette.paper).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    InkWord("اصحى", 1f, 150.dp, "$title، الساعة $time")
                    Num(time, size = 64.sp, color = DarkPalette.ink)
                    Txt(title, size = 20.sp, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WideAction("hourglass", "بعد 5 دقايق", modifier = Modifier.weight(1f)) {
                            ActionReceiver.snooze(this@AlarmActivity, extras)
                            close(extras)
                        }
                        WideAction("seal", "صحيت", primary = true, modifier = Modifier.weight(1f)) { close(extras) }
                    }
                }
            }
        }
    }

    private fun close(extras: Intent) {
        getSystemService(NotificationManager::class.java).cancel(extras.getIntExtra(AlarmReceiver.ID, 0))
        finish()
    }

    override fun onDestroy() {
        if (current === this) current = null
        super.onDestroy()
    }

    companion object {
        private var current: AlarmActivity? = null

        /** «صحيت» from the notification: the screen closes too. */
        fun stopRinging() {
            current?.finish()
        }
    }
}
