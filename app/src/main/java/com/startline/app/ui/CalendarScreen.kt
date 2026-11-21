package com.startline.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.startline.app.AppViewModel
import com.startline.app.data.Fmt
import com.startline.app.data.Race
import com.startline.app.data.RacePriority
import com.startline.app.data.Sport
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@Composable
fun CalendarScreen(vm: AppViewModel) {
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(today) }
    var showAdd by remember { mutableStateOf(false) }

    val dayRaces = vm.races.filter { it.date == selected }
    val dayWorkouts = vm.workouts.filter { it.date == selected }
    val futureWorkouts = vm.workouts.filter { !it.date.isBefore(today) }
    val planLoad = futureWorkouts.sumOf { it.load }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        ScreenHeader(
            title = "Календарь",
            subtitle = "Старты и план тренировок",
            actions = { IconAction(Icons.Filled.Add, "Добавить старт", { showAdd = true }) },
        )

        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    Icon(Icons.Filled.ChevronLeft, "Предыдущий месяц", tint = TextSecondary)
                }
                Text(
                    "${Fmt.monthNom(month.monthValue)} ${month.year}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                if (month != YearMonth.now()) {
                    TextButton(onClick = { month = YearMonth.now(); selected = today }) {
                        Text("Сегодня", color = Lime, style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = { month = month.plusMonths(1) }) {
                    Icon(Icons.Filled.ChevronRight, "Следующий месяц", tint = TextSecondary)
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс").forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            val first = month.atDay(1)
            val leading = first.dayOfWeek.value - 1
            val daysInMonth = month.lengthOfMonth()
            val rows = ((leading + daysInMonth + 6) / 7)
            for (week in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dow in 0..6) {
                        val dayNum = week * 7 + dow - leading + 1
                        if (dayNum in 1..daysInMonth) {
                            val date = month.atDay(dayNum)
                            val races = vm.races.filter { it.date == date }
                            val workouts = vm.workouts.filter { it.date == date }
                            DayCell(
                                date = date,
                                selected = date == selected,
                                today = date == today,
                                racePriority = races.maxByOrNull { priorityWeight(it.priority) }?.priority,
                                hasRun = races.any { it.sport == Sport.RUN } || workouts.any { it.sport == Sport.RUN },
                                hasBike = races.any { it.sport == Sport.BIKE } || workouts.any { it.sport == Sport.BIKE },
                                onClick = { selected = date },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("Выбранный день")
                    Text(
                        "${Fmt.date(selected)}, ${Fmt.weekday(selected)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                }
                if (dayRaces.isNotEmpty() || dayWorkouts.isNotEmpty()) {
                    Pill("${dayRaces.size + dayWorkouts.size}", Lime)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (dayRaces.isEmpty() && dayWorkouts.isEmpty()) {
                Text("Свободный день. Ни стартов, ни тренировок.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                GhostButton("Добавить старт", onClick = { showAdd = true })
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dayRaces.forEach { race ->
                        RaceRow(
                            name = race.name,
                            sport = race.sport,
                            dateText = Fmt.date(race.date),
                            priority = race.priority.key,
                            extra = race.distanceKm?.let { "${Fmt.one(it)} км" },
                            trailing = {
                                IconButton(onClick = { vm.deleteRace(race.id) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Filled.Delete, "Удалить", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            },
                        )
                    }
                    dayWorkouts.forEach { WorkoutRow(it, showDate = false) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("Периодизация")
                    Text("Ближайшие 4 недели", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Pill("${Fmt.load(planLoad)} нагрузка", Lime)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (vm.races.none { !it.date.isBefore(today) }) {
                    "Добавьте старт класса A — генератор подберёт базу, развитие, пик и подводку."
                } else {
                    "План привязан к ближайшему главному старту. После изменения календаря пересоберите его."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                if (futureWorkouts.isEmpty()) "Сгенерировать план" else "Пересобрать план",
                onClick = { vm.generatePlan() },
                loading = vm.generating,
            )
        }

        Spacer(Modifier.height(12.dp))

        if (futureWorkouts.isEmpty()) {
            EmptyCard("План ещё не создан. Нажмите «Сгенерировать план» выше.")
        } else {
            val grouped = futureWorkouts.groupBy { it.date.with(DayOfWeek.MONDAY) }
            grouped.forEach { (weekStart, _) ->
                val items = grouped[weekStart].orEmpty()
                val phase = items.firstOrNull()?.phase ?: "База"
                CardBox {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${Fmt.date(weekStart)} – ${Fmt.date(weekStart.plusDays(6))}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Pill(phase, if (phase == "Старт" || phase == "Подводка") Orange else Lime)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${items.size} тренировок · ${Fmt.load(items.sumOf { it.load })} нагрузки",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.forEach { WorkoutRow(it) }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Приоритет A — главный старт (полная подводка), B — контрольный, C — тренировочный. План пересчитывается от текущей формы.",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showAdd) {
        AddRaceDialog(
            initialDate = selected,
            onDismiss = { showAdd = false },
            onSave = { sport, name, date, priority, distance, target, notes ->
                vm.addRace(
                    Race(
                        userId = 0,
                        sport = sport,
                        name = name,
                        date = date,
                        priority = priority,
                        distanceKm = distance,
                        targetSec = target,
                        notes = notes,
                    )
                )
                selected = date
                month = YearMonth.from(date)
                showAdd = false
            },
        )
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    racePriority: RacePriority?,
    hasRun: Boolean,
    hasBike: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    selected -> Lime.copy(alpha = 0.16f)
                    today -> Surface2
                    else -> androidx.compose.ui.graphics.Color.Transparent
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Lime else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(5.dp))
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    selected -> Lime
                    today -> Lime
                    else -> TextPrimary
                },
                fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Normal,
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (racePriority != null) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                when (racePriority) {
                                    RacePriority.A -> Orange
                                    RacePriority.B -> Cyan
                                    RacePriority.C -> TextSecondary
                                }
                            ),
                    )
                }
                if (hasRun) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Lime),
                    )
                }
                if (hasBike) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Cyan),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRaceDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Sport, String, LocalDate, RacePriority, Double?, Int?, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf(Sport.RUN) }
    var priority by remember { mutableStateOf(RacePriority.B) }
    var date by remember { mutableStateOf(initialDate) }
    var distance by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Новый старт", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Field("Название", name, { name = it }, placeholder = "Полумарафон, гонка, этап")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip("Бег", sport == Sport.RUN, { sport = Sport.RUN })
                    SelectChip("Велосипед", sport == Sport.BIKE, { sport = Sport.BIKE }, color = Cyan)
                }
                Spacer(Modifier.height(10.dp))
                Text("Приоритет", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RacePriority.entries.forEach { p ->
                        SelectChip("${p.key} · ${p.hint}", priority == p, { priority = p }, color = if (p == RacePriority.A) Orange else Lime)
                    }
                }
                Spacer(Modifier.height(10.dp))
                SurfaceDateButton(date, onClick = { showDatePicker = true })
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(
                        "Дистанция, км",
                        distance,
                        { distance = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal,
                        placeholder = "21.1",
                    )
                    Field(
                        "Цель, время",
                        target,
                        { target = it },
                        Modifier.weight(1f),
                        placeholder = "1:35:00",
                    )
                }
                Spacer(Modifier.height(10.dp))
                Field("Заметки", notes, { notes = it }, singleLine = false, placeholder = "Трасса, питание, задачи")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val distanceValue = distance.replace(',', '.').toDoubleOrNull()
                    onSave(
                        sport,
                        name.trim(),
                        date,
                        priority,
                        distanceValue,
                        parseTime(target),
                        notes.trim(),
                    )
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Сохранить", color = if (name.isNotBlank()) Lime else TextSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        },
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK", color = Lime) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена", color = TextSecondary) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun SurfaceDateButton(date: LocalDate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Flag, null, tint = Lime, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Дата: ${Fmt.dateFull(date)}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text("изменить", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

private fun priorityWeight(priority: RacePriority): Int = when (priority) {
    RacePriority.A -> 3
    RacePriority.B -> 2
    RacePriority.C -> 1
}

private fun parseTime(input: String): Int? {
    val parts = input.trim().split(":").map { it.trim() }
    if (parts.any { it.isEmpty() }) return null
    return when (parts.size) {
        2 -> {
            val m = parts[0].toIntOrNull() ?: return null
            val s = parts[1].toIntOrNull() ?: return null
            if (s !in 0..59) null else m * 60 + s
        }
        3 -> {
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val s = parts[2].toIntOrNull() ?: return null
            if (m !in 0..59 || s !in 0..59) null else h * 3600 + m * 60 + s
        }
        else -> null
    }
}
