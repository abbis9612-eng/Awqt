package com.dcc.yawm.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.alarm.ReminderKind
import com.dcc.yawm.shared.alarm.clockText
import com.dcc.yawm.shared.alarm.upcomingReminders
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.screen.DayView
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.zoneOf
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Each kind's sound: original, made from plain tones by tools/sounds.py (no rights to clear). */
enum class YawmSound(val file: String, val label: String, val use: String) {
    DROP("yawm_drop", "قطرة", "تبدي فقرة، وملخص الصبح"),
    TAP("yawm_tap", "نقرتين", "قبل الفقرة بـ5 أو 10 دقايق"),
    SEAL("yawm_seal", "ختم", "تخلص فقرة"),
    PAPER("yawm_paper", "ورقة", "وقت مهمة"),
    BELL("yawm_bell", "جرس هادي", "تنبيه الصلاة"),
    BREATH("yawm_breath", "نَفَس", "خلص التركيز"),
    WAKE("yawm_wake", "صحيان", "المنبّه والصحيان، يعلى شوية شوية"),
}

fun soundOf(kind: ReminderKind): YawmSound = when (kind) {
    ReminderKind.ALARM, ReminderKind.WAKE -> YawmSound.WAKE
    ReminderKind.ADHAN, ReminderKind.PRAYER -> YawmSound.BELL
    ReminderKind.START, ReminderKind.SUMMARY, ReminderKind.REVIEW -> YawmSound.DROP
    ReminderKind.BEFORE -> YawmSound.TAP
    ReminderKind.END -> YawmSound.SEAL
    ReminderKind.TASK -> YawmSound.PAPER
}

/** «شكل التنبيهات وأصواتها»: how each looks and sounds, the rules, and today's list after them. */
@OptIn(ExperimentalTime::class)
@Composable
fun SoundsScreen(settings: UserSettings, day: DayView, t: Int, platform: Platform, close: () -> Unit) {
    val pal = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Display("التنبيهات", 32.sp)
        PillButton("رجوع", close)
    }
    Txt("كل نوع إله شكل وصوت ورجفة خاصة بيه، حتى تعرف شنو هو قبل لا تطلّع بالتلفون. اضغط «اسمع».", size = 15.sp, color = pal.grey)

    Heading("شكل كل تنبيه")
    val up = day.items.filter { it.start > t && !it.isPrayer && !it.isSleep && !it.key.startsWith("free:") }
    val a = up.getOrNull(0) ?: day.items.first()
    val b = up.getOrNull(1) ?: a
    val pr = day.items.firstOrNull { it.isPrayer && it.start > t } ?: day.items.first { it.isPrayer }
    NotificationMock("هسه: ${a.title}", clock(a.start), "لحد ${clock(a.end)}", a.icon, YawmSound.DROP.label, { platform.playSound(YawmSound.DROP.file) }, listOf("خلّصت", "بعد 5 دقايق"))
    NotificationMock("بعد 5 دقايق: ${b.title}", clock(b.start - 5), "تبدي ${clock(b.start)}", b.icon, YawmSound.TAP.label, { platform.playSound(YawmSound.TAP.file) }, listOf("تمام"))
    NotificationMock("هسه: ${b.title}", clock(a.end), "وخلص قبلها: ${a.title}. تنبيهين بنفس الدقيقة صاروا واحد.", b.icon, YawmSound.DROP.label, { platform.playSound(YawmSound.DROP.file) }, listOf("خلّصت", "مدّد 10 دقايق"))
    NotificationMock("مهمة: اتصل بالمحاسب", clock(a.start), "15 دقيقة، ضمن «${a.title}»", "seal", YawmSound.PAPER.label, { platform.playSound(YawmSound.PAPER.file) }, listOf("خلّصت", "أجّلها لباچر"))
    NotificationMock("وقت ${pr.title}", clock(pr.start), "الساعة ${clock(pr.start)}", pr.icon, YawmSound.BELL.label, { platform.playSound(YawmSound.BELL.file) }, listOf("صلّيت"))
    NotificationMock("تركيز · باقي 42:10", clock(t), "التنبيهات الثانية ساكتة إلا المنبّه والصلاة", "nib", actions = listOf("وقّف", "+5 دقايق"), silent = true)

    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).background(lerp(pal.paper, pal.ink, 0.04f), RoundedCornerShape(18.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt("شاشة المنبّه", size = 16.sp, bold = true)
            Txt("تطلع على الشاشة كلها حتى لو التلفون مقفول، والصوت يعلى شوية شوية.", size = 13.5.sp, color = pal.grey, lineHeight = 1.6f)
        }
        PillButton("جرّبها") { platform.testAlarm() }
    }

    Heading("الأصوات")
    for (s in YawmSound.entries) {
        ListRow {
            RowText(s.label, s.use)
            PillButton("▶ اسمع") { platform.playSound(s.file) }
        }
    }
    Txt("هاي الأصوات مسوّية إلنا من نغمات بسيطة، فما بيها حقوق لأحد. الأذان ما نصنعه: تختاره من أصوات تلفونك. وتكدر تغيّر صوت أي نوع من إعدادات التلفون.", size = 14.sp, color = pal.grey, modifier = Modifier.padding(top = 8.dp))

    Heading("شلون تمشي سوا")
    Column(Modifier.fillMaxWidth().background(lerp(pal.paper, pal.ink, 0.05f), RoundedCornerShape(16.dp)).padding(14.dp)) {
        for (line in listOf(
            "نفس الدقيقة: إذا صار تنبيهين سوا، يجيك واحد بيه الثنين.",
            "ما يرن ورا بعض: إذا رن شي قبل أقل من دقيقتين، الثاني يبين بلا صوت.",
            "بوقت نومك: ما يرن إلا المنبّه والصحيان.",
            "بالتركيز: يسكت كلشي إلا المنبّهات والصلاة.",
            "المهام: كل مهمة تاخذ مكان بالفراغ وما تتداخل ويّا غيرها.",
        )) Txt(line, size = 15.sp, lineHeight = 1.75f, modifier = Modifier.padding(vertical = 2.dp))
    }

    Heading("تنبيهات اليوم بعد هاي القواعد")
    val zone = zoneOf(settings)
    val list = remember(settings, t) { upcomingReminders(settings, Clock.System.now(), 24) }
    Txt("${list.size} تنبيه بالـ24 ساعة الجاية. ${list.count { it.lines.size > 1 }} منها بيها أكثر من شي، و${list.count { it.silent }} تبين بلا صوت.", size = 14.5.sp, color = pal.grey)
    for (r in list) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(46.dp)) { Num(clockText(r.at, zone), size = 15.sp) }
            Column(Modifier.weight(1f)) {
                Txt(r.title, size = 15.sp, bold = true, lineHeight = 1.5f)
                for (l in r.lines.drop(1)) Txt("+ ${l.title}", size = 13.sp, color = pal.grey, lineHeight = 1.5f)
            }
            Txt(if (r.silent) "بلا صوت · ${r.why}" else soundOf(r.kind).label, size = 12.5.sp, color = pal.grey, modifier = Modifier.width(96.dp))
        }
    }
    Spacer(Modifier.height(24.dp))
}
