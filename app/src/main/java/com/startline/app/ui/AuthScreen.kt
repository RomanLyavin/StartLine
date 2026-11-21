package com.startline.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startline.app.AppViewModel

@Composable
fun AuthScreen(vm: AppViewModel) {
    var mode by rememberSaveable { mutableStateOf(0) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val loading = vm.authLoading
    val error = localError ?: vm.authError

    fun submit() {
        localError = null
        if (email.isBlank() || password.isBlank()) {
            localError = "Заполните email и пароль"
            return
        }
        if (mode == 1 && password != confirm) {
            localError = "Пароли не совпадают"
            return
        }
        if (mode == 1) vm.register(email, password) else vm.login(email, password)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF131C24), Bg, Bg)
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(48.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Lime),
                contentAlignment = Alignment.Center,
            ) {
                Text("SL", style = MaterialTheme.typography.titleLarge, color = Bg, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "STARTLINE",
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary,
                letterSpacing = 2.sp,
            )
            Text(
                "План тренировок для бега и велосипеда",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(30.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface2)
                            .padding(4.dp),
                    ) {
                        ModeTab("Вход", mode == 0, Modifier.weight(1f)) {
                            mode = 0
                            localError = null
                            vm.clearAuthError()
                        }
                        ModeTab("Регистрация", mode == 1, Modifier.weight(1f)) {
                            mode = 1
                            localError = null
                            vm.clearAuthError()
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Field(
                        label = "Email",
                        value = email,
                        onValueChange = { email = it; localError = null; vm.clearAuthError() },
                        keyboardType = KeyboardType.Email,
                        placeholder = "you@example.com",
                    )
                    Spacer(Modifier.height(10.dp))
                    Field(
                        label = "Пароль",
                        value = password,
                        onValueChange = { password = it; localError = null; vm.clearAuthError() },
                        keyboardType = KeyboardType.Password,
                        password = true,
                        placeholder = "не короче 6 символов",
                    )
                    if (mode == 1) {
                        Spacer(Modifier.height(10.dp))
                        Field(
                            label = "Повторите пароль",
                            value = confirm,
                            onValueChange = { confirm = it; localError = null },
                            keyboardType = KeyboardType.Password,
                            password = true,
                        )
                    }
                    if (error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(error, color = Red, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton(
                        text = if (mode == 0) "Войти" else "Создать аккаунт",
                        onClick = { submit() },
                        loading = loading,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Аккаунт, профиль и планы хранятся локально на устройстве. API-ключи шифруются через Android Keystore.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "intervals.icu подключается в профиле: нужен только API-ключ.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ModeTab(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) Lime else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Bg else TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
