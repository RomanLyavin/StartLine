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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.startline.app.AppViewModel
import com.startline.app.data.Fmt
import com.startline.app.data.Sport
import com.startline.app.domain.HrZone
import com.startline.app.domain.PaceZone

@Composable
fun ModelScreen(vm: AppViewModel, onOpenProfile: () -> Unit) {
    val profile = vm.profile
    if (profile == null) {
        LoadingBlock()
        return
    }
    val model = vm.model
    val fatigue = vm.fatigue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        ScreenHeader(
            title = "Модель",
            subtitle = "Спортсмен: ${profile.name.ifBlank { "без имени" }}",
            actions = { RefreshAction(vm.syncing, onClick = { vm.refresh() }) },
        )

        CardBox {
            SectionLabel("Модель спортсмена")
            Spacer(Modifier.height(6.dp))
            Text(
                model.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(Fmt.one(fatigue.ctl), "CTL фитнес", Modifier.weight(1f), Lime)
                StatTile(Fmt.one(fatigue.atl), "ATL усталость", Modifier.weight(1f), Orange)
                StatTile(Fmt.signed(fatigue.tsb), "TSB форма", Modifier.weight(1f), if (fatigue.tsb >= 0) Lime else Red)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(Fmt.one(model.weeklyHours), "часов в неделю", Modifier.weight(1f), Lime)
            StatTile(Fmt.one(model.weeklySessions), "тренировок/нед", Modifier.weight(1f))
            StatTile("${model.consistency}%", "регулярность", Modifier.weight(1f), Cyan)
            StatTile("${model.intensityShare}%", "интенсивность", Modifier.weight(1f), Orange)
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("Баланс нагрузки")
                    Text("Бег и велосипед · 28 дней", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Text("${model.runShare}% / ${model.bikeShare}%", style = MaterialTheme.typography.titleMedium, color = Lime)
            }
            Spacer(Modifier.height(10.dp))
            ZoneBar(
                fractions = listOf(model.runShare / 100.0, model.bikeShare / 100.0),
                colors = listOf(Lime, Cyan),
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Lime))
                    Spacer(Modifier.width(5.dp))
                    Text("Бег ${Fmt.load(model.runLoad28)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Cyan))
                    Spacer(Modifier.width(5.dp))
                    Text("Вело ${Fmt.load(model.bikeLoad28)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(if (model.longRunMin > 0) "${model.longRunMin} мин" else "—", "длинный бег", Modifier.weight(1f))
                StatTile(if (model.longRideMin > 0) "${model.longRideMin} мин" else "—", "длинная вело", Modifier.weight(1f))
                StatTile(Fmt.load(model.weeklyLoad), "нагрузка/нед", Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            SectionLabel("Сильные стороны")
            Spacer(Modifier.height(8.dp))
            model.strengths.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Lime, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(item, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                }
            }
            Spacer(Modifier.height(10.dp))
            SectionLabel("Ограничения")
            Spacer(Modifier.height(8.dp))
            model.limiters.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Icon(Icons.Filled.Warning, null, tint = Orange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(item, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ZoneTableCard(
            title = "Пульсовые зоны: бег",
            subtitle = "ПАНО ${profile.lthrRun ?: "—"} · макс ${profile.hrMaxRun ?: "—"}",
            zones = model.hrZonesRun,
            accent = Lime,
        )

        Spacer(Modifier.height(12.dp))

        ZoneTableCard(
            title = "Пульсовые зоны: велосипед",
            subtitle = "ПАНО ${profile.lthrBike ?: "—"} · макс ${profile.hrMaxBike ?: "—"}",
            zones = model.hrZonesBike,
            accent = Cyan,
        )

        Spacer(Modifier.height(12.dp))

        CardBox {
            SectionLabel("Темповые зоны бега")
            Spacer(Modifier.height(2.dp))
            Text(
                "ПАНО ${Fmt.pace(profile.thresholdPaceSec?.toDouble())}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            if (model.paceZones.isEmpty()) {
                Text("Укажите темп ПАНО в профиле", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                GhostButton("Открыть профиль", onClick = onOpenProfile)
            } else {
                model.paceZones.forEach { z ->
                    PaceRow(z, Lime)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Как считается модель", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "CTL/ATL — экспоненциальные средние нагрузки за 42 и 7 дней, TSB — их разница. " +
                    "ACWR сравнивает нагрузку недели со средней за 4 недели. Если intervals.icu отдаёт свои CTL/ATL, " +
                    "показываются они. Зоны пульса строятся от ПАНО, темповые — от темпа ПАНО.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            if (!profile.isFilled) {
                Spacer(Modifier.height(8.dp))
                GhostButton("Заполнить профиль", onClick = onOpenProfile)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ZoneTableCard(title: String, subtitle: String, zones: List<HrZone>, accent: Color) {
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                SectionLabel(title)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        Spacer(Modifier.height(10.dp))
        if (zones.isEmpty()) {
            Text("Укажите ПАНО в профиле", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        } else {
            zones.forEachIndexed { index, zone ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(30.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(zoneColor(index)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(zone.name, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text(zone.purpose, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Text("${zone.range()} уд", style = MaterialTheme.typography.labelMedium, color = accent)
                }
            }
        }
    }
}

@Composable
private fun PaceRow(zone: PaceZone, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(zone.name, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(zone.note, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Text(zone.range(), style = MaterialTheme.typography.labelMedium, color = accent)
    }
}

private fun zoneColor(index: Int): Color = when (index) {
    0 -> TextSecondary
    1 -> Cyan
    2 -> Lime
    3 -> Orange
    else -> Red
}
