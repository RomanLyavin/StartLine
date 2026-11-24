package com.startline.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/** Результат синхронизации с одним сервисом часов. */
data class WatchSyncResult(
    val provider: WatchProvider,
    val activityCount: Int,
)

/**
 * Модуль синхронизации тренировок с сервисами часов — пункты 2.1–2.4 плана:
 * Amazfit/Zepp, Garmin Connect, Huawei Health, Polar Flow.
 *
 * Форматы ответов у производителей различаются, поэтому используется таблица
 * алиасов: каждое значение ищется по списку возможных имён полей. Мэппинг
 * вынесен в отдельные функции, чтобы его можно было тестировать без сети.
 */
class WatchSync(private val oauth: WatchOAuthManager) {

    /** Маппинг вынесен в WatchMapper, чтобы его можно было тестировать без Android-контекста. */
    fun extractArray(body: String): JSONArray = WatchMapper.extractArray(body)

    fun mapActivity(userId: Long, provider: WatchProvider, o: JSONObject, profile: Profile): Activity? =
        WatchMapper.mapActivity(userId, provider, o, profile)

    /**
     * Загружает тренировки сервиса за период. Требует подключённого токена.
     * Идентификатор пользователя сервиса берётся из сохранённого значения,
     * иначе запрашивается через userUrl сервиса.
     */
    fun fetchActivities(
        userId: Long,
        provider: WatchProvider,
        profile: Profile,
        oldest: LocalDate,
        newest: LocalDate,
    ): List<Activity> {
        val connection = oauth.connection(provider)
        if (!connection.connected || connection.accessToken.isBlank()) {
            throw IllegalStateException("«${provider.label}» не подключён — выполните OAuth-авторизацию")
        }
        val user = oauth.userId(provider).ifBlank { resolveUserId(provider, connection.accessToken) }
        val body = get(provider.activitiesUrl.replace("{user}", user.ifBlank { "me" }), connection.accessToken)
        val array = extractArray(body)
        val result = ArrayList<Activity>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val activity = mapActivity(userId, provider, o, profile) ?: continue
            if (activity.date.isBefore(oldest) || activity.date.isAfter(newest)) continue
            result.add(activity)
        }
        return result
    }

    private fun resolveUserId(provider: WatchProvider, token: String): String {
        val body = runCatching { get(provider.userUrl, token) }.getOrNull() ?: return ""
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return ""
        val id = listOf("id", "user_id", "userId", "ownerId", "profileId")
            .firstNotNullOfOrNull { k -> json.optString(k).takeIf { it.isNotBlank() } }
        if (id != null) oauth.saveUserId(provider, id)
        return id.orEmpty()
    }

    private fun get(url: String, token: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 25_000
            readTimeout = 35_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } }.orEmpty()
            if (code !in 200..299) {
                throw IOException(
                    when (code) {
                        401, 403 -> "«${labelFor(url)}» отклонил токен (код $code). Переподключите часы."
                        404 -> "«${labelFor(url)}»: данные не найдены (код 404). Проверьте идентификатор пользователя."
                        429 -> "«${labelFor(url)}»: слишком много запросов, попробуйте позже."
                        else -> "«${labelFor(url)}» вернул ошибку $code"
                    },
                )
            }
            return body
        } finally {
            conn.disconnect()
        }
    }

    private fun labelFor(url: String): String =
        WatchProvider.entries.firstOrNull { url.startsWith(it.activitiesUrl.substringBefore("{user}")) }?.label
            ?: "сервис часов"
}
