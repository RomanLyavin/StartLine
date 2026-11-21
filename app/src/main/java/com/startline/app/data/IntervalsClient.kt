package com.startline.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.util.Base64

class IntervalsException(message: String) : IOException(message)

data class IcuAthlete(
    val id: String,
    val name: String,
    val weightKg: Double?,
    val restingHr: Int?,
    val sex: String?,
)

/**
 * Клиент intervals.icu API. Используется Basic Auth: пользователь API_KEY, пароль = ключ.
 * Документация: https://intervals.icu/api/v1/docs
 */
class IntervalsClient(
    private val apiKey: String,
    private val athleteId: String = "0",
) {
    private val base = "https://intervals.icu"
    private val auth: String = "Basic " + Base64.getEncoder()
        .encodeToString("API_KEY:$apiKey".toByteArray(Charsets.UTF_8))

    fun fetchAthlete(): IcuAthlete {
        val json = JSONObject(get("/api/v1/athlete/${enc(athleteId)}"))
        return IcuAthlete(
            id = json.optString("id", athleteId),
            name = json.optString("name", ""),
            weightKg = json.doubleOrNull("icu_weight") ?: json.doubleOrNull("weight"),
            restingHr = json.intOrNull("icu_resting_hr"),
            sex = json.optString("sex").takeIf { it.isNotBlank() },
        )
    }

    fun fetchActivities(
        userId: Long,
        oldest: LocalDate,
        newest: LocalDate,
        profile: Profile,
    ): List<Activity> {
        val url = "/api/v1/athlete/${enc(athleteId)}/activities?oldest=$oldest&newest=$newest"
        val array = JSONArray(get(url))
        val result = ArrayList<Activity>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i)
            if (o == null) continue
            val sport = Sport.fromIcu(o.optString("type"))
            if (sport == null) continue
            val rawId = o.optString("id")
            if (rawId.isBlank()) continue
            val startLocal = o.optString("start_date_local")
            if (startLocal.length < 10) continue
            val date = runCatching { LocalDate.parse(startLocal.substring(0, 10)) }.getOrNull()
            if (date == null) continue
            val moving = o.intOrNull("moving_time") ?: 0
            val avgHr = o.intOrNull("average_heartrate")
            val lthr = if (sport == Sport.RUN) profile.lthrRun else profile.lthrBike
            val load = o.doubleOrNull("icu_training_load") ?: fallbackLoad(moving, avgHr, lthr)
            val startEpoch = o.optString("start_date").let {
                runCatching { Instant.parse(it).epochSecond }.getOrDefault(0L)
            }
            result.add(
                Activity(
                    id = rawId,
                    userId = userId,
                    sport = sport,
                    name = o.optString("name").ifBlank { "Тренировка" },
                    date = date,
                    startLocal = startLocal,
                    startEpoch = startEpoch,
                    distanceM = o.doubleOrNull("distance") ?: o.doubleOrNull("icu_distance") ?: 0.0,
                    movingSec = moving,
                    elapsedSec = o.intOrNull("elapsed_time") ?: moving,
                    load = load,
                    avgHr = avgHr,
                    maxHr = o.intOrNull("max_heartrate"),
                    avgSpeed = o.doubleOrNull("average_speed"),
                    elevationM = o.doubleOrNull("total_elevation_gain"),
                    calories = o.intOrNull("calories"),
                    avgPower = o.intOrNull("average_watts") ?: o.intOrNull("icu_average_watts"),
                    normPower = o.intOrNull("weighted_average_watts") ?: o.intOrNull("icu_weighted_avg_watts"),
                    ctl = o.doubleOrNull("icu_ctl"),
                    atl = o.doubleOrNull("icu_atl"),
                    isRace = isRaceActivity(o),
                )
            )
        }
        return result
    }

    private fun fallbackLoad(movingSec: Int, avgHr: Int?, lthr: Int?): Double {
        if (movingSec <= 0) return 0.0
        val hours = movingSec / 3600.0
        if (avgHr == null || lthr == null || lthr <= 0) return hours * 40.0
        val ratio = avgHr.toDouble() / lthr
        return (hours * ratio * ratio * 100.0).coerceIn(0.0, 300.0)
    }

    fun fetchWellness(userId: Long, oldest: LocalDate, newest: LocalDate): List<WellnessDay> {
        val url = "/api/v1/athlete/${enc(athleteId)}/wellness?oldest=$oldest&newest=$newest"
        val array = JSONArray(get(url))
        val result = ArrayList<WellnessDay>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("id")
            val date = runCatching { LocalDate.parse(id) }.getOrNull() ?: continue
            result.add(
                WellnessDay(
                    date = date,
                    ctl = o.doubleOrNull("ctl"),
                    atl = o.doubleOrNull("atl"),
                    rampRate = o.doubleOrNull("rampRate"),
                    restingHr = o.intOrNull("restingHR"),
                    weightKg = o.doubleOrNull("weight"),
                    sleepSecs = o.intOrNull("sleepSecs"),
                    sleepScore = o.doubleOrNull("sleepScore"),
                )
            )
        }
        return result
    }

    fun fetchStreams(activityId: String): ActivityStreams {
        val array = JSONArray(get("/api/v1/activity/${enc(activityId)}/streams"))
        var time: List<Int> = emptyList()
        var hr: List<Int?> = emptyList()
        var speed: List<Double?> = emptyList()
        var distance: List<Double?> = emptyList()
        var altitude: List<Double?> = emptyList()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val data = o.optJSONArray("data") ?: continue
            when (o.optString("type")) {
                "time" -> time = ints(data)
                "heartrate" -> hr = nullableInts(data)
                "velocity_smooth" -> speed = nullableDoubles(data)
                "distance" -> distance = nullableDoubles(data)
                "altitude", "fixed_altitude" -> altitude = nullableDoubles(data)
            }
        }
        if (time.isEmpty()) {
            val n = maxOf(hr.size, speed.size, distance.size)
            time = (0 until n).toList()
        }
        return ActivityStreams(time, hr, speed, distance, altitude)
    }

    fun fetchActivity(userId: Long, activityId: String): Activity? =
        fetchActivityRaw(activityId)?.let { o ->
            val sport = Sport.fromIcu(o.optString("type")) ?: return null
            val startLocal = o.optString("start_date_local")
            if (startLocal.length < 10) return null
            Activity(
                id = activityId,
                userId = userId,
                sport = sport,
                name = o.optString("name").ifBlank { "Тренировка" },
                date = LocalDate.parse(startLocal.substring(0, 10)),
                startLocal = startLocal,
                startEpoch = runCatching { Instant.parse(o.optString("start_date")).epochSecond }.getOrDefault(0L),
                distanceM = o.doubleOrNull("distance") ?: 0.0,
                movingSec = o.intOrNull("moving_time") ?: 0,
                elapsedSec = o.intOrNull("elapsed_time") ?: 0,
                load = o.doubleOrNull("icu_training_load") ?: 0.0,
                avgHr = o.intOrNull("average_heartrate"),
                maxHr = o.intOrNull("max_heartrate"),
                avgSpeed = o.doubleOrNull("average_speed"),
                elevationM = o.doubleOrNull("total_elevation_gain"),
                calories = o.intOrNull("calories"),
                avgPower = o.intOrNull("average_watts"),
                normPower = o.intOrNull("weighted_average_watts"),
                ctl = o.doubleOrNull("icu_ctl"),
                atl = o.doubleOrNull("icu_atl"),
                isRace = isRaceActivity(o),
            )
        }

    private fun fetchActivityRaw(activityId: String): JSONObject? = runCatching {
        JSONObject(get("/api/v1/activity/${enc(activityId)}"))
    }.getOrNull()

    private fun get(path: String): String {
        val url = URL(base + path)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 25_000
            readTimeout = 35_000
            setRequestProperty("Authorization", auth)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.let { s ->
                BufferedReader(InputStreamReader(s, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            if (code !in 200..299) {
                val detail = runCatching { JSONObject(body).optString("error") }.getOrNull()
                    ?: body.take(200)
                throw IntervalsException(
                    when (code) {
                        401, 403 -> "intervals.icu отклонил API-ключ (код $code). Проверьте ключ в профиле."
                        404 -> "intervals.icu: данные не найдены (код 404). Проверьте ID атлета."
                        429 -> "intervals.icu: слишком много запросов, попробуйте позже."
                        else -> "intervals.icu вернул ошибку $code: $detail"
                    }
                )
            }
            return body
        } catch (e: IntervalsException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Нет связи с intervals.icu: ${e.message ?: "неизвестная ошибка"}", e)
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    /**
     * intervals.icu не имеет отдельного флага гонки в публичном API, поэтому
     * признак определяется по типу активности и названию. Поддерживаются
     * английские и русские обозначения стартов.
     */
    private fun isRaceActivity(o: JSONObject): Boolean {
        val type = o.optString("type").trim().lowercase()
        val name = o.optString("name").trim().lowercase()
        if (type == "race" || type == "competition") return true
        if (o.optBoolean("race", false) || o.optBoolean("is_race", false)) return true
        val markers = listOf(
            "гонка", "старт", "соревнование", "забег", "марафон", "полумарафон",
            "race", "racing", "event", "competition", "marathon", "half marathon",
            "10k", "5k", "triathlon", "duathlon", "time trial", "crit", "xco", "cx ",
        )
        return markers.any { name.contains(it) }
    }

    private fun JSONObject.doubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val v = optDouble(key, Double.NaN)
        return if (v.isNaN()) null else v
    }

    private fun JSONObject.intOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        val v = optDouble(key, Double.NaN)
        return if (v.isNaN()) null else v.toInt()
    }

    private fun ints(a: JSONArray): List<Int> = (0 until a.length()).map { a.optInt(it, 0) }

    private fun nullableInts(a: JSONArray): List<Int?> = (0 until a.length()).map {
        if (a.isNull(it)) null else a.optDouble(it, Double.NaN).takeIf { v -> !v.isNaN() }?.toInt()
    }

    private fun nullableDoubles(a: JSONArray): List<Double?> = (0 until a.length()).map {
        if (a.isNull(it)) null else a.optDouble(it, Double.NaN).takeIf { v -> !v.isNaN() }
    }
}
