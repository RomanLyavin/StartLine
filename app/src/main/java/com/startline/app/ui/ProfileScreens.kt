package com.startline.app.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.startline.app.AppViewModel
import com.startline.app.data.Fmt
import com.startline.app.data.Profile

@Composable
fun ProfileForm(
    initial: Profile,
    email: String,
    syncing: Boolean,
    lastSyncAt: Long,
    athleteName: String,
    saveLabel: String,
    onSave: (Profile) -> Unit,
    onSaveAndSync: (Profile) -> Unit,
    onTestAi: (Profile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var height by remember(initial) { mutableStateOf(initial.heightCm?.toString() ?: "") }
    var weight by remember(initial) { mutableStateOf(initial.weightKg?.let { Fmt.one(it) } ?: "") }
    var age by remember(initial) { mutableStateOf(initial.age?.toString() ?: "") }
    var sex by remember(initial) { mutableStateOf(if (initial.sex == "F") "F" else "M") }
    var hrMaxRun by remember(initial) { mutableStateOf(initial.hrMaxRun?.toString() ?: "") }
    var hrMaxBike by remember(initial) { mutableStateOf(initial.hrMaxBike?.toString() ?: "") }
    var hrRest by remember(initial) { mutableStateOf(initial.hrRest?.toString() ?: "") }
    var lthrRun by remember(initial) { mutableStateOf(initial.lthrRun?.toString() ?: "") }
    var lthrBike by remember(initial) { mutableStateOf(initial.lthrBike?.toString() ?: "") }
    var ftp by remember(initial) { mutableStateOf(initial.ftpWatts?.toString() ?: "") }
    var pace by remember(initial) {
        mutableStateOf(initial.thresholdPaceSec?.let { Fmt.paceShort(it) } ?: "")
    }
    var intervalsId by remember(initial) { mutableStateOf(initial.intervalsId.ifBlank { "0" }) }
    var intervalsKey by remember(initial) { mutableStateOf(initial.intervalsKey) }
    var aiBase by remember(initial) { mutableStateOf(initial.aiBaseUrl) }
    var aiModel by remember(initial) { mutableStateOf(initial.aiModel) }
    var aiKey by remember(initial) { mutableStateOf(initial.aiKey) }
    var error by remember { mutableStateOf<String?>(null) }

    fun collect(): Profile? {
        error = null
        if (name.isBlank()) {
            error = "Укажите имя"
            return null
        }
        val weightValue = weight.replace(',', '.').toDoubleOrNull()
        if (weight.isNotBlank() && (weightValue == null || weightValue !in 25.0..250.0)) {
            error = "Проверьте вес (25–250 кг)"
            return null
        }
        fun intValue(v: String, label: String, min: Int, max: Int): Int? {
            if (v.isBlank()) return null
            val parsed = v.filter { it.isDigit() }.toIntOrNull()
            if (parsed == null || parsed !in min..max) {
                error = "Проверьте поле «$label»"
                return null
            }
            return parsed
        }
        val paceSec = parsePace(pace)
        if (pace.isNotBlank() && paceSec == null) {
            error = "Темп ПАНО укажите как 4:00 (мин:сек на км)"
            return null
        }
        val hrRun = intValue(hrMaxRun, "Макс. пульс бег", 100, 230) ?: return null
        val hrBike = intValue(hrMaxBike, "Макс. пульс вело", 100, 230) ?: return null
        val hrRestValue = intValue(hrRest, "Пульс покоя", 30, 120) ?: return null
        val lthrRunValue = intValue(lthrRun, "ПАНО бег", 100, 230) ?: return null
        val lthrBikeValue = intValue(lthrBike, "ПАНО вело", 100, 230) ?: return null
        val heightValue = intValue(height, "Рост", 100, 250)
        if (height.isNotBlank() && heightValue == null) return null
        val ageValue = intValue(age, "Возраст", 10, 100)
        if (age.isNotBlank() && ageValue == null) return null
        val ftpValue = intValue(ftp, "FTP", 50, 600)
        if (ftp.isNotBlank() && ftpValue == null) return null
        return initial.copy(
            name = name.trim(),
            heightCm = heightValue,
            weightKg = weightValue,
            age = ageValue,
            sex = sex,
            hrMaxRun = hrRun,
            hrMaxBike = hrBike,
            hrRest = hrRestValue,
            lthrRun = lthrRunValue,
            lthrBike = lthrBikeValue,
            thresholdPaceSec = paceSec,
            ftpWatts = ftpValue,
            intervalsId = intervalsId.trim().ifBlank { "0" },
            intervalsKey = intervalsKey.trim(),
            aiBaseUrl = aiBase.trim(),
            aiModel = aiModel.trim(),
            aiKey = aiKey.trim(),
        )
    }

    val lastSyncText = if (lastSyncAt > 0) {
        DateUtils.getRelativeTimeSpanString(
            lastSyncAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    } else "ещё не синхронизировано"

    Column(modifier = modifier.fillMaxWidth()) {
        CardBox {
            SectionLabel("Личные данные")
            Spacer(Modifier.height(10.dp))
            Field("Имя", name, { name = it }, placeholder = "Как к вам обращаться")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Рост, см", height, { height = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
                Field("Вес, кг", weight, { weight = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                Field("Возраст", age, { age = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
                Column(modifier = Modifier.weight(1.4f)) {
                    Text("Пол", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectChip("Мужской", sex == "M", { sex = "M" })
                        SelectChip("Женский", sex == "F", { sex = "F" })
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Email: $email", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            SectionLabel("Пульс и пороги")
            Spacer(Modifier.height(4.dp))
            Text(
                "ПАНО — пульс и темп на анаэробном пороге. Эти значения задают зоны для плана.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Макс. пульс, бег", hrMaxRun, { hrMaxRun = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
                Field("Макс. пульс, вело", hrMaxBike, { hrMaxBike = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Пульс покоя", hrRest, { hrRest = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
                Field("ПАНО бег", lthrRun, { lthrRun = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
                Field("ПАНО вело", lthrBike, { lthrBike = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(
                    "Темп ПАНО, бег",
                    pace,
                    { pace = it },
                    Modifier.weight(1f),
                    placeholder = "4:00",
                    supporting = "мин:сек на км",
                )
                Field("FTP, Вт", ftp, { ftp = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            SectionLabel("intervals.icu")
            Spacer(Modifier.height(8.dp))
            Text(
                "Ключ: intervals.icu → Settings → Developer → API key. Атлет ID можно оставить 0 — тогда берётся владелец ключа.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Field("API-ключ", intervalsKey, { intervalsKey = it }, password = true, placeholder = "ваш ключ intervals.icu")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Field("ID атлета", intervalsId, { intervalsId = it }, Modifier.weight(1f), placeholder = "0")
                Column(modifier = Modifier.weight(1.6f)) {
                    Text(
                        if (athleteName.isBlank()) "Статус: $lastSyncText"
                        else "Атлет: $athleteName · $lastSyncText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (lastSyncAt > 0) Lime else TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                "Сохранить и синхронизировать",
                onClick = { collect()?.let { onSaveAndSync(it) } },
                loading = syncing,
            )
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            SectionLabel("ИИ-тренер")
            Spacer(Modifier.height(8.dp))
            Text(
                "Работает с любым OpenAI-совместимым API: OpenRouter, Groq, Gemini (OpenAI endpoint), локальный сервер. Без ключа используется встроенный тренер.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Field("Базовый URL", aiBase, { aiBase = it }, placeholder = "https://openrouter.ai/api/v1")
            Spacer(Modifier.height(10.dp))
            Field("Модель", aiModel, { aiModel = it }, placeholder = "deepseek/deepseek-chat-v3-0324:free")
            Spacer(Modifier.height(10.dp))
            Field("API-ключ ИИ", aiKey, { aiKey = it }, password = true, placeholder = "необязательно")
            Spacer(Modifier.height(10.dp))
            GhostButton("Проверить подключение", onClick = { collect()?.let { onTestAi(it) } })
        }

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, color = Red, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            saveLabel,
            onClick = { collect()?.let { onSave(it) } },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Ключи хранятся в зашифрованном виде. Пароль аккаунта не покидает устройство.",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    onOpenSchedule: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenTaper: () -> Unit = {},
    onOpenWatches: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
) {
    val profile = vm.profile
    if (profile == null) {
        LoadingBlock()
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        ScreenHeader(
            title = "Профиль",
            subtitle = vm.user?.email,
            actions = {
                GhostButton("Выйти", onClick = { vm.logout() })
            },
        )
        ProfileForm(
            initial = profile,
            email = vm.user?.email ?: "",
            syncing = vm.syncing,
            lastSyncAt = vm.lastSyncAt,
            athleteName = vm.athleteName,
            saveLabel = "Сохранить профиль",
            onSave = { vm.saveProfile(it) },
            onSaveAndSync = { vm.saveProfileAndSync(it) },
            onTestAi = { vm.testAi(it) },
        )
        Spacer(Modifier.height(4.dp))
        CardBox {
            SectionLabel("Настройки")
            Spacer(Modifier.height(6.dp))
            Column {
                GhostButton("Расписание тренировок", onClick = onOpenSchedule)
                GhostButton("Тейпер", onClick = onOpenTaper)
                GhostButton("Часы", onClick = onOpenWatches)
                GhostButton("Резервная копия настроек", onClick = onOpenBackup)
                GhostButton("О программе", onClick = onOpenAbout)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    val profile = vm.profile ?: Profile(userId = vm.user?.id ?: 0)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            "Добро пожаловать",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )
        Text(
            "Заполните профиль: без пульсовых порогов и веса модель не сможет корректно оценивать нагрузку.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        ProfileForm(
            initial = profile,
            email = vm.user?.email ?: "",
            syncing = vm.syncing,
            lastSyncAt = vm.lastSyncAt,
            athleteName = vm.athleteName,
            saveLabel = "Сохранить и начать",
            onSave = { vm.saveProfile(it, markOnboarded = true) },
            onSaveAndSync = { vm.saveProfileAndSync(it) },
            onTestAi = { vm.testAi(it) },
        )
        GhostButton("Настроить позже", onClick = { vm.skipOnboarding() })
        Spacer(Modifier.height(28.dp))
    }
}

private fun parsePace(input: String): Int? {
    val value = input.trim().replace(',', ':').replace('.', ':')
    if (value.isEmpty()) return null
    val parts = value.split(":")
    return when (parts.size) {
        1 -> parts[0].toIntOrNull()?.takeIf { it in 120..900 }
        2 -> {
            val m = parts[0].toIntOrNull()
            val s = parts[1].toIntOrNull()
            if (m != null && s != null && s in 0..59) (m * 60 + s).takeIf { it in 120..900 } else null
        }
        else -> null
    }
}
