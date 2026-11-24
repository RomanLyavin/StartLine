package com.startline.app.data

import org.json.JSONObject
import java.time.Instant

/**
 * Резервная копия настроек пользователя — пункт 4.2 плана.
 * Функции чистые (без Android-зависимостей), чтобы их можно было тестировать.
 */
object SettingsBackup {

    const val VERSION = 1

    /**
     * @param includeKeys включать ли API-ключи. По умолчанию включаются,
     * иначе в файле остаются пустые поля и восстановление настроек неполное.
     */
    fun exportJson(profile: Profile, includeKeys: Boolean = true, now: Instant = Instant.now()): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", now.toString())
        root.put("name", profile.name)
        root.put("heightCm", profile.heightCm ?: JSONObject.NULL)
        root.put("weightKg", profile.weightKg ?: JSONObject.NULL)
        root.put("age", profile.age ?: JSONObject.NULL)
        root.put("sex", profile.sex)
        root.put("hrMaxRun", profile.hrMaxRun ?: JSONObject.NULL)
        root.put("hrMaxBike", profile.hrMaxBike ?: JSONObject.NULL)
        root.put("hrRest", profile.hrRest ?: JSONObject.NULL)
        root.put("lthrRun", profile.lthrRun ?: JSONObject.NULL)
        root.put("lthrBike", profile.lthrBike ?: JSONObject.NULL)
        root.put("thresholdPaceSec", profile.thresholdPaceSec ?: JSONObject.NULL)
        root.put("ftpWatts", profile.ftpWatts ?: JSONObject.NULL)
        root.put("intervalsId", profile.intervalsId)
        root.put("trainingDays", profile.trainingDays)
        root.put("longRunDay", profile.longRunDay)
        root.put("highIntensityDay", profile.highIntensityDay)
        root.put("strengthDay", profile.strengthDay)
        root.put("strengthEveryWeeks", profile.strengthEveryWeeks)
        root.put("aiBaseUrl", profile.aiBaseUrl)
        root.put("aiModel", profile.aiModel)
        if (includeKeys) {
            root.put("intervalsKey", profile.intervalsKey)
            root.put("aiKey", profile.aiKey)
        } else {
            root.put("intervalsKey", "")
            root.put("aiKey", "")
        }
        return root.toString(2)
    }

    /** Читает резервную копию. Пустые или повреждённые поля возвращают значения по умолчанию. */
    fun importJson(json: String): Profile {
        val root = JSONObject(json)
        val version = root.optInt("version", 0)
        if (version <= 0) throw IllegalArgumentException("Файл резервной копии без версии настроек")
        if (version > VERSION) throw IllegalArgumentException("Резервная копия сделана новой версией приложения")

        fun intOrNull(key: String): Int? {
            if (root.isNull(key)) return null
            val value = root.opt(key)
            if (value is Number) return value.toInt()
            return value?.toString()?.filter { it.isDigit() }?.toIntOrNull()
        }

        fun doubleOrNull(key: String): Double? {
            if (root.isNull(key)) return null
            val value = root.opt(key)
            if (value is Number) return value.toDouble()
            return value?.toString()?.replace(',', '.')?.toDoubleOrNull()
        }

        return Profile(
            name = root.optString("name"),
            heightCm = intOrNull("heightCm"),
            weightKg = doubleOrNull("weightKg"),
            age = intOrNull("age"),
            sex = root.optString("sex", "M").ifBlank { "M" },
            hrMaxRun = intOrNull("hrMaxRun"),
            hrMaxBike = intOrNull("hrMaxBike"),
            hrRest = intOrNull("hrRest"),
            lthrRun = intOrNull("lthrRun"),
            lthrBike = intOrNull("lthrBike"),
            thresholdPaceSec = intOrNull("thresholdPaceSec"),
            ftpWatts = intOrNull("ftpWatts"),
            intervalsId = root.optString("intervalsId", "0").ifBlank { "0" },
            intervalsKey = root.optString("intervalsKey"),
            trainingDays = intOrNull("trainingDays") ?: 6,
            longRunDay = intOrNull("longRunDay") ?: 6,
            highIntensityDay = intOrNull("highIntensityDay") ?: 2,
            strengthDay = intOrNull("strengthDay") ?: 4,
            strengthEveryWeeks = intOrNull("strengthEveryWeeks") ?: 1,
            aiBaseUrl = root.optString("aiBaseUrl").ifBlank { "https://openrouter.ai/api/v1" },
            aiModel = root.optString("aiModel").ifBlank { "deepseek/deepseek-chat-v3-0324:free" },
            aiKey = root.optString("aiKey"),
        )
    }
}
