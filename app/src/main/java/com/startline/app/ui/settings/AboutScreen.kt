package com.startline.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.startline.app.AppViewModel
import com.startline.app.R
import com.startline.app.update.GitHubUpdate
import com.startline.app.update.UpdateState
import com.startline.app.ui.AppLinks
import com.startline.app.ui.CardBox
import com.startline.app.ui.GhostButton
import com.startline.app.ui.PrimaryButton
import com.startline.app.ui.Red
import com.startline.app.ui.ScreenHeader
import com.startline.app.ui.SectionLabel
import com.startline.app.ui.TextPrimary
import com.startline.app.ui.TextSecondary
import java.io.File

/**
 * Экран «О программе»: сведения о приложении и разработчике, поддержка
 * проекта и проверка обновлений через GitHub с установкой APK.
 */
@Composable
fun AboutScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val version = vm.currentVersionName()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        ScreenHeader(
            title = "О программе",
            subtitle = "StartLine $version",
            actions = {
                GhostButton("Назад", onClick = onBack)
            },
        )

        CardBox {
            SectionLabel("StartLine")
            Spacer(Modifier.height(6.dp))
            Text(
                "План тренировок по бегу и велоспорту с ИИ, подводкой к соревнованиям и моделью атлета.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Версия $version",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            SectionLabel("Разработчик")
            Spacer(Modifier.height(6.dp))
            Text(
                "Roman Lyavin",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Проект развивается как открытое приложение для самостоятельного планирования тренировок.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            GhostButton("Профиль на GitHub", onClick = { AppLinks.openUrl(context, AppLinks.DEVELOPER_URL) })
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            SectionLabel("Поддержать проект")
            Spacer(Modifier.height(6.dp))
            Text(
                "Если StartLine помогает вам тренироваться, вы можете поддержать развитие проекта. Нажмите на изображение, чтобы перейти к оплате.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Image(
                painter = painterResource(R.drawable.donate),
                contentDescription = "Поддержать проект",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { AppLinks.openUrl(context, AppLinks.SUPPORT_URL) },
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Поддержать", onClick = { AppLinks.openUrl(context, AppLinks.SUPPORT_URL) })
        }

        Spacer(Modifier.height(12.dp))

        CardBox {
            SectionLabel("Обновления")
            Spacer(Modifier.height(6.dp))
            Text(
                "Текущая версия: $version",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            UpdateSection(vm = vm, context = context)
            Spacer(Modifier.height(6.dp))
            GhostButton("Открыть релизы на GitHub", onClick = { AppLinks.openUrl(context, GitHubUpdate.RELEASES_URL) })
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun UpdateSection(vm: AppViewModel, context: Context) {
    when (val state = vm.updateState) {
        UpdateState.Idle -> {
            PrimaryButton("Проверить обновления", onClick = { vm.checkUpdates() })
        }

        UpdateState.Checking -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    "Проверяем релизы на GitHub…",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        is UpdateState.Latest -> {
            Text(
                "У вас последняя версия (${state.version}).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            GhostButton("Проверить снова", onClick = { vm.checkUpdates() })
        }

        is UpdateState.Available -> {
            Text(
                "Доступна версия ${state.version}." +
                    if (state.size > 0L) " Размер: ${state.size / 1024 / 1024} МБ." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Скачать обновление", onClick = { vm.downloadUpdate() })
        }

        is UpdateState.Downloading -> {
            Text(
                "Скачивание обновления: ${state.percent}%",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.percent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is UpdateState.Ready -> {
            Text(
                "Версия ${state.version} скачана и готова к установке.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                "Установить обновление",
                onClick = { installApk(context, state.file) },
            )
        }

        is UpdateState.Error -> {
            Text(
                state.message,
                style = MaterialTheme.typography.bodySmall,
                color = Red,
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Повторить проверку", onClick = { vm.checkUpdates() })
        }
    }
}

private fun installApk(context: Context, file: File) {
    if (!file.exists()) {
        Toast.makeText(context, "Файл обновления не найден", Toast.LENGTH_LONG).show()
        return
    }
    if (!context.packageManager.canRequestPackageInstalls()) {
        Toast.makeText(
            context,
            "Разрешите установку приложений из этого источника",
            Toast.LENGTH_LONG,
        ).show()
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
        } catch (e: Exception) {
            // Настройки недоступны — пользователь сможет включить разрешение вручную.
        }
        return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Не удалось запустить установщик", Toast.LENGTH_LONG).show()
    }
}
