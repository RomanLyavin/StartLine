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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.startline.app.AppViewModel
import com.startline.app.data.MailConfig
import com.startline.app.ui.CardBox
import com.startline.app.ui.Field
import com.startline.app.ui.GhostButton
import com.startline.app.ui.IconAction
import com.startline.app.ui.PrimaryButton
import com.startline.app.ui.ScreenHeader
import com.startline.app.ui.SectionLabel
import com.startline.app.ui.SelectChip
import com.startline.app.ui.TextSecondary

/**
 * Настройки SMTP. Пользователь может ввести сервер и пароль прямо на телефоне —
 * письмо с паролем при регистрации уходит через эти значения. Пароль хранится
 * шифрованным через Android Keystore.
 */
@Composable
fun MailSettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val current = vm.mailSettings
    var host by remember { mutableStateOf(current.host) }
    var port by remember { mutableStateOf(current.port.toString()) }
    var user by remember { mutableStateOf(current.user) }
    var password by remember { mutableStateOf(current.password) }
    var from by remember { mutableStateOf(current.from) }
    var starttls by remember { mutableStateOf(current.useStartTls) }

    val config = MailConfig(
        host = host.trim(),
        port = port.toIntOrNull() ?: 587,
        user = user.trim(),
        password = password,
        from = from.trim(),
        useStartTls = starttls,
    )

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
            title = "Почта (SMTP)",
            subtitle = "Отправка пароля при регистрации",
            actions = { IconAction(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack) },
        )

        CardBox {
            SectionLabel("Сервер")
            Spacer(Modifier.height(8.dp))
            Field("Сервер", host, { host = it }, placeholder = "smtp.example.com")
            Field("Порт", port, { port = it }, keyboardType = KeyboardType.Number, placeholder = "587")
            Field("Логин", user, { user = it }, placeholder = "bot@example.com")
            Field("Пароль", password, { password = it }, password = true, placeholder = "пароль приложения")
            Field("Отправитель", from, { from = it }, keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("STARTTLS", starttls, { starttls = !starttls })
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Сохранить", onClick = { vm.saveMailSettings(config) })
            GhostButton(
                "Отправить тестовое письмо",
                onClick = { vm.sendTestMail() },
                enabled = config.isConfigured,
            )
        }

        CardBox {
            SectionLabel("Как это работает")
            Spacer(Modifier.height(6.dp))
            Text(
                "При регистрации письмо с email и паролем отправляется на адрес, который " +
                    "указан в форме регистрации. Если сервер недоступен, аккаунт всё равно " +
                    "создаётся, и приложение сообщает, что письмо не отправлено.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}
