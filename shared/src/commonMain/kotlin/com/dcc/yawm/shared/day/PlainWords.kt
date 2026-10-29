package com.dcc.yawm.shared.day

/*
 * Plain words on screen (owner, 2026-10-29: «السحر ما أدري شنو كاتب»). The template keeps the
 * reference's titles (rule 7); everything the user sees or hears goes through here. Only words
 * change, never keys, lengths or times. «شنو هاي؟» lines describe the app's plan from METHOD.md;
 * they are not religious texts or rulings.
 */

private val Plain = mapOf(
    "qiyam" to "صلاة الليل والوتر", "qiyam2" to "صلاة الليل والوتر", "ghadwa" to "أذكار الصبح وقرآن، بلا تلفون",
    "deep1" to "تركيز: أصعب شغلة عندك", "deep2" to "تركيز: شغلة مهمة ثانية", "duha" to "استراحة وصلاة الضحى",
    "msgs1" to "رد على الرسائل", "close" to "الرسائل وتسكير شغل اليوم", "work1" to "الشغل: اتصالات ومتابعة",
    "work2" to "الشغل: برّة واجتماعات", "jumua_prep" to "تجهّز للجمعة: غُسل وسورة الكهف", "free" to "وقت حر ويّا الأهل",
    "self" to "وقتك: قراءة أو تعلّم خفيف", "self_am" to "وقتك أو ويّا الأهل", "visit" to "زيارة الأهل وطلعة ويّا الأطفال",
    "prep" to "تجهّز للنوم: محاسبة وترتيب باچر", "child" to "وقت ويّا ابنك وأذكار المساء", "family" to "الأسرة: عشاء وسوالف",
    "light" to "مشي بالشمس وفطور", "lunch" to "غداء ويّا الأهل", "dhuhr" to "صلاة الظهر", "jumua" to "صلاة الجمعة",
    "fajr" to "صلاة الفجر",
)

/** The title to show for a block. The user's own names and the training of the day stay as they are. */
fun plainTitle(key: String, title: String): String = Plain[key] ?: title

private val Short = mapOf(
    "fajr" to "الفجر", "dhuhr" to "الظهر", "jumua" to "الجمعة", "asr" to "العصر", "maghrib" to "المغرب", "isha" to "العشاء",
    "ghadwa" to "الأذكار", "light" to "مشي", "deep1" to "تركيز", "deep2" to "تركيز", "duha" to "استراحة", "msgs1" to "الرسائل",
    "close" to "الرسائل", "work1" to "الشغل", "work2" to "الشغل", "nap" to "قيلولة", "child" to "الابن", "family" to "الأسرة",
    "self" to "وقتك", "self_am" to "وقتك", "prep" to "تجهّز", "witr" to "الوتر", "sleep" to "النوم", "qiyam" to "صلاة الليل",
    "qiyam2" to "صلاة الليل", "jumua_prep" to "تجهّز", "lunch" to "الغداء", "visit" to "زيارة", "free" to "وقت حر",
    "dua" to "الدعاء", "fixed:work" to "الدوام",
)

/** The big ink word for a block: one or two short words. */
fun shortWord(key: String, title: String, catalogWord: String? = null): String {
    if (catalogWord != null) return catalogWord
    if (key.startsWith("free:")) return "فاضي"
    if (key == "train") return when { "مشي" in title -> "مشي"; "تعلم" in title -> "تعلّم"; else -> "تدريب" }
    return Short[key] ?: title.split(' ', ':', '،').first { it.isNotBlank() }
}

private val What = mapOf(
    "qiyam2" to "تصحى قبل الفجر بشوية وتصلّي صلاة الليل والوتر. التطبيق يحطها بس إذا يبقى إلك نوم 6 ساعات أو أكثر، وإذا ما يبقى يحط الوتر قبل النوم بدالها.",
    "qiyam" to "تصحى قبل الفجر بشوية وتصلّي صلاة الليل والوتر. التطبيق يحطها بس إذا يبقى إلك نوم 6 ساعات أو أكثر.",
    "witr" to "تصلّي الوتر قبل لا تنام. إذا نومك الليلة يكفي، التطبيق يحط صلاة الليل قبل الفجر بداله.",
    "ghadwa" to "بعد صلاة الفجر: أذكار الصبح وشوية قرآن، والتلفون بعيد عنك.",
    "deep1" to "أصعب شغلة عندك اليوم، وأنت بعدك فايق. اضغط «ركّز» حتى تسكت التنبيهات لحد ما تخلص.",
    "deep2" to "شغلة مهمة ثانية تحتاج تركيز. اضغط «ركّز» حتى تسكت التنبيهات لحد ما تخلص.",
    "msgs1" to "وقت قصير ترد بيه على الرسائل والمكالمات مرة وحدة، بدل ما تقطع شغلك كل شوية.",
    "close" to "ترد على اللي بقى من الرسائل وتسكّر شغل اليوم.",
    "work1" to "شغلك: اتصالات ومتابعة. مهامك تنحط هنا إذا بيها فراغ.",
    "work2" to "شغلك برّة أو اجتماعات. مهامك تنحط هنا إذا بيها فراغ.",
    "duha" to "استراحة قصيرة، وإذا تحب صلاة الضحى.",
    "nap" to "قيلولة قصيرة تعوّض شوية من نوم الليل.",
    "prep" to "قبل النوم: محاسبة دقيقتين وترتيب شغلات باچر.",
    "free" to "وقت حر ويّا أهلك. مهامك تنحط هنا إذا بيها فراغ.",
    "self" to "وقت إلك. مهامك تنحط هنا إذا بيها فراغ.",
    "self_am" to "وقت إلك أو ويّا أهلك. مهامك تنحط هنا إذا بيها فراغ.",
    "light" to "تطلع للشمس وتمشي شوية، وتفطر.",
    "jumua_prep" to "تتجهّز لصلاة الجمعة: غُسل، وتقرا سورة الكهف، وتروح بدري.",
)

/** One line for «شنو هاي؟», or null when the title says it all. */
fun whatIs(key: String): String? = when {
    key.startsWith("free:") -> "وقت ما حاطط بيه شي. مهامك تنحط هنا وحدها."
    key == "fixed:work" -> "دوامك بساعته الثابتة. الصلاة بنصه تبقى بوقتها."
    else -> What[key]
}

/** Blocks where «ركّز» makes sense. */
fun isFocusable(key: String, icon: String): Boolean =
    key in setOf("deep1", "deep2", "train", "self") || icon in setOf("nib", "book", "laptop")
