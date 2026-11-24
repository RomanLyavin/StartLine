package com.startline.app.update

import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Последний стабильный релиз GitHub с APK-файлом.
 * [apkUrl] — ссылка на скачивание, либо null, если APK в релизе нет.
 */
data class GitHubRelease(
    val tag: String,
    val version: String,
    val apkUrl: String?,
    val apkName: String?,
    val apkSize: Long,
)

/**
 * Читает последний релиз публичного репозитория StartLine и скачивает APK.
 * Токен не нужен: репозиторий открытый.
 */
object GitHubUpdate {
    const val REPO_URL = "https://github.com/RomanLyavin/StartLine"
    const val RELEASES_URL = "$REPO_URL/releases"
    const val API_URL = "https://api.github.com/repos/RomanLyavin/StartLine/releases/latest"

    private const val MAX_APK_BYTES = 80L * 1024L * 1024L
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    fun parseLatestRelease(json: String): GitHubRelease? {
        val root = JSONObject(json)
        if (root.optBoolean("draft", false) || root.optBoolean("prerelease", false)) {
            return null
        }
        val tag = root.optString("tag_name").trim()
        if (tag.isEmpty()) return null
        val version = normalizeTag(tag)
        val assets = root.optJSONArray("assets")
            ?: return GitHubRelease(tag, version, null, null, 0L)

        var best: JSONObject? = null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            if (best == null) {
                best = asset
                continue
            }
            val bestName = best.optString("name")
            val exact = name.contains(version, ignoreCase = true)
            val bestExact = bestName.contains(version, ignoreCase = true)
            if (exact && !bestExact) best = asset
        }

        val apk = best ?: return GitHubRelease(tag, version, null, null, 0L)
        val apiUrl = apk.optString("url").takeIf { it.isNotBlank() }
        val browserUrl = apk.optString("browser_download_url").takeIf { it.isNotBlank() }
        return GitHubRelease(
            tag = tag,
            version = version,
            apkUrl = apiUrl ?: browserUrl,
            apkName = apk.optString("name").takeIf { it.isNotBlank() },
            apkSize = apk.optLong("size", 0L).coerceAtLeast(0L),
        )
    }

    /** Преобразует `v1.0.2` или `1.0.2-beta` в `1.0.2`. */
    fun normalizeTag(tag: String): String =
        tag.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')

    fun isNewer(remote: String, current: String): Boolean =
        compareVersions(remote, current) > 0

    /**
     * Сравнивает числовые части версий. Недостающая часть считается нулём,
     * поэтому `1.0` и `1.0.0` равны.
     */
    fun compareVersions(first: String, second: String): Int {
        val left = numericParts(first) ?: return 0
        val right = numericParts(second) ?: return 0
        val size = maxOf(left.size, right.size)
        for (index in 0 until size) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    fun fetchLatest(currentVersion: String): GitHubRelease {
        val connection = openConnection(API_URL, currentVersion, "application/vnd.github+json")
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("GitHub HTTP $code")
            }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            return parseLatestRelease(json)
                ?: throw IOException("В релизе GitHub не указана версия")
        } finally {
            connection.disconnect()
        }
    }

    fun downloadApk(
        url: String,
        target: File,
        expectedSize: Long,
        currentVersion: String,
        onProgress: (Int) -> Unit,
    ): File {
        if (!url.startsWith("https://", ignoreCase = true)) {
            throw IOException("APK должен скачиваться по HTTPS")
        }
        val connection = openConnection(url, currentVersion, "application/octet-stream")
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("APK HTTP $code")
            }
            val total = connection.contentLengthLong.takeIf { it > 0L } ?: expectedSize
            if (total > MAX_APK_BYTES) throw IOException("APK слишком большой")

            target.parentFile?.mkdirs()
            var downloaded = 0L
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        downloaded += read
                        if (downloaded > MAX_APK_BYTES) {
                            throw IOException("APK слишком большой")
                        }
                        output.write(buffer, 0, read)
                        if (total > 0L) {
                            val percent = ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                            onProgress(percent)
                        }
                    }
                }
            }
            if (downloaded <= 0L) throw IOException("Скачанный APK пуст")
            return target
        } finally {
            connection.disconnect()
        }
    }

    private fun numericParts(version: String): List<Int>? {
        val cleaned = normalizeTag(version)
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split('.')
        val result = ArrayList<Int>(parts.size)
        for (part in parts) {
            val digits = part.takeWhile { it.isDigit() }
            if (digits.isEmpty()) return null
            result.add(digits.toIntOrNull() ?: return null)
        }
        return result
    }

    private fun openConnection(
        url: String,
        currentVersion: String,
        accept: String,
    ): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", accept)
        connection.setRequestProperty("User-Agent", "StartLine/$currentVersion")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        return connection
    }
}
