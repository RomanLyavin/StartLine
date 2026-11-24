package com.startline.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MailerTest {

    private val config = MailConfig(
        host = "smtp.example.com",
        port = 587,
        user = "bot@example.com",
        password = "app-password",
        from = "bot@example.com",
    )

    @Test
    fun emptyConfigIsNotUsable() {
        val empty = MailConfig(host = "", port = 587, user = "", password = "", from = "")
        assertFalse(empty.isConfigured)
        val error = runCatching { MailSender(empty).send("a@b.com", "t", "b") }.exceptionOrNull()
        assertTrue(error is MailException)
        assertEquals("Почта не настроена", error?.message)
    }

    @Test
    fun messageHasHeadersAndBlankLineBeforeBody() {
        val message = MailSender(config).buildMessage("bot@example.com", "user@example.com", "Тема", "Строка тела")
        assertTrue(message.startsWith("From: bot@example.com\r\nTo: user@example.com\r\n"))
        assertTrue(message.contains("Content-Type: text/plain; charset=UTF-8\r\n"))
        assertTrue(message.contains("\r\n\r\nСтрока тела\r\n"))
    }

    @Test
    fun cyrillicSubjectIsBase64Encoded() {
        val message = MailSender(config).buildMessage("bot@example.com", "user@example.com", "Тема", "body")
        val line = message.lineSequence().first { it.startsWith("Subject: ") }
        val encoded = line.removePrefix("Subject: =?UTF-8?B?").removeSuffix("?=")
        val decoded = String(
            java.util.Base64.getDecoder().decode(encoded),
            Charsets.UTF_8,
        )
        assertEquals("Тема", decoded)
    }

    @Test
    fun asciiSubjectIsNotEncoded() {
        val message = MailSender(config).buildMessage("bot@example.com", "user@example.com", "StartLine", "body")
        assertTrue(message.contains("Subject: StartLine\r\n"))
    }

    @Test
    fun leadingDotIsDoubled() {
        val message = MailSender(config).buildMessage("bot@example.com", "user@example.com", "t", ".top\nsecond")
        assertTrue(message.endsWith("\r\n\r\n..top\r\nsecond\r\n"))
    }

    @Test
    fun welcomeBodyContainsEmailAndPassword() {
        val body = WelcomeMail.body("roman@example.com", "secret123", Instant.parse("2026-11-24T12:00:00Z"))
        assertTrue(body.contains("roman@example.com"))
        assertTrue(body.contains("secret123"))
        assertTrue(body.contains("Аккаунт StartLine создан."))
    }
}
