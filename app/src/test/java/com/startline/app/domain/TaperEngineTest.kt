package com.startline.app.domain

import com.startline.app.data.Race
import com.startline.app.data.RacePriority
import com.startline.app.data.Sport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaperEngineTest {

    private val today = LocalDate.of(2026, 11, 24)

    private fun race(date: LocalDate): Race = Race(
        userId = 1,
        sport = Sport.RUN,
        name = "Полумарафон",
        date = date,
        priority = RacePriority.A,
    )

    @Test
    fun noRaceMeansNoPlan() {
        val plan = TaperEngine.plan(emptyList(), today)
        assertNull(plan.race)
        assertEquals(6, plan.steps.size)
        assertTrue(plan.recommendation.contains("Добавьте старт"))
    }

    @Test
    fun tenDaysOutIsSecondTaperStep() {
        val plan = TaperEngine.plan(listOf(race(today.plusDays(10))), today)
        assertNotNull(plan.current)
        assertEquals("Подводка 2", plan.current!!.phase)
        assertEquals(60, plan.current!!.volumePct)
        assertEquals(10L, plan.daysLeft)
    }

    @Test
    fun raceDayIsZeroVolume() {
        val plan = TaperEngine.plan(listOf(race(today)), today)
        assertEquals("День гонки", plan.current!!.phase)
        assertEquals(0, plan.current!!.volumePct)
    }

    @Test
    fun picksTheNearestStart() {
        val plan = TaperEngine.plan(
            listOf(race(today.plusDays(40)), race(today.plusDays(12))),
            today,
        )
        assertEquals(12L, plan.daysLeft)
    }
}
