package com.startline.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SettingsBackupTest {

    private val profile = Profile(
        userId = 7,
        name = "Roman",
        heightCm = 178,
        weightKg = 71.5,
        age = 34,
        sex = "M",
        hrMaxRun = 192,
        hrMaxBike = 188,
        hrRest = 48,
        lthrRun = 160,
        lthrBike = 170,
        thresholdPaceSec = 255,
        ftpWatts = 240,
        intervalsId = "ath123",
        intervalsKey = "3jfe3dr911fsq3pgpqh873br1",
        trainingDays = 5,
        longRunDay = 6,
        highIntensityDay = 2,
        strengthDay = 4,
        aiModel = "deepseek/deepseek-chat-v3-0324:free",
        aiKey = "sk-test",
    )

    @Test
    fun roundTripKeepsAllFields() {
        val json = SettingsBackup.exportJson(profile)
        val restored = SettingsBackup.importJson(json)
        // В копии нет идентификатора пользователя — он берётся из текущей сессии.
        assertEquals(profile.copy(userId = 0), restored)
    }

    @Test
    fun keysCanBeExcludedFromCopy() {
        val restored = SettingsBackup.importJson(SettingsBackup.exportJson(profile, includeKeys = false))
        assertEquals("", restored.intervalsKey)
        assertEquals("", restored.aiKey)
        assertEquals(profile.name, restored.name)
        assertEquals(profile.trainingDays, restored.trainingDays)
    }

    @Test
    fun importKeepsScheduleDays() {
        val restored = SettingsBackup.importJson(SettingsBackup.exportJson(profile))
        assertEquals(5, restored.trainingDays)
        assertEquals(4, restored.strengthDay)
    }

    @Test
    fun importKeepsStrengthFrequency() {
        val withFrequency = profile.copy(strengthEveryWeeks = 2)
        val restored = SettingsBackup.importJson(SettingsBackup.exportJson(withFrequency))
        assertEquals(2, restored.strengthEveryWeeks)
    }

    @Test
    fun copyWithoutVersionIsRejected() {
        val error = runCatching { SettingsBackup.importJson("""{"name":"Roman"}""") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun copyFromNewerVersionIsRejected() {
        val json = """{"version":99,"name":"Roman"}"""
        val error = runCatching { SettingsBackup.importJson(json) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun exportContainsTimestamp() {
        val json = SettingsBackup.exportJson(profile, now = Instant.parse("2026-11-24T12:00:00Z"))
        assertTrue(json.contains("2026-11-24T12:00:00Z"))
    }
}
