package com.dcc.yawm.shared.data

import android.content.Context
import androidx.room3.Room

/** One database per process, in the app's private storage (kept until the app is uninstalled). */
internal object AndroidDatabase {
    @Volatile private var data: YawmData? = null

    fun get(context: Context): YawmData = data ?: synchronized(this) {
        data ?: YawmData(open(context.applicationContext)).also { data = it }
    }

    private fun open(app: Context): YawmDatabase =
        Room.databaseBuilder<YawmDatabase>(
            context = app,
            name = app.getDatabasePath(DATABASE_FILE).absolutePath,
        ).buildYawmDatabase()
}
