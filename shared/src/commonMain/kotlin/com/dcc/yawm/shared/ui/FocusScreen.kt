package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Focus: the word «تركيز» fills as the time passes; other alerts stay silent until it ends. */
@OptIn(ExperimentalTime::class)
@Composable
fun FocusScreen(title: String, fromMs: Long, untilMs: Long, onStop: () -> Unit, onMore: () -> Unit) {
    val pal = LocalPalette.current
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(untilMs) {
        while (true) {
            now = Clock.System.now().toEpochMilliseconds()
            if (now >= untilMs) { onStop(); break }
            delay(1000)
        }
    }
    val left = (untilMs - now).coerceAtLeast(0)
    val total = (untilMs - fromMs).coerceAtLeast(1)
    val mm = left / 60_000
    val ss = (left / 1000) % 60
    Column(
        Modifier.fillMaxSize().background(pal.paper)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Txt(title, size = 16.sp, color = pal.grey, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        InkWord("تركيز", 1f - left.toFloat() / total, 138.dp, "تركيز، باقي $mm دقيقة")
        Num("${mm.toString().padStart(2, '0')}:${ss.toString().padStart(2, '0')}", size = 64.sp, color = pal.ink)
        Txt("التنبيهات الثانية ساكتة لحد ما تخلص، إلا المنبّه والصلاة", size = 16.sp, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WideAction("hourglass", "+5 دقايق", modifier = Modifier.weight(1f), onClick = onMore)
            WideAction("seal", "وقّف", primary = true, modifier = Modifier.weight(1f), onClick = onStop)
        }
    }
}
