package com.startline.app.ai

import com.startline.app.data.Activity
import com.startline.app.data.Fmt
import com.startline.app.data.Profile
import com.startline.app.data.Race
import com.startline.app.data.Sport
import com.startline.app.data.Workout
import com.startline.app.domain.AthleteModel
import com.startline.app.domain.FatigueState
import com.startline.app.domain.RiskLevel
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AiResult(
    val text: String,
    val usedAi: Boolean,
    val model: String = "",
)

object AiPrompt {
    fun system(): String = buildString {
        append("Ты опытный тренер по бегу и велоспорту. Анализируй данные спортсмена ")
        append("и давай конкретные, безопасные рекомендации на русском языке. ")
        append("Опирайся только на переданные цифры. Не выдумывай измерения. ")
        append("Формат ответа: 1) Оценка состояния (2–3 предложения). ")
        append("2) Риски и что контролировать (2–3 пункта). ")
        append("3) План на 7 дней (3–5 пунктов с длительностью и интенсивностью). ")
        append("4) Одна главная рекомендация. Без markdown-таблиц, до 350 слов.")
    }

    fun user(
        profile: Profile,
        fatigue: FatigueState,
        model: AthleteModel,
        activities: List<Activity>,
        races: List<Race>,
        workouts: List<Workout>,
        today: java.time.LocalDate,
    ): String = buildString {
        appendLine("Дата: $today")
        appendLine("Профиль: ${profile.sexLabel()}, ${profile.age ?: "?"} лет, рост ${profile.heightCm ?: "?"} см, вес ${profile.weightKg ?: "?"} кг.")
        appendLine("Пульс: покой ${profile.hrRest ?: "?"}, max бег ${profile.hrMaxRun ?: "?"}, max вело ${profile.hrMaxBike ?: "?"}, ПАНО бег ${profile.lthrRun ?: "?"}, ПАНО вело ${profile.lthrBike ?: "?"}.")
        appendLine("Темп ПАНО бега: ${Fmt.pace(profile.thresholdPaceSec?.toDouble())}, FTP: ${profile.ftpWatts ?: "?"} Вт.")
        appendLine()
        appendLine("Состояние: CTL ${Fmt.one(fatigue.ctl)}, ATL ${Fmt.one(fatigue.atl)}, TSB ${Fmt.signed(fatigue.tsb)}, готовность ${fatigue.readiness}/100 (${fatigue.label}).")
        appendLine("ACWR ${Fmt.one(fatigue.acwr)}, монотонность ${Fmt.one(fatigue.monotony)}, strain ${Fmt.one(fatigue.strain)}, риск ${fatigue.risk.name}.")
        appendLine("Нагрузка за 7 дней ${Fmt.one(fatigue.load7)}, за 28 дней ${Fmt.one(fatigue.load28)}. Пульс покоя: ${fatigue.restingHrDelta?.let { Fmt.signed(it.toDouble()) } ?: "нет данных"} к базе.")
        appendLine("Модель: ${model.summary}")
        appendLine("Сильные стороны: ${model.strengths.joinToString("; ")}")
        appendLine("Ограничения: ${model.limiters.joinToString("; ")}")
        appendLine()
        appendLine("Последние тренировки:")
        activities.take(10).forEach { a ->
            append("- ${a.date} ${a.sport.label}: ${Fmt.distance(a.distanceM)}, ${Fmt.duration(a.movingSec)}, ")
            append("нагрузка ${Fmt.load(a.load)}, пульс ${a.avgHr ?: "?"}/${a.maxHr ?: "?"}")
            if (a.sport == Sport.RUN) append(", темп ${Fmt.pace(a.avgSpeed?.let { 1000.0 / it })}")
            if (a.sport == Sport.BIKE) append(", скорость ${Fmt.speedKmh(a.avgSpeed)}")
            appendLine()
        }
        appendLine()
        if (races.isEmpty()) {
            appendLine("Старты не запланированы.")
        } else {
            appendLine("Календарь стартов:")
            races.filter { !it.date.isBefore(today) }.take(8).forEach {
                appendLine("- ${it.date} ${it.name} (${it.sport.label}), приоритет ${it.priority.key}, осталось ${it.daysLeft(today)} дн.")
            }
        }
        appendLine()
        appendLine("Сгенерированный план на ближайшие 7 дней:")
        workouts.filter { !it.date.isBefore(today) && it.date.isBefore(today.plusDays(7)) }.forEach {
            appendLine("- ${it.date} ${it.sport.label}, ${it.title}, ${it.durationMin} мин, ${it.target}, нагрузка ${Fmt.load(it.load)}")
        }
        appendLine()
        append("Дай анализ и скорректируй план с учётом усталости. Если риск высокий — предложи разгрузку.")
    }

    private fun Profile.sexLabel(): String = if (sex == "F") "женщина" else "мужчина"
}

