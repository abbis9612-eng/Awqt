package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.alarm.UserAlarm
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.PartSpan
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.screen.Ask
import com.dcc.yawm.shared.screen.Where
import com.dcc.yawm.shared.screen.card
import com.dcc.yawm.shared.screen.understand

/*
 * «المساعد» (owner, 2026-11-06; preview docs/design/yawm-assistant.html): say or type one sentence, see a
 * card with what was understood, and nothing happens until «إي». It knows only «يوم»'s own things and runs
 * on the phone; the voice uses the phone's on-device recognizer only.
 */

private sealed interface Line {
    data class Me(val text: String) : Line
    data class It(val text: String) : Line
    /** [state]: 0 waiting for an answer, 1 done, 2 not done. */
    data class Card(val ask: Ask, val state: Int = 0) : Line
}

private val Tries = listOf(
    "صحّيني باچر خمسة ونص", "ذكّرني أتصل بأمي بعد المغرب كل يوم", "خلّي الرياضة الساعة ستة",
    "أخّر الرياضة نص ساعة", "قراءة بعد ساعة", "شنو عندي اليوم",
)

@Composable
private fun AText(text: String, size: TextUnit, color: Color, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    BasicText(text, modifier, style = TextStyle(color = color, fontSize = size, fontWeight = weight, fontFamily = LocalFonts.current.ui, textAlign = align, lineHeight = size * 1.5f))
}

/** The colour of a request's card: its part's accent, or the quiet slate. */
private fun accentOf(a: Ask): Color = when (a) {
    is Ask.AddThing -> (a.draft.where as? Where.In)?.part?.let { PartAccent.getValue(it) } ?: NeutralAccent
    is Ask.DeleteThing, is Ask.DeleteAlarm -> PartAccent.getValue(DayPart.MAGHRIB)
    else -> NeutralAccent
}

@Composable
fun BoxScope.AssistantSheet(
    things: List<Thing>, alarms: List<UserAlarm>, spans: List<PartSpan>, nowMinute: Int, defaultPart: DayPart,
    platform: Platform, close: () -> Unit, onDo: (Ask) -> Unit,
) {
    val lines = remember { mutableStateListOf<Line>(Line.It("هلا. گول أو اكتب شنو تريد بجملة وحدة، وآني أرتّبها. ما أسوي شي إلا لما تگول «إي».")) }
    var input by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { platform.stopListening() } }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        lines.add(Line.Me(t))
        when (val a = understand(t, things, alarms, spans, nowMinute, defaultPart)) {
            is Ask.Tell -> lines.add(Line.It(a.text))
            else -> lines.add(Line.Card(a))
        }
        input = ""
    }

    CalmSheet("المساعد", close) {
        val c = LocalCalm.current
        // Header.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(PartAccent.getValue(DayPart.MORNING), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                AText("✦", 18.sp, Color.White, FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                AText("المساعد", 17.sp, c.ink, FontWeight.Bold)
                AText("يفهم شغلات «يوم» بس · كلشي بتلفونك", 12.5.sp, c.ink3)
            }
            Box(Modifier.heightIn(min = 48.dp).clickable(onClick = close).semantics { role = Role.Button }.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                AText("سكّر", 15.sp, c.ink2, FontWeight.SemiBold)
            }
        }

        // The conversation.
        Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (line in lines) when (line) {
                is Line.Me -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Box(Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(18.dp)).background(c.ink).padding(horizontal = 14.dp, vertical = 9.dp)) {
                        AText(line.text, 15.5.sp, c.surface)
                    }
                }
                is Line.It -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(Modifier.widthIn(max = 320.dp).shadow(if (c.dark) 0.dp else 4.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(c.card).padding(horizontal = 14.dp, vertical = 9.dp)) {
                        AText(line.text, 15.sp, c.ink2)
                    }
                }
                is Line.Card -> AskCardView(line, spans) { yes ->
                    if (yes) onDo(line.ask)
                    val i = lines.indexOfFirst { it === line }
                    if (i >= 0) lines[i] = line.copy(state = if (yes) 1 else 2)
                }
            }
            if (listening) AText("أسمعك…", 14.sp, c.ink2, FontWeight.SemiBold, Modifier.fillMaxWidth(), TextAlign.Center)
        }

        // Ready sentences to try.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (t in Tries) Box(
                Modifier.heightIn(min = 40.dp).shadow(if (c.dark) 0.dp else 3.dp, CircleShape).clip(CircleShape).background(c.card)
                    .clickable { send(t) }.semantics { role = Role.Button }.padding(horizontal = 13.dp),
                contentAlignment = Alignment.Center,
            ) { AText(t, 14.sp, c.ink2, FontWeight.Medium) }
        }

        // Say or type.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(if (listening) Color(0xFFC62E2E) else c.soft)
                    .clickable {
                        if (listening) { platform.stopListening(); listening = false }
                        else platform.listen { ev ->
                            when (ev) {
                                Heard.Listening -> listening = true
                                is Heard.Partial -> input = ev.text
                                is Heard.Final -> { listening = false; send(ev.text) }
                                is Heard.Problem -> { listening = false; lines.add(Line.It(ev.text)) }
                            }
                        }
                    }
                    .semantics { role = Role.Button; contentDescription = "احچي"; stateDescription = if (listening) "يسمع" else "ما يسمع" },
                contentAlignment = Alignment.Center,
            ) { MicGlyph(if (listening) Color.White else c.ink) }
            Box(
                Modifier.weight(1f).heightIn(min = 52.dp).shadow(if (c.dark) 0.dp else 4.dp, CircleShape).clip(CircleShape).background(c.card).padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (input.isEmpty()) AText("گول أو اكتب شنو تريد…", 16.sp, c.ink3)
                BasicTextField(
                    input, { input = it.take(120) }, singleLine = true,
                    textStyle = TextStyle(color = c.ink, fontSize = 16.sp, fontFamily = LocalFonts.current.ui),
                    cursorBrush = SolidColor(c.ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(input) }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "شنو تريد؟" },
                )
            }
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(PartAccent.getValue(DayPart.MORNING)).clickable { send(input) }
                    .semantics { role = Role.Button; contentDescription = "دز" },
                contentAlignment = Alignment.Center,
            ) { AText("←", 22.sp, Color.White, FontWeight.Bold) }
        }
        AText("الصوت يشتغل بس إذا تلفونك يفهم العربي بدون إنترنت. إذا لا، اكتبها وتشتغل نفس الشي.", 12.5.sp, c.ink3)
    }
}

