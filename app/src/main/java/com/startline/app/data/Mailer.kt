package com.startline.app.data

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * Настройка SMTP. Значения берутся из BuildConfig (файл mail.properties,
 * который не попадает в git), поэтому реквизиты не хранятся в исходниках.
 */
data class MailConfig(
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val from: String,
    val useStartTls: Boolean = true,
) {
    val isConfigured: Boolean
        get() = host.isNotBlank() && user.isNotBlank() && password.isNotBlank() && from.isNotBlank()
}

class MailException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Отправка письма по SMTP (EHLO, STARTTLS, AUTH LOGIN). Без сторонних
 * библиотек: Android не содержит javax.mail.
 */
class MailSender(private val config: MailConfig) {

    /** Собирает письмо RFC 2822. Точки в начале строки удваиваются (dot-stuffing). */
    fun buildMessage(from: String, to: String, subject: String, body: String): String {
        val sb = StringBuilder()
        sb.append("From: ").append(from).append("\r\n")
        sb.append("To: ").append(to).append("\r\n")
        sb.append("Subject: ").append(encodeHeader(subject)).append("\r\n")
        sb.append("Date: ").append(ZonedDateTime.now().format(RFC_DATE)).append("\r\n")
        sb.append("MIME-Version: 1.0\r\n")
        sb.append("Content-Type: text/plain; charset=UTF-8\r\n")
        sb.append("Content-Transfer-Encoding: 7bit\r\n")
        sb.append("\r\n")
        for (line in body.replace("\r\n", "\n").split("\n")) {
            if (line.startsWith(".")) sb.append(".")
            sb.append(line).append("\r\n")
        }
        return sb.toString()
    }

    /** Отправляет письмо. Бросает MailException, если сервер отказал. */
    fun send(to: String, subject: String, body: String) {
        if (!config.isConfigured) throw MailException("Почта не настроена")
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(config.host, config.port), 25_000)
            var reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            var out = socket.getOutputStream()

            expect(reader, 220)
            command(out, reader, "EHLO mobile", 250)

            if (config.useStartTls) {
                command(out, reader, "STARTTLS", 250)
                val ssl = javax.net.ssl.SSLContext.getDefault().socketFactory
                    .createSocket(socket, config.host, config.port, true)
                reader = BufferedReader(InputStreamReader(ssl.getInputStream(), Charsets.UTF_8))
                out = ssl.getOutputStream()
                command(out, reader, "EHLO mobile", 250)
            }

            command(out, reader, "AUTH LOGIN", 334)
            command(out, reader, b64(config.user), 334)
            command(out, reader, b64(config.password), 235)
            command(out, reader, "MAIL FROM:<${config.from}>", 250)
            command(out, reader, "RCPT TO:<$to>", 250)
            command(out, reader, "DATA", 354)
            out.write(buildMessage(config.from, to, subject, body).toByteArray(Charsets.UTF_8))
            // Терминатор тела письма — строка из одной точки.
            out.write(".\r\n".toByteArray(Charsets.UTF_8))
            out.flush()
            expect(reader, 250)
            command(out, reader, "QUIT", 221)
        } catch (e: IOException) {
            throw MailException("Не удалось отправить письмо: ${e.message}", e)
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun command(out: OutputStream, reader: BufferedReader, text: String, expected: Int) {
        out.write((text + "\r\n").toByteArray(Charsets.UTF_8))
        out.flush()
        expect(reader, expected)
    }

    /** Читает ответ SMTP; многострочный ответ (250-…) доводится до последней строки. */
    private fun expect(reader: BufferedReader, expected: Int) {
        var line = reader.readLine() ?: throw MailException("SMTP закрыл соединение без ответа")
        while (line.length >= 4 && line[3] == '-') {
            line = reader.readLine() ?: throw MailException("SMTP закрыл соединение без ответа")
        }
        if (!line.startsWith(expected.toString())) throw MailException("SMTP ответил: $line")
    }

    private fun b64(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    /** Тема с кириллицей кодируется как MIME encoded-word, иначе сервер её испортит. */
    private fun encodeHeader(value: String): String {
        if (value.all { it.code < 128 }) return value
        return "=?UTF-8?B?" + b64(value) + "?="
    }

    private companion object {
        val RFC_DATE = DateTimeFormatter.RFC_1123_DATE_TIME
    }
}
