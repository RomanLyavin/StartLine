package com.startline.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.startline.app.AppViewModel
import com.startline.app.data.Fmt
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AiScreen(vm: AppViewModel, onBack: () -> Unit, onOpenProfile: () -> Unit) {
    val profile = vm.profile
    val analysis = vm.analysis

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Назад", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("ИИ-тренер", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(
                    if (profile?.aiKey?.isNotBlank() == true) "Модель: ${profile.aiModel}"
                    else "Встроенный тренер",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Icon(Icons.Filled.AutoAwesome, null, tint = Lime, modifier = Modifier.size(22.dp))
        }

        CardBox {
            Text(
                if (profile?.aiKey?.isNotBlank() == true) {
                    "Анализ отправит обезличенные цифры тренировок в выбранную модель: пульс, нагрузку, старты и план. Пароль и ключ intervals.icu не отправляются."
                } else {
                    "Сейчас работает встроенный тренер: правила по нагрузке, усталости и вашему календарю. Чтобы получить разбор от бесплатной модели ИИ, добавьте её ключ в профиле."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                if (vm.aiLoading) "Анализирую…" else "Сгенерировать анализ",
                onClick = { vm.analyze() },
                loading = vm.aiLoading,
            )
            if (profile?.aiKey?.isBlank() != false) {
                Spacer(Modifier.height(4.dp))
                GhostButton("Настроить ИИ", onClick = onOpenProfile)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (analysis == null) {
            EmptyCard(
                "Анализа ещё нет. Нажмите кнопку выше — рекомендации появятся здесь и сохранятся.",
            )
        } else {
            CardBox {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("Рекомендации")
                        Text(
                            formattedDate(analysis.first),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    Pill(if (vm.aiUsedModel) "ИИ" else "Правила", Lime)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    analysis.second,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.15f,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "ИИ может ошибаться, а рекомендации не заменяют врача. При боли, аритмии или сильной усталости снижайте нагрузку и консультируйтесь со специалистом.",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun formattedDate(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val time = Instant.ofEpochMilli(timestamp).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
    return "${Fmt.dateFull(date)}, $time"
}
