package com.startline.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.startline.app.AppViewModel
import com.startline.app.data.WatchConnection

/** Экран подводки к соревнованию (тейпера). */
@Composable
fun TaperScreen(vm: AppViewModel, onBack: (() -> Unit)? = null, onOpenCalendar: () -> Unit = {}) {
    val plan = vm.taperPlan
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        ScreenHeader(
            title = "Тейпер",
            subtitle = "Подводка к соревнованию",
            actions = {
                if (onBack != null) IconAction(Icons.Filled.ArrowBack, "Назад", onBack)
            },
        )
        CardBox {
            Text(
                if (plan.hasRace) "Ближайший старт" else "Нет запланированных стартов",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                plan.recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            if (plan.daysLeft != null) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("${plan.daysLeft} дн. до старта", Lime, solid = true)
                    plan.current?.let { Pill(it.phase, Cyan) }
                }
            }
            if (!plan.hasRace) {
                Spacer(Modifier.height(8.dp))
                GhostButton("Открыть календарь стартов", onClick = onOpenCalendar)
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionLabel("Ступени подводки")
        Spacer(Modifier.height(8.dp))
        plan.steps.forEach { step ->
            CardBox(modifier = Modifier.padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        step.phase,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (plan.current?.phase == step.phase) Lime else TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Pill("${step.volumePct}% объёма", if (step.volumePct <= 40) Orange else Lime)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Дни ${step.fromDay}–${if (step.toDay == Long.MAX_VALUE) "∞" else step.toDay.toString()} · ${step.intensity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan,
                )
                Spacer(Modifier.height(6.dp))
                Text(step.note, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Экран OAuth-подключения часов. */
@Composable
fun WatchConnectionsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        ScreenHeader(
            title = "Часы",
            subtitle = "OAuth-подключение производителей",
            actions = { IconAction(Icons.Filled.ArrowBack, "Назад", onBack) },
        )
        Text(
            "Подключите часы, чтобы синхронизировать тренировки. Авторизация открывается в браузере; " +
                "токены хранятся в зашифрованном виде на устройстве.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        vm.watchConnections.forEach { connection ->
            WatchConnectionCard(
                connection = connection,
                onConnect = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(vm.watchAuthUrl(connection.provider)))
                    runCatching { context.startActivity(intent) }
                },
                onDisconnect = { vm.disconnectWatch(connection.provider) },
                onSync = { vm.syncWatch(connection.provider) },
            )
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WatchConnectionCard(
    connection: WatchConnection,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit,
) {
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                connection.provider.label,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Pill(
                if (connection.connected) "Подключены" else "Не подключены",
                if (connection.connected) Lime else TextSecondary,
                solid = connection.connected,
            )
        }
        if (connection.connected) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Дата подключения: ${connection.connectedDate}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))
        if (connection.connected) {
            GhostButton("Загрузить тренировки", onClick = onSync)
            Spacer(Modifier.height(8.dp))
            GhostButton("Отключить", onClick = onDisconnect)
        } else {
            androidx.compose.material3.Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Surface3,
                    contentColor = TextPrimary,
                ),
            ) {
                Text("Подключить", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
