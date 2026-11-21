package com.startline.app.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

/**
 * Поддерживаемые производители часов. Для каждого сервиса задаётся
 * конфигурация OAuth 2.0 Authorization Code flow.
 *
 * ВАЖНО: clientId/clientSecret — публичные демо-значения. Для реальной
 * работы нужно зарегистрировать приложение у производителя и заменить
 * значения на выданные ключи (см. README.md).
 */
enum class WatchProvider(
    val key: String,
    val label: String,
    val clientId: String,
    val clientSecret: String,
    val authorizeUrl: String,
    val tokenUrl: String,
    val scope: String,
) {
    AMAZFIT(
        "amazfit",
        "Amazfit / Zepp",
        "startline_amazfit",
        "startline_amazfit_secret",
        "https://api.amazfit.com/oauth/authorize",
        "https://api.amazfit.com/oauth/token",
        "user:read activity:read",
    ),
    GARMIN(
        "garmin",
        "Garmin Connect",
        "startline_garmin",
        "startline_garmin_secret",
        "https://connect.garmin.com/oauthConfirm",
        "https://connectapi.garmin.com/oauth-service/oauth/token",
        "",
    ),
    HUAWEI(
        "huawei",
        "Huawei Health",
        "startline_huawei",
        "startline_huawei_secret",
        "https://oauth.login.vmall.com/oauth2/authorize",
        "https://oauth.login.vmall.com/oauth2/token",
        "https://schemas.health.cloud.huawei.com/healthkit",
    ),
    POLAR(
        "polar",
        "Polar Flow",
        "startline_polar",
        "startline_polar_secret",
        "https://flow.polar.com/oauth2/authorization",
        "https://polarremote.com/v2/oauth2/token",
        "accesslink.read_all",
    );

    companion object {
        const val REDIRECT_URI = "startline://oauth/watch"
        fun fromKey(key: String?): WatchProvider? = entries.firstOrNull { it.key == key }
    }
}

data class WatchConnection(
    val provider: WatchProvider,
    val connected: Boolean,
    val connectedAt: Long = 0L,
    val accessToken: String = "",
    val refreshToken: String = "",
) {
    val connectedDate: String
        get() = if (connectedAt > 0) Instant.ofEpochMilli(connectedAt).toString().take(10) else "—"
}

/**
 * Хранит OAuth-токены в SharedPreferences. Токены шифруются через
 * Already существующий SecretStore (Android Keystore / AES-GCM).
 */
class WatchOAuthManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("startline_watch_oauth", Context.MODE_PRIVATE)
    private val secrets = SecretStore()

    fun authorizationUrl(provider: WatchProvider, state: String = "startline"): String {
        val params = mutableListOf(
            "client_id" to provider.clientId,
            "response_type" to "code",
            "redirect_uri" to WatchProvider.REDIRECT_URI,
            "state" to state,
        )
        if (provider.scope.isNotBlank()) params += "scope" to provider.scope
        return provider.authorizeUrl + "?" + params.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
    }

    /**
     * Обменивает код авторизации на access/refresh токен.
     * Возвращает true, если токен получен и сохранён.
     */
    fun exchangeCode(provider: WatchProvider, code: String): Boolean {
        val body = buildString {
            append("grant_type=authorization_code")
            append("&code=").append(URLEncoder.encode(code, "UTF-8"))
            append("&client_id=").append(URLEncoder.encode(provider.clientId, "UTF-8"))
            append("&client_secret=").append(URLEncoder.encode(provider.clientSecret, "UTF-8"))
            append("&redirect_uri=").append(URLEncoder.encode(WatchProvider.REDIRECT_URI, "UTF-8"))
        }
        val response = post(provider.tokenUrl, body)
        val json = runCatching { JSONObject(response) }.getOrNull() ?: return false
        val token = json.optString("access_token")
        if (token.isBlank()) return false
        save(
            WatchConnection(
                provider = provider,
                connected = true,
                connectedAt = System.currentTimeMillis(),
                accessToken = token,
                refreshToken = json.optString("refresh_token"),
            )
        )
        return true
    }

    /** Разбирает deep-link вида startline://oauth/watch?code=...&state=... */
    fun handleCallback(uri: Uri): WatchProvider? {
        if (uri.scheme != "startline" || uri.host != "oauth") return null
        val code = uri.getQueryParameter("code") ?: return null
        val provider = WatchProvider.fromKey(uri.getQueryParameter("state")?.removePrefix("startline_"))
            ?: WatchProvider.entries.firstOrNull { uri.getQueryParameter("state") == it.key }
            ?: return null
        return if (exchangeCode(provider, code)) provider else null
    }

    fun connections(): List<WatchConnection> = WatchProvider.entries.map { provider ->
        val token = prefs.getString("${provider.key}_token", "").orEmpty()
        val decrypted = if (token.isNotBlank()) secrets.decrypt(token) else ""
        WatchConnection(
            provider = provider,
            connected = decrypted.isNotBlank(),
            connectedAt = prefs.getLong("${provider.key}_at", 0L),
            accessToken = decrypted,
            refreshToken = prefs.getString("${provider.key}_refresh", "").orEmpty().let {
                if (it.isBlank()) "" else secrets.decrypt(it)
            },
        )
    }

    fun disconnect(provider: WatchProvider) {
        prefs.edit()
            .remove("${provider.key}_token")
            .remove("${provider.key}_refresh")
            .remove("${provider.key}_at")
            .apply()
    }

    private fun save(c: WatchConnection) {
        prefs.edit()
            .putString("${c.provider.key}_token", secrets.encrypt(c.accessToken))
            .putString("${c.provider.key}_refresh", secrets.encrypt(c.refreshToken))
            .putLong("${c.provider.key}_at", c.connectedAt)
            .apply()
    }

    private fun post(url: String, body: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25_000
            readTimeout = 35_000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            return stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } }.orEmpty()
        } catch (e: IOException) {
            throw IOException("Нет связи с ${WatchProvider.entries.firstOrNull { it.tokenUrl == url }?.label ?: "сервисом"}: ${e.message}", e)
        } finally {
            conn.disconnect()
        }
    }
}