@Composable
private fun AskCardView(line: Line.Card, spans: List<PartSpan>, answer: (Boolean) -> Unit) {
    val c = LocalCalm.current
    val card = line.ask.card(spans) ?: return
    val acc = accentOf(line.ask)
    Column(
        Modifier.fillMaxWidth().shadow(if (c.dark) 0.dp else 6.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(c.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(acc, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { AText("✦", 15.sp, Color.White, FontWeight.Bold) }
            Column {
                AText("فهمت، تريد:", 12.5.sp, c.ink3, FontWeight.SemiBold)
                AText(card.head, 16.sp, c.ink, FontWeight.Bold)
            }
        }
        for ((k, v) in card.facts) Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            AText(k, 14.5.sp, c.ink3)
            AText(v, 14.5.sp, c.ink, FontWeight.SemiBold)
        }
        when (line.state) {
            0 -> Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.weight(2f).heightIn(min = 48.dp).clip(RoundedCornerShape(13.dp)).background(acc).clickable { answer(true) }.semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) { AText(card.yes, 15.sp, Color.White, FontWeight.Bold) }
                Box(
                    Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(13.dp)).background(c.soft).clickable { answer(false) }.semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) { AText("لا، غيّر", 15.sp, c.ink2, FontWeight.Bold) }
            }
            1 -> AText("✓ ${card.done}", 14.sp, acc, FontWeight.Bold)
            else -> AText("تمام، ما سوّيت شي. گولها بطريقة ثانية.", 14.sp, c.ink2, FontWeight.SemiBold)
        }
    }
}

/** A microphone, drawn (the app's icon set has none). */
@Composable
private fun MicGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.09f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * 0.36f, w * 0.08f), size = androidx.compose.ui.geometry.Size(w * 0.28f, w * 0.5f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f), style = stroke)
        drawArc(color, 0f, 180f, false, topLeft = androidx.compose.ui.geometry.Offset(w * 0.22f, w * 0.3f), size = androidx.compose.ui.geometry.Size(w * 0.56f, w * 0.48f), style = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.5f, w * 0.78f), androidx.compose.ui.geometry.Offset(w * 0.5f, w * 0.92f), strokeWidth = w * 0.09f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}
