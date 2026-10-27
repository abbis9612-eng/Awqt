package com.dcc.yawm.shared

import com.dcc.yawm.shared.content.ReviewedTexts
import kotlin.test.Test
import kotlin.test.assertTrue

class ContentGateTest {
    /** CLAUDE.md rule 6: no religious text ships without source, grade, grader and reviewer. */
    @Test
    fun everyShippedTextIsReviewed() {
        for (t in ReviewedTexts.all) {
            assertTrue(
                listOf(t.text, t.source, t.grade, t.gradedBy, t.reviewedBy, t.reviewedAt).all { it.isNotBlank() },
                "unreviewed text ${t.id}",
            )
        }
        assertTrue(ReviewedTexts.all.map { it.id }.toSet().size == ReviewedTexts.all.size)
    }
}