/** Клиент для OpenAI-совместимых API: OpenRouter, Groq, Gemini (OpenAI endpoint), локальные серверы. */
class AiClient {
    fun chat(baseUrl: String, model: String, apiKey: String, system: String, user: String): String {
        if (apiKey.isBlank()) throw IOException("Не задан API-ключ ИИ")
        if (model.isBlank()) throw IOException("Не задана модель ИИ")
        val url = baseUrl.trim().trimEnd('/') + "/chat/completions"
        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.4)
            put("max_tokens", 900)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", system))
                    put(JSONObject().put("role", "user").put("content", user))
                }
            )
        }
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { s ->
                BufferedReader(InputStreamReader(s, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            if (code !in 200..299) {
                val detail = runCatching {
                    JSONObject(text).optJSONObject("error")?.optString("message")
                }.getOrNull() ?: text.take(240)
                throw IOException("ИИ вернул ошибку $code: $detail")
            }
            val json = JSONObject(text)
            val content = json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            if (content.isBlank()) throw IOException("ИИ вернул пустой ответ")
            return content.trim()
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Не удалось обратиться к ИИ: ${e.message}", e)
        } finally {
            conn.disconnect()
        }
    }

    fun ping(baseUrl: String, model: String, apiKey: String): String =
        chat(baseUrl, model, apiKey, "Ты отвечаешь кратко.", "Ответь одним словом: готов")
}

/** Локальный тренер: работает без внешних сервисов. */
object LocalCoach {
    fun analyze(
        profile: Profile,
        fatigue: FatigueState,
        model: AthleteModel,
        activities: List<Activity>,
        races: List<Race>,
        workouts: List<Workout>,
        today: java.time.LocalDate,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Оценка состояния")
        when {
            fatigue.risk == RiskLevel.HIGH ->
                sb.appendLine("Форма глубоко отрицательная: TSB ${Fmt.signed(fatigue.tsb)} при CTL ${Fmt.one(fatigue.ctl)}. Организм накопил усталость, и рост нагрузки сейчас даст больше вреда, чем пользы.")
            fatigue.risk == RiskLevel.MEDIUM ->
                sb.appendLine("Усталость умеренно повышена: TSB ${Fmt.signed(fatigue.tsb)}, ACWR ${Fmt.one(fatigue.acwr)}. Одна качественная тренировка в неделю допустима, остальное — легко.")
            fatigue.tsb > 12 ->
                sb.appendLine("Форма свежая: TSB ${Fmt.signed(fatigue.tsb)}. Это хорошее окно для качественной работы или контрольного старта.")
            else ->
                sb.appendLine("Нагрузка сбалансирована: CTL ${Fmt.one(fatigue.ctl)}, TSB ${Fmt.signed(fatigue.tsb)}. Можно продолжать текущую прогрессию.")
        }
        sb.appendLine("Готовность ${fatigue.readiness}/100 — ${fatigue.label.lowercase()}.")
        sb.appendLine()

        sb.appendLine("Риски и контроль")
        if (fatigue.acwr > 1.3) {
            sb.appendLine("• ACWR ${Fmt.one(fatigue.acwr)} — нагрузка растёт быстрее адаптации. Не увеличивайте объём на этой неделе.")
        }
        if ((fatigue.restingHrDelta ?: 0) >= 5) {
            sb.appendLine("• Пульс покоя выше базы на ${fatigue.restingHrDelta} уд/мин. Проверьте сон и самочувствие, возможна простуда.")
        }
        if (fatigue.monotony > 1.8) {
            sb.appendLine("• Высокая монотонность (${Fmt.one(fatigue.monotony)}): чередуйте лёгкие и качественные дни.")
        }
        if ((fatigue.sleepScore ?: 100.0) < 70) {
            sb.appendLine("• Сон ниже 70 баллов. Приоритет — 7–9 часов сна, а не дополнительная тренировка.")
        }
        sb.appendLine("• Не повышайте недельную нагрузку более чем на 5–8% относительно прошлой недели.")
        sb.appendLine()

        sb.appendLine("План на 7 дней")
        val next = workouts.filter { !it.date.isBefore(today) && it.date.isBefore(today.plusDays(7)) }
        if (next.isEmpty()) {
            sb.appendLine("• Сгенерируйте план в календаре — появятся конкретные тренировки с пульсовыми и темповыми целями.")
        } else {
            next.take(6).forEach { w ->
                sb.appendLine("• ${Fmt.weekday(w.date)}, ${Fmt.date(w.date)} — ${w.sport.label}: ${w.title}, ${w.durationMin} мин (${w.target}).")
            }
        }
        sb.appendLine()

        sb.appendLine("Главная рекомендация")
        val race = races.filter { !it.date.isBefore(today) }.minByOrNull { it.date }
        when {
            fatigue.risk == RiskLevel.HIGH -> sb.appendLine("Сделайте 3–4 дня только Z1–Z2 и вернитесь к качеству после снижения TSB до −10.")
            race != null && race.daysLeft(today) <= 14 ->
                sb.appendLine("До старта «${race.name}» ${race.daysLeft(today)} дн. Сокращайте объём, сохраняя короткие ускорения, и не добавляйте новые нагрузки.")
            profile.thresholdPaceSec == null ->
                sb.appendLine("Уточните темп ПАНО в профиле — тогда темповые цели станут точными. Пока ориентируйтесь на дыхание и зону Z4.")
            else ->
                sb.appendLine("Держите 80% времени в Z1–Z2 и одну качественную сессию в неделю. Регулярность важнее разовых подвигов.")
        }
        sb.appendLine()
        sb.appendLine("Анализ построен встроенным тренером по вашим данным intervals.icu. Для персонального разбора подключите бесплатную модель ИИ в профиле.")
        return sb.toString().trim()
    }
}
