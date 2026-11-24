package com.startline.app.domain

import com.startline.app.data.Profile
import com.startline.app.data.Workout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Частота силовых тренировок — пункт 1.3 плана доработок.
 */
class PlanEngineTest {

    // 23 ноября 2026 — понедельник, чтобы недели плана начинались с today.
    private val today = LocalDate.of(2026, 11, 23)

    private fun profile(everyWeeks: Int): Profile = Profile(
        userId = 1,
        trainingDays = 7,
        strengthDay = 4, // четверг → смещение 3 внутри недели
        strengthEveryWeeks = everyWeeks,
    )

    private fun strengthSessions(everyWeeks: Int): List<Workout> =
        PlanEngine.generate(profile(everyWeeks), FatigueState(ctl = 60.0), emptyList(), today, weeks = 4)
            .filter { it.type == "strength" }

    private fun offsets(sessions: List<Workout>): List<Long> =
        sessions.map { ChronoUnit.DAYS.between(today, it.date) }

    @Test
    fun everyWeekPutsStrengthInAllWeeks() {
        assertEquals(listOf(3L, 10L, 17L, 24L), offsets(strengthSessions(1)))
    }

    @Test
    fun everyTwoWeeksPutsStrengthOnlyInFirstAndThirdWeek() {
        assertEquals(listOf(3L, 17L), offsets(strengthSessions(2)))
    }

    @Test
    fun everyThreeWeeksPutsStrengthInFirstAndFourthWeek() {
        assertEquals(listOf(3L, 24L), offsets(strengthSessions(3)))
    }

    @Test
    fun everyFourWeeksPutsStrengthOnlyInFirstWeek() {
        assertEquals(listOf(3L), offsets(strengthSessions(4)))
    }
}
