package com.dcc.yawm.shared.day

/** The ten daily points of docs/METHOD.md section 6, with their labels and roles as written there. */
enum class ChecklistPoint(val number: Int, val role: Role, val label: String) {
    PRAYERS(1, Role.WORSHIP, "الصلوات الخمس في وقتها"),
    MORNING_WIRD(2, Role.WORSHIP, "ورد الغدوة (أذكار وقرآن)"),
    NO_PHONE_EDGES(3, Role.SELF, "لا تلفون في الساعة الأولى والأخيرة من اليوم"),
    DEEP_WORK(4, Role.DEV, "كتلة عمق واحدة على الأقل"),
    MOVEMENT(5, Role.BODY, "تدريب أو مشي حسب اليوم"),
    NAP(6, Role.BODY, "قيلولة أو راحة الظهر"),
    SPOUSE(7, Role.SPOUSE, "20 دقيقة مع الزوج/الزوجة"),
    CHILDREN(8, Role.CHILD, "30 دقيقة مع الأبناء"),
    LEARNING(9, Role.LEARNING, "تعلم أو خلوة تفكر"),
    SLEEP_ON_TIME(10, Role.BODY, "النوم في الموعد"),
    ;

    companion object {
        fun ofNumber(number: Int): ChecklistPoint? = entries.firstOrNull { it.number == number }

        /** Points whose role the user did not choose are hidden (METHOD.md section 6). */
        fun visibleFor(roles: Set<Role>): List<ChecklistPoint> = entries.filter { it.role in roles }
    }
}

/** A day's score: points done out of the points shown. Hidden points never count. */
data class ChecklistScore(val done: Int, val shown: Int)

fun checklistScore(done: Set<ChecklistPoint>, roles: Set<Role>): ChecklistScore {
    val shown = ChecklistPoint.visibleFor(roles)
    return ChecklistScore(done = shown.count { it in done }, shown = shown.size)
}
