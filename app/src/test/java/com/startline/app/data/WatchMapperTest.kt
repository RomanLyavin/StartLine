package com.startline.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchMapperTest {

    private val profile = Profile(
        userId = 1,
        lthrRun = 160,
        lthrBike = 170,
    )

    @Test
    fun findsArrayInPlainRoot() {
        val array = WatchMapper.extractArray("""[{"id":"a1"}]""")
        assertEquals(1, array.length())
    }

    @Test
    fun findsArrayInNestedObject() {
        val array = WatchMapper.extractArray("""{"data":{"list":[{"id":"a1"},{"id":"a2"}]}}""")
        assertEquals(2, array.length())
    }

    @Test
    fun mapsGarminStyleRecord() {
        val o = JSONObject(
            """
            {
              "id": 12345,
              "activityName": "Гонка по грунту",
              "startTimeLocal": "2026-11-20T08:15:00.000Z",
              "distance": 42000.0,
              "duration": 5400.0,
              "averageHeartRate": 152,
              "maxHeartRate": 178,
              "activityType": {"label": "Cycling"},
              "calories": 900
            }
            """.trimIndent()
        )
        val activity = WatchMapper.mapActivity(1L, WatchProvider.GARMIN, o, profile)
        assertNotNull(activity)
        assertEquals("garmin:12345", activity!!.id)
        assertEquals(Sport.BIKE, activity.sport)
        assertEquals(42000.0, activity.distanceM, 0.001)
        assertEquals(5400, activity.movingSec)
        assertEquals(152, activity.avgHr)
        assertEquals("garmin", activity.source)
        assertTrue(activity.isRace)
    }

    @Test
    fun mapsPolarStyleRecordWithEpochMillis() {
        val o = JSONObject(
            """
            {
              "id": "777",
              "title": "Интервалы",
              "start_time": 1763600000000,
              "duration": 3600,
              "distance": 12000,
              "averageHeartRate": 165,
              "sport": "running"
            }
            """.trimIndent()
        )
        val activity = WatchMapper.mapActivity(1L, WatchProvider.POLAR, o, profile)
        assertNotNull(activity)
        assertEquals("polar:777", activity!!.id)
        assertEquals(Sport.RUN, activity.sport)
        assertEquals(1763600000L, activity.startEpoch)
        assertFalse(activity.isRace)
    }

    @Test
    fun unknownSportIsSkipped() {
        val o = JSONObject("""{"id":"x","type":"swimming","start_time":1763600000}""")
        assertNull(WatchMapper.mapActivity(1L, WatchProvider.HUAWEI, o, profile))
    }

    @Test
    fun recordWithoutDateIsSkipped() {
        val o = JSONObject("""{"id":"x","type":"running"}""")
        assertNull(WatchMapper.mapActivity(1L, WatchProvider.AMAZFIT, o, profile))
    }

    @Test
    fun loadFallsBackToHeartRateFormula() {
        val o = JSONObject(
            """{"id":"x","type":"running","start_time":1763600000,"duration":7200,"averageHeartRate":160}"""
        )
        val activity = WatchMapper.mapActivity(1L, WatchProvider.AMAZFIT, o, profile)
        assertEquals(200.0, activity!!.load, 0.01)
    }
}
