package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.SimpleDate
import com.dcc.yawm.shared.day.arabicDate

// Step 1 test screen only. The Ink identity (docs/design/IDENTITY.md) arrives in step 7.
private val Paper = Color(0xFFFFFFFF)
private val Ink = Color(0xFF121212)
private val Grey = Color(0xFF77736C)

@Composable
fun App(today: SimpleDate) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxSize().background(Paper).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = "يوم",
                style = TextStyle(color = Ink, fontSize = 64.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            )
            BasicText(
                text = arabicDate(today),
                style = TextStyle(color = Grey, fontSize = 20.sp, textAlign = TextAlign.Center),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
