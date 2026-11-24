package com.startline.app.data

import android.content.Context
import android.net.Uri
import com.startline.app.BuildConfig
import com.startline.app.ai.AiClient
import com.startline.app.ai.AiPrompt
import com.startline.app.ai.AiResult
import com.startline.app.ai.LocalCoach
import com.startline.app.domain.AthleteModelBuilder
import com.startline.app.domain.FatigueEngine
import com.startline.app.domain.PlanEngine
import java.io.File
import java.time.LocalDate

data class SyncResult(
    val activityCount: Int,
    val wellnessCount: Int,
    val athleteName: String,
    val watchCount: Int = 0,
)

/** Результат регистрации: письмо с паролем могло не пройти (SMTP недоступен). */
data class RegistrationResult(
    val user: User,
    val mailSent: Boolean,
    val mailError: String?,
)

class Repository(context: Context) {
    private val appContext = context.applicationContext
    private val db = Db(appContext)
    private val secrets = SecretStore()
    private val prefs = appContext.getSharedPreferences("startline", Context.MODE_PRIVATE)
    private val watchOAuth = WatchOAuthManager(appContext)
    private val watchSync = WatchSync(watchOAuth)
    private val mailConfig = MailConfig(
        host = BuildConfig.MAIL_HOST,
        port = BuildConfig.MAIL_PORT,
        user = BuildConfig.MAIL_USER,
        password = BuildConfig.MAIL_PASSWORD,
        from = BuildConfig.MAIL_FROM,
        useStartTls = BuildConfig.MAIL_STARTTLS,
    )

    fun mailConfig(): MailConfig = mailConfig

    // ---------- session ----------

    fun currentUser(): User? {
        val id = prefs.getLong("session_user_id", 0L)
        if (id <= 0L) return null
        return db.userById(id)
    }

    fun register(email: String, password: String): RegistrationResult {
        val normalized = email.trim().lowercase()
        require(normalized.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) { "Введите корректный email" }
        require(password.length >= 6) { "Пароль должен быть не короче 6 символов" }
        if (db.findUser(normalized) != null) throw IllegalArgumentException("Аккаунт с таким email уже существует")
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash(password, salt)
        val user = db.createUser(normalized, hash, salt) ?: throw IllegalStateException("Не удалось создать аккаунт")
        db.saveProfile(Profile(userId = user.id))
        prefs.edit().putLong("session_user_id", user.id).apply()

        // Письмо с паролем. Регистрация не отменяется, если SMTP недоступен:
        // аккаунт уже создан, и пользователь узнаёт об этом отдельным сообщением.
        val send = runCatching {
            MailSender(mailConfig).send(normalized, WelcomeMail.SUBJECT, WelcomeMail.body(normalized, password))
        }
        return RegistrationResult(user, send.isSuccess, send.exceptionOrNull()?.message)
    }

    fun login(email: String, password: String): User {
        val normalized = email.trim().lowercase()
        val record = db.findUser(normalized) ?: throw IllegalArgumentException("Аккаунт не найден")
        if (!PasswordHasher.verify(password, record.salt, record.passwordHash)) {
            throw IllegalArgumentException("Неверный пароль")
        }
        prefs.edit().putLong("session_user_id", record.user.id).apply()
        return record.user
    }

    fun logout() {
        prefs.edit().remove("session_user_id").apply()
    }

    fun isOnboarded(): Boolean = prefs.getBoolean("onboarded", false)
    fun setOnboarded(value: Boolean) {
        prefs.edit().putBoolean("onboarded", value).apply()
    }

    // ---------- profile ----------

    fun profile(userId: Long): Profile {
        val stored = db.profile(userId) ?: Profile(userId = userId)
        return stored.copy(
            intervalsKey = secrets.decrypt(stored.intervalsKey),
            aiKey = secrets.decrypt(stored.aiKey),
        )
    }

    fun saveProfile(profile: Profile) {
        val stored = profile.copy(
            intervalsKey = secrets.encrypt(profile.intervalsKey.trim()),
            aiKey = secrets.encrypt(profile.aiKey.trim()),
            intervalsId = profile.intervalsId.trim().ifBlank { "0" },
        )
        db.saveProfile(stored)
    }

    fun lastSyncAt(): Long = prefs.getLong("last_sync", 0L)
    fun athleteName(): String = prefs.getString("athlete_name", "").orEmpty()

    // ---------- data reads ----------

    fun activities(userId: Long, sport: Sport? = null, limit: Int = 400): List<Activity> =
        db.activities(userId, sport, limit)

    fun activity(userId: Long, id: String): Activity? = db.activity(userId, id)

    fun wellness(userId: Long, limit: Int = 120): List<WellnessDay> = db.wellness(userId, limit)

    fun races(userId: Long): List<Race> = db.races(userId)

    fun addRace(race: Race): Long = db.addRace(race)

    fun deleteRace(userId: Long, raceId: Long) = db.deleteRace(userId, raceId)

    fun workouts(userId: Long): List<Workout> = db.workouts(userId)

    fun analysis(userId: Long): Pair<Long, String>? = db.analysis(userId)

    // ---------- sync ----------

