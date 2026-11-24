package com.startline.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.startline.app.AppViewModel
import com.startline.app.data.Fmt
import com.startline.app.ui.CardBox
import com.startline.app.ui.GhostButton
import com.startline.app.ui.LoadingBlock
import com.startline.app.ui.Orange
import com.startline.app.ui.PrimaryButton
import com.startline.app.ui.ScreenHeader
import com.startline.app.ui.SectionLabel
import com.startline.app.ui.SelectChip
import com.startline.app.ui.TextSecondary

/**
 * Настройки тренировочного графика: сколько дней в неделю тренироваться и на
 * какие дни ставить длительную, высокоинтенсивную и силовую тренировки.
 */
@Composable
fun TrainingScheduleScreen(vm: AppViewModel, onBack: () -> Unit) {
    val profile = vm.profile
    if (profile == null) {
        LoadingBlock()
        return
    }

    var trainingDays by remember(profile) { mutableIntStateOf(profile.trainingDays.coerceIn(1, 7)) }
    var longRunDay by remember(profile) { mutableIntStateOf(profile.longRunDay.coerceIn(1, 7)) }
    var highIntensityDay by remember(profile) { mutableIntStateOf(profile.highIntensityDay.coerceIn(1, 7)) }
    var strengthDay by remember(profile) { mutableIntStateOf(profile.strengthDay.coerceIn(1, 7)) }
    var strengthEveryWeeks by remember(profile) { mutableIntStateOf(profile.strengthEveryWeeks.coerceIn(1, 4)) }

    val keyDays = listOf(longRunDay, highIntensityDay, strengthDay)
    val hasConflict = keyDays.distinct().size != keyDays.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        ScreenHeader(
            title = "Расписание",
            subtitle = "Дни и количество тренировок",
            actions = {
                GhostButton("Назад", onClick = onBack)
            },
        )

        CardBox {
            SectionLabel("Дней в неделю")
            Spacer(Modifier.height(4.dp))
            Text(
                "Сколько тренировок планировать каждую неделю. Ключевые занятия — интервалы, длительная и силовая — остаются в плане первыми.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..4).forEach { day ->
                    SelectChip(day.toString(), trainingDays == day, { trainingDays = day })
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (5..7).forEach { day ->
                    SelectChip(day.toString(), trainingDays == day, { trainingDays = day })
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Выбрано: $trainingDays ${Fmt.plural(trainingDays, "день", "дня", "дней")} в неделю",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(12.dp))
        DayPicker(
            title = "Длительная тренировка",
            hint = "Длинная пробежка или велотренировка в аэробной зоне.",
            selected = longRunDay,
            onSelect = { longRunDay = it },
        )

        Spacer(Modifier.height(12.dp))
        DayPicker(
            title = "Высокоинтенсивная (интервалы)",
            hint = "Пороговая работа, VO2max или темповый бег.",
            selected = highIntensityDay,
            onSelect = { highIntensityDay = it },
        )

        Spacer(Modifier.height(12.dp))
        DayPicker(
            title = "Силовая тренировка",
            hint = "Круговая силовая. Не ставится в неделю старта и на подводке.",
            selected = strengthDay,
            onSelect = { strengthDay = it },
        )

        Spacer(Modifier.height(12.dp))
        CardBox {
            SectionLabel("Частота силовых")
            Spacer(Modifier.height(4.dp))
            Text(
                "Как часто ставить силовую: каждую неделю, раз в 2 недели и т. д. В неделю старта и на подводке силовая снимается.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..4).forEach { n ->
                    SelectChip(
                        if (n == 1) "каждую неделю" else "раз в $n ${Fmt.plural(n, "неделю", "недели", "недель")}",
                        strengthEveryWeeks == n,
                        { strengthEveryWeeks = n },
                    )
                }
            }
        }

        if (hasConflict) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Выбранные дни пересекаются: лишнее занятие автоматически сдвинется на ближайший свободный день.",
                style = MaterialTheme.typography.bodySmall,
                color = Orange,
            )
        }

        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            "Сохранить и пересобрать план",
            onClick = {
                vm.saveSchedule(
                    profile.copy(
                        trainingDays = trainingDays,
                        longRunDay = longRunDay,
                        highIntensityDay = highIntensityDay,
                        strengthDay = strengthDay,
                        strengthEveryWeeks = strengthEveryWeeks,
                    ),
                )
                onBack()
            },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Изменения применятся к плану на ближайшие 4 недели.",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DayPicker(
    title: String,
    hint: String,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    CardBox {
        SectionLabel(title)
        Spacer(Modifier.height(4.dp))
        Text(hint, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..4).forEach { day ->
                SelectChip(Fmt.weekdayShort(day), selected == day, { onSelect(day) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (5..7).forEach { day ->
                SelectChip(Fmt.weekdayShort(day), selected == day, { onSelect(day) })
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Выбрано: ${Fmt.weekdayName(selected)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
