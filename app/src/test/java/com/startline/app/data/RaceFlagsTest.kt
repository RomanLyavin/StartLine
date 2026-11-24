package com.startline.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaceFlagsTest {

    @Test
    fun detectsRaceByType() {
        assertTrue(RaceFlags.isRace("Race", "Тренировка"))
        assertTrue(RaceFlags.isRace("competition", "Выходная"))
    }

    @Test
    fun detectsRaceByNameOnBothLanguages() {
        assertTrue(RaceFlags.isRace("Run", "Полумарафон Москва"))
        assertTrue(RaceFlags.isRace("Ride", "Time trial"))
        assertTrue(RaceFlags.isRace("Run", "гонка в парке"))
    }

    @Test
    fun ordinaryTrainingIsNotRace() {
        assertFalse(RaceFlags.isRace("Run", "Лёгкая 12 км"))
        assertFalse(RaceFlags.isRace(null, null))
    }

    @Test
    fun explicitFlagWins() {
        assertTrue(RaceFlags.isRace("Run", "Лёгкая", explicitFlag = true))
    }
}
