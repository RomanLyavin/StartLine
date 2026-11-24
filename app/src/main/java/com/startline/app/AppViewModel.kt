package com.startline.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.startline.app.data.Activity
import com.startline.app.data.ActivityStreams
import com.startline.app.data.IntervalsClient
import com.startline.app.data.MailConfig
import com.startline.app.data.Profile
import com.startline.app.data.Race
import com.startline.app.data.Repository
import com.startline.app.data.Sport
import com.startline.app.data.User
import com.startline.app.data.WatchConnection
import com.startline.app.data.WatchProvider
import com.startline.app.data.WellnessDay
import com.startline.app.data.Workout
import com.startline.app.domain.AthleteModel
import com.startline.app.domain.AthleteModelBuilder
import com.startline.app.domain.FatigueEngine
import com.startline.app.domain.FatigueState
import com.startline.app.domain.TaperEngine
import com.startline.app.domain.TaperPlan
import com.startline.app.update.ApkVerifier
import com.startline.app.update.GitHubUpdate
import com.startline.app.update.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = Repository(application)

    var user by mutableStateOf<User?>(null)
        private set
    var profile by mutableStateOf<Profile?>(null)
        private set
    var activities by mutableStateOf<List<Activity>>(emptyList())
        private set
    var wellness by mutableStateOf<List<WellnessDay>>(emptyList())
        private set
    var races by mutableStateOf<List<Race>>(emptyList())
        private set
    var workouts by mutableStateOf<List<Workout>>(emptyList())
        private set
    var fatigue by mutableStateOf(FatigueState())
        private set
    var model by mutableStateOf(AthleteModel())
        private set
    var analysis by mutableStateOf<Pair<Long, String>?>(null)
        private set
    var lastSyncAt by mutableStateOf(0L)
        private set
    var athleteName by mutableStateOf("")
        private set

    var onboarded by mutableStateOf(true)
        private set
    var authLoading by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var syncing by mutableStateOf(false)
        private set
    var generating by mutableStateOf(false)
        private set
    var aiLoading by mutableStateOf(false)
        private set
    var aiUsedModel by mutableStateOf(false)
        private set
    var streamsLoading by mutableStateOf(false)
        private set
    var streams by mutableStateOf<ActivityStreams?>(null)
        private set
    var streamsError by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var updateState by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set
    var taperPlan by mutableStateOf<TaperPlan>(TaperEngine.plan(emptyList()))
        private set
    var watchConnections by mutableStateOf<List<WatchConnection>>(emptyList())
        private set
    var watchSyncBusy by mutableStateOf(false)
        private set
    var mailSettings by mutableStateOf(MailConfig("", 587, "", "", ""))
        private set

    private var updateBusy = false
    private val appVersion: String by lazy {
        try {
            val app = getApplication<Application>()
            app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "0"
        } catch (e: Exception) {
            "0"
        }
    }

    private var autoSyncAttempted = false

    init {
        mailSettings = repo.mailConfig()
        user = repo.currentUser()
        if (user != null) {
            loading = true
            // Загрузка данных (SQLite + Android Keystore) не должна блокировать
            // главный поток: иначе первый кадр не рисуется и экран выглядит зависшим.
            val u = user!!
            viewModelScope.launch(Dispatchers.IO) { loadAllInternal(u) }
        }
    }

    fun consumeMessage() {
        message = null
    }

    fun clearAuthError() {
        authError = null
    }

    // ---------- почта (SMTP) ----------

    fun saveMailSettings(config: MailConfig) {
        repo.saveMailSettings(config)
        mailSettings = config
        message = "Настройки почты сохранены"
    }

    fun sendTestMail() {
        if (!mailSettings.isConfigured) {
            message = "Заполните настройки почты"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.sendTestMail()
            message = if (result.isSuccess) {
                "Тестовое письмо отправлено на ${mailSettings.from}"
            } else {
                "Письмо не отправлено: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun login(email: String, password: String) {
        authLoading = true
        authError = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val u = repo.login(email, password)
                user = u
                loadAllInternal(u)
            } catch (e: Exception) {
                authError = e.message ?: "Не удалось войти"
            } finally {
                authLoading = false
            }
        }
    }

    fun register(email: String, password: String) {
        authLoading = true
        authError = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repo.register(email, password)
                user = result.user
                loadAllInternal(result.user)
                message = if (result.mailSent) {
                    "Пароль отправлен на ${result.user.email}"
                } else {
                    "Аккаунт создан, но письмо не отправлено: ${result.mailError}"
                }
            } catch (e: Exception) {
                authError = e.message ?: "Не удалось зарегистрироваться"
            } finally {
                authLoading = false
            }
        }
    }

    fun logout() {
        repo.logout()
        user = null
        profile = null
        activities = emptyList()
        wellness = emptyList()
        races = emptyList()
        workouts = emptyList()
        fatigue = FatigueState()
        model = AthleteModel()
        analysis = null
        onboarded = true
    }

    fun saveProfile(newProfile: Profile, markOnboarded: Boolean = false) {
        val u = user ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.saveProfile(newProfile.copy(userId = u.id))
                if (markOnboarded) repo.setOnboarded(true)
                loadAllInternal(u)
                if (markOnboarded) onboarded = true
                message = "Профиль сохранён"
            } catch (e: Exception) {
                message = e.message ?: "Не удалось сохранить профиль"
            }
        }
    }

    fun saveProfileAndSync(newProfile: Profile) {
        val u = user ?: return
        if (syncing) return
        syncing = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.saveProfile(newProfile.copy(userId = u.id))
                loadAllInternal(u)
                val result = repo.sync(u.id)
                if (repo.workouts(u.id).isEmpty()) repo.generatePlan(u.id)
                loadAllInternal(u)
                message = "Профиль сохранён. Загружено ${result.activityCount} тренировок"
            } catch (e: Exception) {
                message = e.message ?: "Ошибка синхронизации"
            } finally {
                syncing = false
            }
        }
    }

    fun skipOnboarding() {
        repo.setOnboarded(true)
        onboarded = true
    }

    /** Сохраняет дни тренировок и сразу пересобирает план под новое расписание. */
    fun saveSchedule(newProfile: Profile) {
        val u = user ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.saveProfile(newProfile.copy(userId = u.id))
                repo.generatePlan(u.id)
                loadAllInternal(u)
                message = "Расписание сохранено, план пересобран"
            } catch (e: Exception) {
                message = e.message ?: "Не удалось сохранить расписание"
            }
        }
    }

    fun checkUpdates() {
        if (updateBusy) return
        updateBusy = true
        updateState = UpdateState.Checking
        val current = appVersion
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val release = GitHubUpdate.fetchLatest(current)
                updateState = when {
                    !GitHubUpdate.isNewer(release.version, current) -> UpdateState.Latest(current)
                    release.apkUrl.isNullOrBlank() -> UpdateState.Error("В релизе ${release.version} нет APK")
                    else -> UpdateState.Available(release.version, release.apkUrl, release.apkSize)
                }
            } catch (e: Exception) {
                updateState = UpdateState.Error(e.message ?: "Не удалось проверить обновления")
            } finally {
                updateBusy = false
            }
        }
    }

    fun downloadUpdate() {
        val available = updateState as? UpdateState.Available ?: return
        if (updateBusy) return
        updateBusy = true
        updateState = UpdateState.Downloading(0)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val dir = File(app.cacheDir, "updates").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val target = File(dir, "StartLine-${available.version}.apk")
                GitHubUpdate.downloadApk(available.apkUrl, target, available.size, appVersion) { percent ->
                    updateState = UpdateState.Downloading(percent)
                }
                val downloadedVersion = ApkVerifier.versionName(app, target)
                if (!ApkVerifier.isSameApp(app, target) || downloadedVersion != available.version) {
                    target.delete()
                    updateState = UpdateState.Error("Скачанный файл не подходит для обновления")
                } else {
                    updateState = UpdateState.Ready(available.version, target)
                }
            } catch (e: Exception) {
                updateState = UpdateState.Error(e.message ?: "Не удалось скачать обновление")
            } finally {
                updateBusy = false
            }
        }
    }

    fun resetUpdateState() {
        if (!updateBusy) updateState = UpdateState.Idle
    }

    fun currentVersionName(): String = appVersion

    fun syncNow() {
        val u = user ?: return
        if (syncing) return
        val p = profile
        if (p == null || p.intervalsKey.isBlank()) {
            message = "Сначала укажите API-ключ intervals.icu в профиле"
            return
        }
        syncing = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repo.sync(u.id)
                if (repo.workouts(u.id).isEmpty()) repo.generatePlan(u.id)
                loadAllInternal(u)
                message = "Синхронизировано: ${result.activityCount} тренировок, ${result.wellnessCount} дней wellness" +
                    if (result.watchCount > 0) ", из часов: ${result.watchCount}" else ""
            } catch (e: Exception) {
                message = e.message ?: "Ошибка синхронизации"
            } finally {
                syncing = false
            }
        }
    }

    fun maybeAutoSync() {
        if (autoSyncAttempted) return
        autoSyncAttempted = true
        val p = profile ?: return
        if (p.intervalsKey.isNotBlank() && System.currentTimeMillis() - repo.lastSyncAt() > 6 * 3600_000L) {
            syncNow()
        }
    }

    fun addRace(race: Race) {
        val u = user ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.addRace(race.copy(userId = u.id))
                repo.generatePlan(u.id)
                loadAllInternal(u)
                message = "Старт добавлен, план пересобран"
            } catch (e: Exception) {
                message = e.message ?: "Не удалось добавить старт"
            }
        }
    }

    fun deleteRace(id: Long) {
        val u = user ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.deleteRace(u.id, id)
                repo.generatePlan(u.id)
                loadAllInternal(u)
                message = "Старт удалён"
            } catch (e: Exception) {
                message = e.message ?: "Не удалось удалить старт"
            }
        }
    }

    fun generatePlan() {
        val u = user ?: return
        if (generating) return
        generating = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.generatePlan(u.id)
                loadAllInternal(u)
                message = "План на 4 недели готов"
            } catch (e: Exception) {
                message = e.message ?: "Не удалось собрать план"
            } finally {
                generating = false
            }
        }
    }

    fun analyze() {
        val u = user ?: return
        if (aiLoading) return
        aiLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repo.analyze(u.id)
                analysis = System.currentTimeMillis() to result.text
                aiUsedModel = result.usedAi
                if (!result.usedAi && result.text.startsWith("Не удалось")) {
                    message = "ИИ недоступен, показан встроенный анализ"
                }
            } catch (e: Exception) {
                message = e.message ?: "Ошибка анализа"
            } finally {
                aiLoading = false
            }
        }
    }

    fun testAi(candidate: Profile) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val answer = repo.pingAi(candidate)
                message = "ИИ ответил: ${answer.take(80)}"
            } catch (e: Exception) {
                message = "Проверка ИИ: ${e.message}"
            }
        }
    }

    fun openActivity(activityId: String) {
        streams = null
        streamsError = null
        streamsLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            val p = profile
            if (p == null || p.intervalsKey.isBlank()) {
                streamsError = "Нет API-ключа intervals.icu"
                streamsLoading = false
                return@launch
            }
            try {
                streams = IntervalsClient(p.intervalsKey, p.intervalsId.ifBlank { "0" })
                    .fetchStreams(activityId)
            } catch (e: Exception) {
                streamsError = e.message ?: "Не удалось загрузить пульсовой ряд"
            } finally {
                streamsLoading = false
            }
        }
    }

    fun closeActivity() {
        streams = null
        streamsError = null
        streamsLoading = false
    }

    fun activityById(id: String): Activity? = activities.firstOrNull { it.id == id }

    fun activitiesFor(sport: Sport?): List<Activity> =
        if (sport == null) activities else activities.filter { it.sport == sport }

    // ---------- тейпер и сервисы часов (пункты 2.1–2.4) ----------

    fun watchAuthUrl(provider: WatchProvider): String = repo.watchAuthUrl(provider)

    fun disconnectWatch(provider: WatchProvider) {
        repo.disconnectWatch(provider)
        watchConnections = repo.watchConnections()
    }

    /** Deep-link startline://oauth/watch?code=… — обмен кода авторизации на токен. */
    fun handleWatchCallback(uri: Uri): Boolean {
        val provider = repo.handleWatchCallback(uri) ?: return false
        watchConnections = repo.watchConnections()
        message = "«${provider.label}» подключено"
        return true
    }

    /** Загружает тренировки одного подключённого сервиса. */
    fun syncWatch(provider: WatchProvider) {
        val u = user ?: return
        if (watchSyncBusy) return
        watchSyncBusy = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repo.syncWatch(u.id, provider)
                watchConnections = repo.watchConnections()
                loadAllInternal(u)
                message = "${provider.label}: загружено ${result.activityCount} тренировок"
            } catch (e: Exception) {
                message = e.message ?: "Не удалось загрузить тренировки"
            } finally {
                watchSyncBusy = false
            }
        }
    }

    // ---------- резервная копия настроек (пункт 4.2) ----------

    fun backupFiles(): List<File> = repo.backups()

    fun exportBackup(includeKeys: Boolean = true) {
        val u = user ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = repo.exportBackup(u.id, includeKeys)
                message = "Резервная копия сохранена: ${file.name}"
            } catch (e: Exception) {
                message = e.message ?: "Не удалось сохранить резервную копию"
            }
        }
    }

    fun importBackup(file: File) {
        val u = user ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.importBackup(u.id, file)
                loadAllInternal(u)
                message = "Настройки восстановлены из ${file.name}"
            } catch (e: Exception) {
                message = e.message ?: "Не удалось восстановить настройки"
            }
        }
    }

    private fun loadAllInternal(u: User) {
        try {
            val p = repo.profile(u.id)
            val acts = repo.activities(u.id, null, 400)
            val w = repo.wellness(u.id, 120)
            val r = repo.races(u.id)
            val today = LocalDate.now()
            val fat = FatigueEngine.compute(acts, w, today)
            val mod = AthleteModelBuilder.build(p, acts, fat, today)
            val wks = repo.workouts(u.id)
            val a = repo.analysis(u.id)

            profile = p
            activities = acts
            wellness = w
            races = r
            workouts = wks
            fatigue = fat
            model = mod
            analysis = a
            lastSyncAt = repo.lastSyncAt()
            athleteName = repo.athleteName()
            taperPlan = TaperEngine.plan(r)
            watchConnections = repo.watchConnections()
            onboarded = repo.isOnboarded() || p.isFilled
        } catch (e: Exception) {
            message = e.message ?: "Ошибка загрузки данных"
        } finally {
            loading = false
        }
    }

    fun refresh() {
        val u = user ?: return
        viewModelScope.launch(Dispatchers.IO) { loadAllInternal(u) }
    }
}