    @Synchronized
    fun sync(userId: Long): SyncResult {
        val profile = profile(userId)
        if (profile.intervalsKey.isBlank()) {
            throw IllegalStateException("Укажите API-ключ intervals.icu в профиле")
        }
        val client = IntervalsClient(profile.intervalsKey, profile.intervalsId.ifBlank { "0" })
        val today = LocalDate.now()
        val athlete = client.fetchAthlete()
        val activities = client.fetchActivities(userId, today.minusDays(365), today, profile)
        val wellness = client.fetchWellness(userId, today.minusDays(120), today)

        db.upsertActivities(activities)
        db.upsertWellness(wellness, userId)
        val watchCount = syncWatchActivities(userId, profile, today)

        val updated = profile.copy(
            name = profile.name.ifBlank { athlete.name },
            weightKg = profile.weightKg ?: athlete.weightKg,
            hrRest = profile.hrRest ?: athlete.restingHr,
        )
        if (updated != profile) saveProfile(updated)

        prefs.edit()
            .putLong("last_sync", System.currentTimeMillis())
            .putString("athlete_name", athlete.name)
            .apply()

        return SyncResult(activities.size, wellness.size, athlete.name, watchCount)
    }

    // ---------- сервисы часов (пункты 2.1–2.4) ----------

    /** Пункт 4.1: модуль синхронизации с внешними сервисами. */
    private fun syncWatchActivities(userId: Long, profile: Profile, today: LocalDate): Int {
        var total = 0
        for (connection in watchOAuth.connections()) {
            if (!connection.connected) continue
            try {
                val list = watchSync.fetchActivities(userId, connection.provider, profile, today.minusDays(120), today)
                db.upsertActivities(list)
                total += list.size
            } catch (e: Exception) {
                // Сервис недоступен: не блокируем основную синхронизацию, но показываем причину.
                prefs.edit().putString("watch_error_${connection.provider.key}", e.message ?: "Ошибка").apply()
            }
        }
        return total
    }

    fun syncWatch(userId: Long, provider: WatchProvider): WatchSyncResult {
        val profile = profile(userId)
        val today = LocalDate.now()
        val activities = watchSync.fetchActivities(userId, provider, profile, today.minusDays(120), today)
        db.upsertActivities(activities)
        return WatchSyncResult(provider, activities.size)
    }

    fun watchConnections(): List<WatchConnection> = watchOAuth.connections()

    fun watchAuthUrl(provider: WatchProvider): String = watchOAuth.authorizationUrl(provider)

    fun disconnectWatch(provider: WatchProvider) = watchOAuth.disconnect(provider)

    fun handleWatchCallback(uri: Uri): WatchProvider? = watchOAuth.handleCallback(uri)

    fun watchError(provider: WatchProvider): String? = prefs.getString("watch_error_${provider.key}", null)

    // ---------- резервная копия настроек (пункт 4.2) ----------

    private val backupDir = File(appContext.filesDir, "backups").apply { mkdirs() }

    /** Сохраняет настройки в JSON-файл во внутренней памяти приложения. */
    fun exportBackup(userId: Long, includeKeys: Boolean = true): File {
        val json = SettingsBackup.exportJson(profile(userId), includeKeys)
        val file = File(backupDir, "startline-settings-${LocalDate.now()}.json")
        file.writeText(json, Charsets.UTF_8)
        return file
    }

    /** Восстанавливает настройки из файла резервной копии. */
    fun importBackup(userId: Long, file: File): Profile {
        val restored = SettingsBackup.importJson(file.readText(Charsets.UTF_8))
        saveProfile(restored.copy(userId = userId))
        return profile(userId)
    }

    fun backups(): List<File> = backupDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

    // ---------- model / plan ----------

    fun fatigue(userId: Long, today: LocalDate = LocalDate.now()) =
        FatigueEngine.compute(activities(userId, null, 400), wellness(userId, 120), today)

    fun model(userId: Long, today: LocalDate = LocalDate.now()) =
        AthleteModelBuilder.build(
            profile(userId),
            activities(userId, null, 400),
            fatigue(userId, today),
            today,
        )

    @Synchronized
    fun generatePlan(userId: Long): List<Workout> {
        val profile = profile(userId)
        val today = LocalDate.now()
        val activities = activities(userId, null, 400)
        val fatigue = FatigueEngine.compute(activities, wellness(userId, 120), today)
        val races = races(userId)
        val plan = PlanEngine.generate(profile, fatigue, races, today)
        db.replaceWorkouts(userId, plan)
        return db.workouts(userId)
    }

    // ---------- AI ----------

    fun analyze(userId: Long): AiResult {
        val profile = profile(userId)
        val today = LocalDate.now()
        val activities = activities(userId, null, 60)
        val fatigue = FatigueEngine.compute(activities(userId, null, 400), wellness(userId, 120), today)
        val model = AthleteModelBuilder.build(profile, activities(userId, null, 400), fatigue, today)
        val races = races(userId)
        val workouts = workouts(userId)

        val local = LocalCoach.analyze(profile, fatigue, model, activities, races, workouts, today)
        val result = if (profile.aiKey.isBlank() || profile.aiModel.isBlank()) {
            AiResult(local, usedAi = false)
        } else {
            try {
                val text = AiClient().chat(
                    baseUrl = profile.aiBaseUrl,
                    model = profile.aiModel,
                    apiKey = profile.aiKey,
                    system = AiPrompt.system(),
                    user = AiPrompt.user(profile, fatigue, model, activities, races, workouts, today),
                )
                AiResult(text, usedAi = true, model = profile.aiModel)
            } catch (e: Exception) {
                AiResult(
                    "Не удалось получить ответ ИИ: ${e.message}\n\n" + local,
                    usedAi = false,
                )
            }
        }
        db.saveAnalysis(userId, result.text)
        return result
    }

    fun pingAi(profile: Profile): String {
        if (profile.aiKey.isBlank()) throw IllegalStateException("Сначала вставьте API-ключ")
        return AiClient().ping(profile.aiBaseUrl, profile.aiModel, profile.aiKey)
    }
}
