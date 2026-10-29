package com.dcc.yawm.shared.day

import kotlinx.datetime.LocalDate

/*
 * Tasks (owner's decision 2026-10-29, «4ج»): a task drops by itself into the first free time of
 * its day that can hold it, and any task can be pinned to a clock time instead. Unpinned tasks
 * never overlap each other or a pinned one. A task with no day waits «بالبال»; a task left
 * undone from an earlier day comes along to today.
 */

data class TaskItem(
    val id: String,
    val title: String,
    val minutes: Int,
    /** The day it is for; null = «بالبال» (no day yet). */
    val day: LocalDate?,
    /** Minutes from that day's midnight when the user chose the time; null = drop into free time. */
    val pinAt: Int? = null,
    /** The day it was done; null = still open. */
    val doneOn: LocalDate? = null,
) {
    val done: Boolean get() = doneOn != null
}

/** Free time a task can go into. */
data class Slot(val key: String, val start: Int, val end: Int)

data class PlacedTask(
    val task: TaskItem,
    /** The block it sits in (a free block, or for a pinned task whatever is at that time). */
    val slotKey: String?,
    val start: Int?,
    val pinned: Boolean = false,
    /** No free time today is long enough; the screen says it moves on. */
    val noRoom: Boolean = false,
)

/** The tasks of [date]: its own, plus older open ones carried over to today. */
fun tasksOn(date: LocalDate, today: LocalDate, tasks: List<TaskItem>): List<TaskItem> = tasks.filter { t ->
    val d = t.day ?: return@filter false
    d == date || (date == today && d < today && !t.done)
}

/**
 * Places [tasks] (already those of one day) in [slots], not before minute [from]. Pinned tasks
 * keep their time; open unpinned ones take the first gap that holds them whole, five-minute
 * aligned, skipping pinned time. Done tasks are returned without a place.
 */
fun placeTasks(tasks: List<TaskItem>, slots: List<Slot>, from: Int, blocks: List<Slot> = slots): List<PlacedTask> {
    val busy = tasks.filter { !it.done && it.pinAt != null }.map { it.pinAt!! until it.pinAt + it.minutes }.toMutableList()
    val cursor = slots.filter { it.end > from }.associate { it.key to ceil5(maxOf(it.start, from)) }.toMutableMap()
    val placed = mutableMapOf<String, PlacedTask>()
    for (t in tasks) {
        if (t.done) placed[t.id] = PlacedTask(t, null, null)
        else if (t.pinAt != null) placed[t.id] = PlacedTask(t, blocks.firstOrNull { t.pinAt >= it.start && t.pinAt < it.end }?.key, t.pinAt, pinned = true)
    }
    for (t in tasks) {
        if (t.done || t.pinAt != null) continue
        var hit: PlacedTask? = null
        for (s in slots) {
            var at = cursor[s.key] ?: continue
            for (k in 0 until 16) {
                val clash = busy.firstOrNull { at < it.last + 1 && at + t.minutes > it.first } ?: break
                at = ceil5(clash.last + 1)
            }
            if (s.end - at >= t.minutes && busy.none { at < it.last + 1 && at + t.minutes > it.first }) {
                hit = PlacedTask(t, s.key, at)
                busy += at until at + t.minutes
                cursor[s.key] = at + t.minutes
                break
            }
        }
        placed[t.id] = hit ?: PlacedTask(t, null, null, noRoom = true)
    }
    return tasks.map { placed.getValue(it.id) }
}

private fun ceil5(m: Int) = (m + 4) / 5 * 5

/** Keys of the template's blocks that hold free time («الشغل»، «وقتك»، «فراغ»…). */
val TemplateFreeKeys = setOf("work1", "work2", "self", "self_am", "free", "visit")

fun isFreeKey(key: String): Boolean = key in TemplateFreeKeys || key.startsWith("free:")
