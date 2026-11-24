package com.startline.app.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Текст письма с данными для входа (пункт «отправка пароля на почту при
 * регистрации»). Функции чистые, чтобы их можно было тестировать.
 */
object WelcomeMail {

    const val SUBJECT = "StartLine: данные для входа"

    fun body(email: String, password: String, at: Instant = Instant.now()): String {
        val local = at.atZone(ZoneId.systemDefault()).format(
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        )
        return buildString {
            append("Аккаунт StartLine создан.\n\n")
            append("Email: ").append(email).append("\n")
            append("Пароль: ").append(password).append("\n")
            append("Дата регистрации: ").append(local).append("\n\n")
            append("Пароль хранится на устройстве в виде хэша, это письмо — единственное место, ")
            append("где он записан открытым текстом. Сохраните его и не пересылайте.")
        }
    }
}
