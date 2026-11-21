package com.startline.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.startline.app.AppViewModel
import com.startline.app.data.Activity
import com.startline.app.data.Fmt
import com.startline.app.data.Sport
import com.startline.app.domain.AthleteModelBuilder
import com.startline.app.domain.HrZones
import java.time.LocalDate

@Composable
fun ActivitiesScreen(vm: AppViewModel, onOpenActivity: (String) -> Unit) {
    var filter by remember { mutableStateOf<Sport?>(null) }
    val today = LocalDate.now()
    val filtered = vm.activitiesFor(filter)
    val monthActivities = filtered.filter {
        it.date.year == today.year && it.date.month == today.month
    }
    val zoneColors = listOf(TextSecondary, Cyan, Lime, Orange, Red)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "Тренировки",
                subtitle = "Загружено ${vm.activities.size} активностей",
                actions = { RefreshAction(vm.syncing, onClick = { vm.syncNow() }) },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("Все", filter == null, { filter = null })
                SelectChip("Бег", filter == Sport.RUN, { filter = Sport.RUN })
                SelectChip("Велосипед", filter == Sport.BIKE, { filter = Sport.BIKE }, color = Cyan)
            }
        }
        item {
            CardBox {
                SectionLabel("За этот месяц")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        Fmt.distance(monthActivities.sumOf { it.distanceM }),
                        "дистанция",
                        Modifier.weight(1f),
                        Lime,
                    )
                    StatTile(
                        Fmt.one(monthActivities.sumOf { it.movingSec } / 3600.0),
                        "часов",
                        Modifier.weight(1f),
                        Cyan,
                    )
                    StatTile(
                        Fmt.load(monthActivities.sumOf { it.load }),
                        "нагрузка",
                        Modifier.weight(1f),
                        TextPrimary,
                    )
                    StatTile(
                        "${monthActivities.size}",
                        "тренировок",
                        Modifier.weight(1f),
                        TextPrimary,
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item { EmptyCard("Нет тренировок. Проверьте ключ intervals.icu и синхронизацию.") }
        }
        items(filtered, key = { it.id }) { activity ->
            ActivityCard(activity) { onOpenActivity(activity.id) }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ActivityCard(activity: Activity, onClick: () -> Unit) {
    CardBox(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (activity.sport == Sport.RUN) Lime.copy(alpha = 0.12f) else Cyan.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                SportIcon(activity.sport, 20)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activity.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(Fmt.date(activity.date))
                        if (activity.startLocal.length >= 16) append(" · ${activity.startLocal.substring(11, 16)}")
                        append(" · ${activity.sport.label}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            if (activity.isRace) {
                Spacer(Modifier.width(6.dp))
                Pill("ГОНКА", Orange, solid = true)
            }
            Spacer(Modifier.width(6.dp))
            Pill("${Fmt.load(activity.load)}", if (activity.sport == Sport.RUN) Lime else Cyan)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatTile(Fmt.distance(activity.distanceM), "дистанция", Modifier.weight(1f))
            StatTile(Fmt.duration(activity.movingSec), "время", Modifier.weight(1f))
            StatTile(
                if (activity.sport == Sport.RUN) Fmt.pace(activity.avgSpeed?.let { 1000.0 / it })
                else Fmt.speedKmh(activity.avgSpeed),
                if (activity.sport == Sport.RUN) "темп" else "скорость",
                Modifier.weight(1f),
            )
            StatTile(
                activity.avgHr?.toString() ?: "—",
                "пульс ср.",
                Modifier.weight(1f),
                if ((activity.avgHr ?: 0) > 0) Orange else TextPrimary,
            )
        }
    }
}

@Composable
fun ActivityDetailScreen(vm: AppViewModel, activity: Activity, onBack: () -> Unit) {
    val profile = vm.profile
    val context = LocalContext.current
    LaunchedEffect(activity.id) { vm.openActivity(activity.id) }
    DisposableEffect(activity.id) {
        onDispose { vm.closeActivity() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Назад", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Тренировка", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(
                    "${Fmt.dateFull(activity.date)} · ${activity.sport.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            if (activity.isRace) {
                Pill("ГОНКА", Orange, solid = true)
                Spacer(Modifier.width(6.dp))
            }
            Pill(activity.sport.label, if (activity.sport == Sport.RUN) Lime else Cyan)
        }

        Text(
            activity.name,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )
        Spacer(Modifier.height(12.dp))

        CardBox {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(Fmt.distance(activity.distanceM), "дистанция", Modifier.weight(1f), Lime)
                StatTile(Fmt.duration(activity.movingSec), "время", Modifier.weight(1f))
                StatTile(Fmt.load(activity.load), "нагрузка", Modifier.weight(1f), Orange)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    if (activity.sport == Sport.RUN) Fmt.pace(activity.avgSpeed?.let { 1000.0 / it })
                    else Fmt.speedKmh(activity.avgSpeed),
                    if (activity.sport == Sport.RUN) "средний темп" else "средняя скорость",
                    Modifier.weight(1f),
                )
                StatTile(activity.avgHr?.toString() ?: "—", "пульс средний", Modifier.weight(1f), Orange)
                StatTile(activity.maxHr?.toString() ?: "—", "пульс макс", Modifier.weight(1f), Red)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(activity.elevationM?.let { "${it.toInt()} м" } ?: "—", "набор", Modifier.weight(1f))
                StatTile(activity.calories?.toString() ?: "—", "ккал", Modifier.weight(1f))
                StatTile(
                    activity.normPower?.toString() ?: activity.avgPower?.toString() ?: "—",
                    "мощность NP/ср",
                    Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("Пульсовой ряд")
                    Text("Динамика пульса", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                if (vm.streamsLoading) {
                    Text("загрузка…", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(10.dp))
            when {
                vm.streamsLoading -> LoadingBlock()
                vm.streamsError != null -> Text(
                    vm.streamsError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                vm.streams != null -> {
                    val lthr = if (activity.sport == Sport.RUN) profile?.lthrRun else profile?.lthrBike
                    HrLineChart(vm.streams!!.heartrate)
                    if (lthr != null && lthr > 0) {
                        val fractions = HrZones.distribution(vm.streams!!.heartrate, lthr)
                        Spacer(Modifier.height(12.dp))
                        ZoneBar(fractions, colors = listOf(TextSecondary, Cyan, Lime, Orange, Red))
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            fractions.forEachIndexed { index, fraction ->
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Z${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                    Text(
                                        "${(fraction * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
                else -> Text("Нет данных", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Spacer(Modifier.height(12.dp))
        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Источник: intervals.icu", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text(
                        "Полный анализ, сегменты и карта доступны в веб-версии.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
                Icon(Icons.Filled.OpenInNew, null, tint = Lime, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            GhostButton("Открыть в intervals.icu", onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://intervals.icu/activities/${activity.id}"))
                    )
                }
            })
        }

        Spacer(Modifier.height(24.dp))
    }
}
