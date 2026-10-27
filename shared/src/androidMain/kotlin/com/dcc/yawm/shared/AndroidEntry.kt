package com.dcc.yawm.shared

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dcc.yawm.shared.data.AndroidDatabase
import com.dcc.yawm.shared.ui.App
import java.time.LocalDate

/** The Android entry point calls this, so `:androidApp` needs no Compose compiler of its own. */
fun ComponentActivity.showYawm() {
    val today = LocalDate.now()
    val data = AndroidDatabase.get(this)
    setContent { App(SimpleDate(today.year, today.monthValue, today.dayOfMonth), data) }
}
