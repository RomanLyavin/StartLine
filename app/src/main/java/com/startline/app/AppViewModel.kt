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
import com.startline.app.data.Profile
import com.startline.app.data.Race
import com.startline.app.data.Repository
import com.startline.app.data.Sport
import com.startline.app.data.User
import com.startline.app.data.WatchConnection
import com.startline.app.data.WatchOAuthManager
import com.startline.app.data.WatchProvider
import com.startline.app.data.WellnessDay
import com.startline.app.data.Workout
import com.startline.app.domain.AthleteModel
import com.startline.app.domain.AthleteModelBuilder
import com.startline.app.domain.FatigueEngine
import com.startline.app.domain.FatigueState
import com.startline.app.domain.TaperEngine
import com.startline.app.domain.TaperPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = Repository(application)
    private val watchManager = WatchOAuthManager(application)

    var watchConnections by mutableStateOf<List<WatchConnection>>(watchManager.connections())
        private set

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
    var taperPlan by mutableStateOf<TaperPlan>(TaperEngine.plan(emptyList()))
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

    private var autoSyncAttempted = false

    init {
        user = repo.currentUser()
        if (user != null) {
            loading = true
            loadAllInternal(user!!)
        }
    }

    fun consumeMessage() {
        message = null
    }

    fun clearAuthError() {
        authError = null
    }

    /** URL для открытия OAuth-авторизации производителя часов. */
    fun watchAuthUrl(provider: WatchProvider): String =
        watchManager.authorizationUrl(provider, state = provider.key)

    /** Обработка deep-link callback после авторизации. */
    fun handleWatchCallback(uri: Uri) {
        if (uri.scheme != "startline") return
        val connected = watchManager.handleCallback(uri)
        watchConnections = watchManager.connections()
        message = if (connected != null) {
            "${connected.label}: часы подключены"
        } else {
            "Не удалось подключить часы. Повторите попытку."
        }
    }

    fun disconnectWatch(provider: WatchProvider) {
        watchManager.disconnect(provider)
        watchConnections = watchManager.connections()
        message = "${provider.label}: часы отключены"
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
                val u = repo.register(email, password)
                user = u
                loadAllInternal(u)
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
        taperPlan = TaperEngine.plan(emptyList())
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
                message = "Синхронизировано: ${result.activityCount} тренировок, ${result.wellnessCount} дней wellness"
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
            taperPlan = TaperEngine.plan(r)
            workouts = wks
            fatigue = fat
            model = mod
            analysis = a
            lastSyncAt = repo.lastSyncAt()
            athleteName = repo.athleteName()
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
