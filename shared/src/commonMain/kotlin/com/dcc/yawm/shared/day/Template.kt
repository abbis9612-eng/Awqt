package com.dcc.yawm.shared.day

/*
 * The adaptive day template (docs/METHOD.md section 3), ported from workday_template() and
 * friday_template() in docs/reference/resolver_reference.py. Titles are the reference's own words.
 */

enum class Anchor { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA, BEDTIME }

enum class Role { WORSHIP, BODY, SPOUSE, CHILD, BUSINESS, DEV, LEARNING, SELF }

/** OFFDAY is a day off chosen by the user (not in the reference; see Customization.kt). */
enum class DayType { WORKDAY, FRIDAY, OFFDAY }

data class BlockSpec(
    val key: String,
    val title: String,
    val role: Role,
    /** Ignored for the flex block. */
    val preferredMinutes: Int,
    val minMinutes: Int,
    /** 1 = most important; larger numbers shrink and drop first. */
    val priority: Int,
    val droppable: Boolean = true,
    /** Takes the rest of its segment; at most one per segment. */
    val flex: Boolean = false,
    val isDeepWork: Boolean = false,
    val isPrayer: Boolean = false,
)

data class SegmentSpec(val from: Anchor, val to: Anchor, val blocks: List<BlockSpec>)

object Templates {
    fun forDay(type: DayType, napMinutes: Int): List<SegmentSpec> = when (type) {
        DayType.WORKDAY -> workday(napMinutes)
        DayType.FRIDAY -> friday(napMinutes)
        DayType.OFFDAY -> offday(napMinutes)
    }

    fun workday(napMinutes: Int): List<SegmentSpec> = listOf(
        SegmentSpec(Anchor.FAJR, Anchor.SUNRISE, listOf(
            BlockSpec("fajr", "صلاة الفجر جماعة", Role.WORSHIP, 25, 15, 1, droppable = false, isPrayer = true),
            BlockSpec("ghadwa", "ورد الغدوة: أذكار وقرآن، بلا تلفون", Role.WORSHIP, 0, 15, 2, droppable = false, flex = true),
        )),
        SegmentSpec(Anchor.SUNRISE, Anchor.DHUHR, listOf(
            BlockSpec("light", "ضوء الشمس ومشي وفطور", Role.BODY, 25, 10, 3),
            BlockSpec("deep1", "تركيز: أصعب مهمة", Role.DEV, 90, 45, 2, droppable = false, isDeepWork = true),
            BlockSpec("duha", "استراحة وركعتا الضحى", Role.WORSHIP, 20, 10, 4),
            BlockSpec("deep2", "تركيز: الكتلة الثانية", Role.DEV, 90, 45, 5, isDeepWork = true),
            BlockSpec("msgs1", "نافذة الرسائل", Role.BUSINESS, 20, 10, 4),
            BlockSpec("work1", "العمل: اتصالات ومتابعة", Role.BUSINESS, 0, 0, 6, flex = true),
        )),
        SegmentSpec(Anchor.DHUHR, Anchor.ASR, listOf(
            BlockSpec("dhuhr", "صلاة الظهر وغداء خفيف", Role.WORSHIP, 40, 25, 1, droppable = false, isPrayer = true),
            BlockSpec("nap", "قيلولة $napMinutes دقيقة", Role.BODY, napMinutes, SHORT_NAP, 2, droppable = false),
            BlockSpec("work2", "العمل: ميدان واجتماعات", Role.BUSINESS, 0, 0, 6, flex = true),
        )),
        SegmentSpec(Anchor.ASR, Anchor.MAGHRIB, listOf(
            BlockSpec("asr", "صلاة العصر", Role.WORSHIP, 12, 10, 1, droppable = false, isPrayer = true),
            BlockSpec("train", "تدريب أو تعلم (حسب اليوم)", Role.BODY, 60, 30, 4),
            BlockSpec("close", "الرسائل وإغلاق يوم العمل", Role.BUSINESS, 20, 10, 5),
            BlockSpec("child", "وقت الابن وأذكار المساء", Role.CHILD, 0, 30, 2, droppable = false, flex = true),
        )),
        SegmentSpec(Anchor.MAGHRIB, Anchor.ISHA, listOf(
            BlockSpec("maghrib", "صلاة المغرب", Role.WORSHIP, 15, 10, 1, droppable = false, isPrayer = true),
            BlockSpec("family", "الأسرة: عشاء وحديث مع الزوجة", Role.SPOUSE, 0, 30, 2, droppable = false, flex = true),
        )),
        SegmentSpec(Anchor.ISHA, Anchor.BEDTIME, listOf(
            BlockSpec("isha", "صلاة العشاء", Role.WORSHIP, 15, 10, 1, droppable = false, isPrayer = true),
            BlockSpec("self", "وقت لك: قراءة أو تعلم خفيف", Role.SELF, 0, 0, 6, flex = true),
            BlockSpec("prep", "الاستعداد للنوم: محاسبة وترتيب الغد", Role.SELF, 30, 15, 3),
            BlockSpec("witr", "الوتر وأذكار النوم", Role.WORSHIP, 15, 10, 1, droppable = false),
        )),
    )

