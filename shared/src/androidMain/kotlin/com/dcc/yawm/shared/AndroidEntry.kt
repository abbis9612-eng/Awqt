package com.dcc.yawm.shared

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dcc.yawm.shared.alarm.AndroidDeviceControls
import com.dcc.yawm.shared.data.AndroidDatabase
import com.dcc.yawm.shared.ui.App

/** The Android entry point calls this, so `:androidApp` needs no Compose compiler of its own. */
fun ComponentActivity.showYawm() {
    enableEdgeToEdge()
    // «الألوان»: the field's colour runs behind the navigation bar too, without the system's white veil.
    if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
    val data = AndroidDatabase.get(this)
    val device = AndroidDeviceControls(this)
    val platform = AndroidPlatform(this)
    // Coming back from a settings screen may have changed a permission: read it again and
    // set the next 48 hours of alarms (also covers the first launch after install).
    lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            device.refresh()
            device.reschedule()
        }
    })
    setContent { App(data, device, platform) }
}
