package com.dcc.yawm.shared.content

/** One religious text with its source, grade and review (CLAUDE.md content rules). */
data class ReligiousText(
    val id: String,
    /** quran, hadith or athar. */
    val kind: String,
    val text: String,
    val source: String,
    val grade: String,
    val gradedBy: String,
    /** Shown with the text when the grading is disputed. */
    val note: String?,
    /** Where the app uses it, from METHOD.md section 11. */
    val usage: String,
    val reviewedBy: String,
    val reviewedAt: String,
)

/** The only door to religious text in the app: reviewed records, nothing else. */
object ReviewedTexts {
    val all: List<ReligiousText> get() = ReviewedTextList
    fun byId(id: String): ReligiousText? = ReviewedTextList.firstOrNull { it.id == id }
}
