package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.screen.Field
import com.dcc.yawm.shared.screen.PartsView
import com.dcc.yawm.shared.screen.Spectrum
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.dateLine
import com.dcc.yawm.shared.screen.fieldLine
import com.dcc.yawm.shared.screen.fieldName
import com.dcc.yawm.shared.screen.partOf

/*
 * «الألوان», step 1 (docs/design/yawm-colors.html): today's home. The screen is the colour of the
 * chosen field (the part you are in, unless you tap another on the spectrum); its big name in Lalezar,
 * one sentence, the prayer first, the things, «أضف»; the spectrum of the whole day at the bottom.
 * The field's palette is provided to the shared rows, so ThingRow, Check and the switch take its colours.
 */

@Composable
fun ColorsHome(
    view: PartsView,
    spec: Spectrum,
    t: Int,
    field: Field,
    prayed: Set<String>,
    hijri: String,
    wakeOn: Boolean,
    onSelect: (Field) -> Unit,
    onTick: (Thing) -> Unit,
    onOpen: (Thing) -> Unit,
    onPray: (String) -> Unit,
    onAdd: (DayPart?) -> Unit,
    onWake: (Boolean) -> Unit,
    openSettings: () -> Unit,
    openAlarms: () -> Unit,
    /** Another day than today: no «now» on it, and a way back to today. */
    backToToday: (() -> Unit)? = null,
) {
    val isToday = backToToday == null
    val fonts = LocalFonts.current
    val pal = fieldPalette(field)
    Fielded(field) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Txt("${dateLine(view.date)}، $hijri", size = 15.sp, color = pal.ink, modifier = Modifier.weight(1f), lineHeight = 1.4f)
                    if (backToToday != null) PillButton("رجوع لليوم", backToToday)
                    else Box(
                        Modifier.heightIn(min = 48.dp).clickable(onClick = openSettings).semantics { role = Role.Button }.padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) { Txt("الإعدادات", size = 15.sp, bold = true, lineHeight = 1.3f) }
                }
                BasicText(
                    fieldName(field).joinToString("\n"),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = fieldName(field).joinToString(" ") },
                    style = TextStyle(color = pal.ink, fontFamily = fonts.poster, fontSize = if (fieldName(field).size == 1) 96.sp else 86.sp, lineHeight = 90.sp),
                )
                Txt(fieldLine(view, spec, field, t), size = 20.sp, bold = true, lineHeight = 1.45f)

                if (field == Field.NIGHT) {
                    CardGroup {
                        ListRow(divider = false, minHeight = 64.dp) {
                            RowText("منبّه الصحيان ${clock(view.wake)}", "يتحرك ويّا الفجر كل يوم", bold = true)
                            InkSwitch(wakeOn, "منبّه الصحيان", onWake)
                        }
                    }
                    AddCard("المنبّهات", openAlarms)
                } else {
                    val p = view.partOf(field)!!
                    val key = view.prayerKey(p.part)
                    val name = view.prayerName(p.part)
                    val s = p.span
                    if (key != null && name != null) PrayCard(name, s.start, s.start <= t, key in prayed) { onPray(key) }
                    for (x in p.things) ThingCard(x, view.date, onTick, onOpen)
                    if (p.things.isEmpty() && s.end > t) Txt("ماكو شي بهالقسم. وقتك فاضي.", size = 16.sp, color = pal.grey)
                    AddCard("أضف ل" + p.part.label.let { if (it.startsWith("ال")) "ل" + it.drop(1) else it }) { onAdd(p.part) }
                }
                // What is not tied to a part stays reachable on the field of now.
                if (isToday && field == spec.fieldAt(t)) {
                    if (view.tomorrow.isNotEmpty()) {
                        FieldHeading("باچر")
                        for (x in view.tomorrow) ThingCard(x, view.date, onTick, onOpen)
                    }
                    if (view.later.isNotEmpty()) {
                        FieldHeading("بعدين")
                        for (x in view.later) ThingCard(x, view.date, onTick, onOpen)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            SpectrumBar(spec, t, field, isToday, onSelect, Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 14.dp))
        }
    }
}

/**
 * The whole day as one bar: each field at its real length (the night is about a third), the past
 * dimmed, a white needle at now, the chosen field outlined. A tap anywhere picks the field under it;
 * each segment also carries its own accessibility action, since some are narrower than a finger.
 */
@Composable
private fun SpectrumBar(spec: Spectrum, t: Int, selected: Field, showNow: Boolean, onSelect: (Field) -> Unit, modifier: Modifier = Modifier) {
    val fg = FieldColour.getValue(selected).fg
    val now = spec.fieldAt(t)
    BoxWithConstraints(
        modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(20.dp)).border(2.dp, fg.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .pointerInput(spec) {
                detectTapGestures { pos ->
                    // In right-to-left the bar starts at the right edge.
                    onSelect(spec.fieldAtFraction(((size.width - pos.x) / size.width).coerceIn(0f, 0.9999f)))
                }
            },
    ) {
        val w = maxWidth
        Row(Modifier.fillMaxSize()) {
            for (s in spec.segments) {
                val c = FieldColour.getValue(s.field)
                val past = showNow && s.field != Field.NIGHT && s.field != now && s.end <= t
                val isSel = s.field == selected
                Box(
                    Modifier.weight(spec.fraction(s).coerceAtLeast(0.001f)).fillMaxHeight().background(c.bg)
                        .then(if (isSel) Modifier.border(3.dp, Color.White).padding(3.dp).border(1.5.dp, Color(0xFF111111)) else Modifier)
                        .semantics {
                            role = Role.Tab; this.selected = isSel; contentDescription = fieldName(s.field).joinToString(" ")
                            onClick { onSelect(s.field); true }
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (past) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
                    if (spec.fraction(s) > 0.09f) Txt(s.field.short, size = 13.sp, bold = true, color = c.fg, modifier = Modifier.padding(bottom = 6.dp), lineHeight = 1.2f)
                }
            }
        }
        if (showNow) Box(Modifier.offset(x = w * spec.needle(t) - 1.5.dp).width(3.dp).fillMaxHeight().background(Color.White).border(1.dp, Color(0xFF111111)))
    }
}
