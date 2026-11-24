package com.startline.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.startline.app.AppViewModel
import com.startline.app.data.Fmt
import com.startline.app.data.Sport
import com.startline.app.domain.RiskLevel
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun DashboardScreen(
    vm: AppViewModel,
    onOpenCalendar: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenAbout: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
) {
    val profile = vm.profile
    if (profile == null) {
        LoadingBlock()
        return
    }
    val fatigue = vm.fatigue
    val today = LocalDate.now()
    val name = profile.name.ifBlank { vm.user?.email?.substringBefore("@") ?: "атлет" }
    val futureRaces = vm.races.filter { !it.date.isBefore(today) }
    val nextRace = futureRaces.filter { it.priority.key == "A" }.minByOrNull { it.date }
        ?: futureRaces.minByOrNull { it.date }
    val weekStart = today.with(DayOfWeek.MONDAY)
    val weekEnd = weekStart.plusDays(6)
    val weekWorkouts = vm.workouts.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }
    val weekLoad = weekWorkouts.sumOf { it.load }
    val last7 = vm.activities.filter { !it.date.isBefore(today.minusDays(6)) }
    val riskColor = when (fatigue.risk) {
        RiskLevel.LOW -> Lime
        RiskLevel.MEDIUM -> Orange
        RiskLevel.HIGH -> Red
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        ScreenHeader(
            title = "Обзор",
            subtitle = "${Fmt.weekday(today)}, ${Fmt.date(today)}",
            actions = { RefreshAction(vm.syncing, onClick = { vm.syncNow() }) },
        )

        Text(
            "Привет, $name",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )
        Text(
            if (profile.isFilled) "Данные intervals.icu и ваша модель готовы к работе"
            else "Заполните профиль, чтобы модель считала нагрузку точнее",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(14.dp))

        if (profile.intervalsKey.isBlank() || vm.activities.isEmpty()) {
            CardBox {
                SectionLabel("Подключение")
                Spacer(Modifier.height(6.dp))
                Text(
                    if (profile.intervalsKey.isBlank()) {
                        "Добавьте API-ключ intervals.icu — приложение загрузит тренировки и построит модель."
                    } else {
                        "Ключ есть, но тренировки ещё не загружены. Запустите синхронизацию."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(10.dp))
                PrimaryButton(
                    if (profile.intervalsKey.isBlank()) "Заполнить профиль" else "Синхронизировать",
                    onClick = {
                        if (profile.intervalsKey.isBlank()) onOpenProfile() else vm.syncNow()
                    },
                    loading = vm.syncing,
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReadinessRing(fatigue.readiness, fatigue.label)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Состояние", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Pill(
                        when (fatigue.risk) {
                            RiskLevel.LOW -> "Риск низкий"
                            RiskLevel.MEDIUM -> "Следите за усталостью"
                            RiskLevel.HIGH -> "Высокий риск перегрузки"
                        },
                        riskColor,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatTile("${Fmt.one(fatigue.ctl)}", "CTL фитнес", Modifier.weight(1f), Lime)
                        StatTile("${Fmt.one(fatigue.atl)}", "ATL усталость", Modifier.weight(1f), Orange)
                        StatTile(Fmt.signed(fatigue.tsb), "TSB форма", Modifier.weight(1f), if (fatigue.tsb >= 0) Lime else Red)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                fatigue.advice,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Источник: ${fatigue.source} · ACWR ${Fmt.one(fatigue.acwr)} · монотонность ${Fmt.one(fatigue.monotony)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("Динамика нагрузки")
                    Text("Фитнес и усталость", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(Lime)
                    Text(" CTL", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(Modifier.width(10.dp))
                    LegendDot(Orange)
                    Text(" ATL", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            FitnessChart(fatigue.timeline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("120 дней", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("сегодня", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                Fmt.one(last7.sumOf { it.movingSec } / 3600.0),
                "часов за 7 дней",
                Modifier.weight(1f),
                Lime,
            )
            StatTile(Fmt.load(last7.sumOf { it.load }), "нагрузка 7 дней", Modifier.weight(1f), Cyan)
            StatTile("${last7.size}", "тренировок", Modifier.weight(1f), TextPrimary)
        }

        Spacer(Modifier.height(12.dp))

        CardBox(onClick = onOpenCalendar) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("Эта неделя")
                    Text(
                        "${weekWorkouts.size} тренировок · ${Fmt.load(weekLoad)} нагрузки",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                }
                Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary)
            }
            Spacer(Modifier.height(10.dp))
            if (weekWorkouts.isEmpty()) {
                Text(
                    "План на неделю не собран. Откройте календарь и нажмите «Сгенерировать план».",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    weekWorkouts.take(4).forEach { WorkoutRow(it) }
                }
                if (weekWorkouts.size > 4) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ещё ${weekWorkouts.size - 4} тренировок в календаре",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        CardBox(onClick = onOpenCalendar) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Flag, null, tint = Orange, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("Следующий старт")
                    if (nextRace == null) {
                        Text("Старты не запланированы", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Добавьте гонку класса A/B/C в календарь", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    } else {
                        Text(nextRace.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1)
                        Text(
                            "${Fmt.dateFull(nextRace.date)} · ${nextRace.sport.label} · приоритет ${nextRace.priority.key}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
                if (nextRace != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${nextRace.daysLeft(today)}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Lime,
                        )
                        Text("дней", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                } else {
                    Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        CardBox(onClick = onOpenAi) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Lime.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Lime, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("ИИ-тренер", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        when {
                            vm.analysis != null -> "Анализ готов · ${analysisAge(vm.analysis!!.first)}"
                            profile.aiKey.isNotBlank() -> "Подключена модель ${profile.aiModel}"
                            else -> "Встроенный тренер + бесплатные модели"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
                Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                vm.analysis?.second?.lineSequence()
                    ?.filter { it.isNotBlank() }
                    ?.take(3)
                    ?.joinToString(" ")
                    ?.take(260)
                    ?: "Получите разбор состояния, рисков и плана на неделю. Работает и без ключа ИИ.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val lastActivity = vm.activities.firstOrNull()
        if (lastActivity != null) {
            Spacer(Modifier.height(12.dp))
            CardBox(onClick = onOpenActivities) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SportIcon(lastActivity.sport, 20)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("Последняя тренировка")
                        Text(
                            lastActivity.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            maxLines = 1,
                        )
                        Text(
                            "${Fmt.date(lastActivity.date)} · ${Fmt.distance(lastActivity.distanceM)} · ${Fmt.duration(lastActivity.movingSec)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("О программе", onClick = onOpenAbout, modifier = Modifier.weight(1f))
            GhostButton("Поддержать", onClick = onOpenSupport, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color),
    )
}

private fun analysisAge(timestamp: Long): String {
    val minutes = (System.currentTimeMillis() - timestamp) / 60_000
    return when {
        minutes < 2 -> "только что"
        minutes < 60 -> "$minutes мин назад"
        minutes < 24 * 60 -> "${minutes / 60} ч назад"
        else -> "${minutes / (24 * 60)} дн назад"
    }
}
