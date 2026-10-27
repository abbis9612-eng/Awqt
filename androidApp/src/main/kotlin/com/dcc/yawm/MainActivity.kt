package com.dcc.yawm

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.dcc.yawm.shared.showYawm

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showYawm()
    }
}