    fun friday(napMinutes: Int): List<SegmentSpec> {
        val t = workday(napMinutes).toMutableList()
        t[1] = SegmentSpec(Anchor.SUNRISE, Anchor.DHUHR, listOf(
            BlockSpec("light", "ضوء الشمس ومشي وفطور مع الأهل", Role.BODY, 40, 15, 3),
            BlockSpec("self_am", "وقت لك أو للأسرة", Role.SELF, 0, 0, 6, flex = true),
            BlockSpec("jumua_prep", "التبكير للجمعة: غُسل وسورة الكهف", Role.WORSHIP, 60, 30, 1, droppable = false),
        ))
        t[2] = SegmentSpec(Anchor.DHUHR, Anchor.ASR, listOf(
            BlockSpec("jumua", "الجمعة: الخطبة والصلاة", Role.WORSHIP, 50, 40, 1, droppable = false, isPrayer = true),
            BlockSpec("lunch", "غداء مع الأسرة", Role.SPOUSE, 40, 20, 3),
            BlockSpec("nap", "قيلولة $napMinutes دقيقة", Role.BODY, napMinutes, SHORT_NAP, 2, droppable = false),
            BlockSpec("visit", "صلة الرحم ونزهة الأبناء", Role.CHILD, 0, 0, 6, flex = true),
        ))
        t[3] = SegmentSpec(Anchor.ASR, Anchor.MAGHRIB, listOf(
            BlockSpec("asr", "صلاة العصر", Role.WORSHIP, 12, 10, 1, droppable = false, isPrayer = true),
            BlockSpec("free", "فراغ مباح مع الأهل", Role.SELF, 0, 0, 6, flex = true),
            BlockSpec("dua", "بلا تلفون: دعاء آخر ساعة من الجمعة", Role.WORSHIP, 60, 30, 2, droppable = false),
        ))
        return t
    }

    /**
     * A day off the user chose (owner's decision 2026-10-27): the Friday template's own family
     * blocks, with Dhuhr in place of Jumu'ah and without the Friday-only worship blocks.
     */
    fun offday(napMinutes: Int): List<SegmentSpec> {
        val t = workday(napMinutes).toMutableList()
        val f = friday(napMinutes)
        t[1] = SegmentSpec(Anchor.SUNRISE, Anchor.DHUHR, f[1].blocks.filter { it.key != "jumua_prep" })
        t[2] = SegmentSpec(Anchor.DHUHR, Anchor.ASR, listOf(
            BlockSpec("dhuhr", "صلاة الظهر", Role.WORSHIP, 15, 10, 1, droppable = false, isPrayer = true),
        ) + f[2].blocks.drop(1))
        t[3] = SegmentSpec(Anchor.ASR, Anchor.MAGHRIB, listOf(
            t[3].blocks.first { it.key == "asr" },
            t[3].blocks.first { it.key == "train" },
            f[3].blocks.first { it.key == "free" },
        ))
        return t
    }
}
