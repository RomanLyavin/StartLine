package com.startline.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

/**
 * Чистый маппинг ответов сервисов часов в модель Activity — без Android-контекста,
 * чтобы его можно было тестировать. Используется модулем WatchSync.
 */
object WatchMapper {

    /** Ищет список активностей независимо от формы ответа сервиса. */
    fun extractArray(body: String): JSONArray {
        runCatching { JSONArray(body) }.getOrNull()?.let { return it }
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return JSONArray()
        for (key in listOf("activities", "list", "data", "items", "records", "result")) {
            val value = root.opt(key)
            if (value is JSONArray) return value
            if (value is JSONObject) {
                for (inner in listOf("list", "items", "data", "activities", "records")) {
                    val nested = value.opt(inner)
                    if (nested is JSONArray) return nested
                }
            }
        }
        return JSONArray()
    }

    /**
     * Одна запись сервиса в Activity. Возвращает null, если запись нельзя
     * отнести к бегу/велосипеду/силе или у неё нет идентификатора.
     */
    fun mapActivity(userId: Long, provider: WatchProvider, o: JSONObject, profile: Profile): Activity? {
        val id = firstString(o, "id", "activityId", "activity_id", "trackId", "recordId") ?: return null
        val type = activityType(o)
        val sport = Sport.fromWatchType(type) ?: return null
        val startEpoch = firstEpoch(o)
        if (startEpoch <= 0L) return null
        val date = Instant.ofEpochSecond(startEpoch).atZone(ZoneId.systemDefault()).toLocalDate()
        val name = firstString(o, "name", "title", "activityName", "label", "description")
            ?.ifBlank { null } ?: "Тренировка"
        val moving = firstInt(o, "moving_time", "movingTime", "duration", "durationSeconds", "totalTime", "time") ?: 0
        val avgHr = firstInt(o, "average_heartrate", "averageHeartRate", "avgHeartRate", "avg_hr", "averageHr")
        val distance = firstDouble(o, "distance", "distanceMeters", "total_distance") ?: 0.0
        val load = firstDouble(o, "training_load", "trainingLoad", "load", "score", "trainingScore")
            ?: fallbackLoad(moving, avgHr, if (sport == Sport.RUN) profile.lthrRun else profile.lthrBike)

        return Activity(
            id = "${provider.key}:$id",
            userId = userId,
            sport = sport,
            name = name,
            date = date,
            startLocal = Instant.ofEpochSecond(startEpoch).atZone(ZoneId.systemDefault()).toString(),
            startEpoch = startEpoch,
            distanceM = distance,
            movingSec = moving,
            elapsedSec = firstInt(o, "elapsed_time", "elapsedTime", "totalElapsed") ?: moving,
            load = load,
            avgHr = avgHr,
            maxHr = firstInt(o, "max_heartrate", "maxHeartRate", "max_hr", "maxHr"),
            avgSpeed = firstDouble(o, "average_speed", "averageSpeed", "speed"),
            elevationM = firstDouble(o, "total_elevation_gain", "elevationGain", "elevation", "ascents"),
            calories = firstInt(o, "calories", "energy", "kcal"),
            avgPower = firstInt(o, "average_watts", "averagePower", "power"),
            normPower = null,
            ctl = null,
            atl = null,
            isRace = RaceFlags.isRace(type, name, firstBoolean(o, "race", "is_race", "isRace", "competition")),
            source = provider.key,
        )
    }

    private fun fallbackLoad(movingSec: Int, avgHr: Int?, lthr: Int?): Double {
        if (movingSec <= 0) return 0.0
        val hours = movingSec / 3600.0
        if (avgHr == null || lthr == null || lthr <= 0) return hours * 40.0
        val ratio = avgHr.toDouble() / lthr
        return (hours * ratio * ratio * 100.0).coerceIn(0.0, 300.0)
    }

    /**
     * Тип активности: у одних сервисов это строка, у других — вложенный
     * объект activityType с полем label (Garmin).
     */
    private fun activityType(o: JSONObject): String? {
        val direct = firstString(o, "type", "sport", "sportName", "exerciseType", "category")
        if (!direct.isNullOrBlank()) return direct
        val nested = o.opt("activityType")
        if (nested is JSONObject) return nested.optString("label").takeIf { it.isNotBlank() }
        return null
    }

    private fun firstString(o: JSONObject, vararg keys: String): String? =
        keys.firstNotNullOfOrNull { k -> o.optString(k).takeIf { it.isNotBlank() } }

    private fun firstInt(o: JSONObject, vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { k -> if (o.has(k) && !o.isNull(k)) o.optInt(k, 0) else null }

    private fun firstDouble(o: JSONObject, vararg keys: String): Double? =
        keys.firstNotNullOfOrNull { k -> if (o.has(k) && !o.isNull(k)) o.optDouble(k, 0.0).takeIf { !it.isNaN() } else null }

    private fun firstBoolean(o: JSONObject, vararg keys: String): Boolean =
        keys.any { k -> o.optBoolean(k, false) }

    /** Epoch в секундах: сервисы отдают либо секунды, либо миллисекунды. */
    private fun firstEpoch(o: JSONObject): Long {
        val numeric = firstLong(o, "start_time", "startTime", "startEpoch", "timestamp")
        if (numeric != null && numeric > 0) return if (numeric > 10_000_000_000L) numeric / 1000 else numeric
        val raw = firstString(o, "start_date_local", "startTimeLocal", "startedAt", "start_time", "startTime", "date")
        if (raw.isNullOrBlank()) return 0L
        runCatching { Instant.parse(raw).epochSecond }.getOrNull()?.let { return it }
        runCatching { Instant.ofEpochMilli(raw.toLong()).epochSecond }.getOrNull()?.let { return it }
        return 0L
    }

    private fun firstLong(o: JSONObject, vararg keys: String): Long? =
        keys.firstNotNullOfOrNull { k -> if (o.has(k) && !o.isNull(k)) o.optLong(k, 0L).takeIf { it > 0 } else null }
}
