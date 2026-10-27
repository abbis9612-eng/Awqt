package com.dcc.yawm.shared.day

import kotlin.test.Test
import kotlin.test.assertEquals

class ChecklistTest {
    @Test
    fun tenPointsNumberedOneToTen() {
        assertEquals((1..10).toList(), ChecklistPoint.entries.map { it.number })
        assertEquals(ChecklistPoint.SLEEP_ON_TIME, ChecklistPoint.ofNumber(10))
        assertEquals(null, ChecklistPoint.ofNumber(11))
    }

    @Test
    fun allRolesShowAllTen() {
        val done = setOf(ChecklistPoint.PRAYERS, ChecklistPoint.NAP, ChecklistPoint.CHILDREN)
        assertEquals(ChecklistScore(3, 10), checklistScore(done, Role.entries.toSet()))
    }

    @Test
    fun hiddenPointsNeverCount() {
        // No child role: point 8 is hidden even if it was ticked before the role was removed.
        val roles = Role.entries.toSet() - Role.CHILD
        val done = setOf(ChecklistPoint.PRAYERS, ChecklistPoint.CHILDREN)
        assertEquals(ChecklistScore(1, 9), checklistScore(done, roles))
        assertEquals(ChecklistScore(0, 0), checklistScore(done, emptySet()))
    }
}
