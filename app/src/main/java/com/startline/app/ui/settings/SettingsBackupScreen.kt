package com.startline.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.startline.app.AppViewModel
import com.startline.app.data.SettingsBackup
import com.startline.app.ui.CardBox
import com.startline.app.ui.GhostButton
import com.startline.app.ui.IconAction
import com.startline.app.ui.Lime
import com.startline.app.ui.PrimaryButton
import com.startline.app.ui.ScreenHeader
import com.startline.app.ui.SectionLabel
import com.startline.app.ui.SelectChip
import com.startline.app.ui.TextPrimary
import com.startline.app.ui.TextSecondary
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * Резервная копия настроек — пункт 4.2 плана.
 * Копия лежит во внутренней памяти приложения; API-ключи в самой базе
 * шифруются через Android Keystore, в файл они попадают только если
 * пользователь сам это разрешил.
 */
@Composable
fun SettingsBackupScreen(vm: AppViewModel, onBack: () -> Unit) {
    var includeKeys by remember { mutableStateOf(true) }
    val files = vm.backupFiles()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = "Резервная копия",
            subtitle = "Настройки профиля и ключи",
            actions = { IconAction(Icons.Filled.ArrowBack, "Назад", onBack) },
        )

        CardBox {
            SectionLabel("Экспорт")
            Spacer(Modifier.height(8.dp))
            Text(
                "Файл сохраняется во внутреннюю память приложения (files/backups). " +
                    "Ключи в базе шифруются Android Keystore; в копию они попадают только при включённой опции.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip(
                    text = "Сохранять API-ключи",
                    selected = includeKeys,
                    onClick = { includeKeys = !includeKeys },
                    color = Lime,
                )
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Сохранить резервную копию", onClick = { vm.exportBackup(includeKeys) })
        }

        CardBox {
            SectionLabel("Файлы копий")
            Spacer(Modifier.height(8.dp))
            if (files.isEmpty()) {
                Text(
                    "Копий пока нет. Создайте первую — она пригодится при переносе на другое устройство.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            } else {
                files.forEach { file ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text(
                                "${formatDate(file.lastModified())} · ${file.length()} байт",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                        GhostButton("Восстановить", onClick = { vm.importBackup(file) })
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Text(
            "Версия формата копии: ${SettingsBackup.VERSION}. Копии более новой версии приложение не читает.",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
