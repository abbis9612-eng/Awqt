package com.dcc.yawm.shared

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dcc.yawm.shared.alarm.AndroidDeviceControls
import com.dcc.yawm.shared.data.AndroidDatabase
import com.dcc.yawm.shared.ui.App
import java.time.LocalDate

/** The Android entry point calls this, so `:androidApp` needs no Compose compiler of its own. */
fun ComponentActivity.showYawm() {
    val today = LocalDate.now()
    val data = AndroidDatabase.get(this)
    val device = AndroidDeviceControls(this)
    // Coming back from a settings screen may have changed a permission: read it again and
    // set the next 48 hours of alarms (also covers the first launch after install).
    lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            device.refresh()
            device.reschedule()
        }
    })
    setContent { App(SimpleDate(today.year, today.monthValue, today.dayOfMonth), data, device) }
}
