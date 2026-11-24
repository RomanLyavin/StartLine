package com.startline.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.util.Base64

/**
 * Проверка порядка команд SMTP на локальном сервере-заглушке. TLS здесь не
 * используется, но последовательность EHLO → AUTH LOGIN → MAIL FROM → RCPT TO
 * → DATA → QUIT проверяется целиком, включая base64-логин и dot-stuffing.
 */
class MailSenderTest {

    private val received = StringBuilder()

    private fun b64(value: String) = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    @Test
    fun sendsMessageThroughFakeSmtpServer() {
        val server = ServerSocket(0)
        val port = server.localPort
        val config = MailConfig(
            host = "127.0.0.1",
            port = port,
            user = "bot@example.com",
            password = "app-password",
            from = "bot@example.com",
            useStartTls = false,
        )

        val serverThread = Thread {
            try {
                server.accept().use { socket ->
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                    val out = socket.getOutputStream()

                    fun read(): String? {
                        val line = reader.readLine()
                        if (line != null) received.append(line).append('\n')
                        return line
                    }

                    fun reply(code: Int, text: String) {
                        out.write(("$code $text\r\n").toByteArray(Charsets.UTF_8))
                        out.flush()
                    }

                    reply(220, "fake smtp ready")
                    read()                                  // EHLO
                    reply(250, "fake smtp greets you")
                    read()                                  // AUTH LOGIN
                    reply(334, "Username:")
                    read()                                  // base64 user
                    reply(334, "Password:")
                    read()                                  // base64 password
                    reply(235, "authenticated")
                    read()                                  // MAIL FROM
                    reply(250, "sender ok")
                    read()                                  // RCPT TO
                    reply(250, "recipient ok")
                    read()                                  // DATA
                    reply(354, "start mail input")
                    var line = read()
                    while (line != null && line != ".") line = read()
                    reply(250, "message accepted")
                    read()                                  // QUIT
                    reply(221, "bye")
                }
            } catch (e: Exception) {
                received.append("server error: ").append(e.message).append('\n')
            } finally {
                runCatching { server.close() }
            }
        }

        serverThread.start()
        MailSender(config).send("roman@example.com", "Test", ".line with leading dot")
        serverThread.join(10_000)

        val log = received.toString()
        assertEquals("EHLO mobile", log.lines()[0])
        assertEquals("AUTH LOGIN", log.lines()[1])
        assertEquals(b64("bot@example.com"), log.lines()[2])
        assertEquals(b64("app-password"), log.lines()[3])
        assertEquals("MAIL FROM:<bot@example.com>", log.lines()[4])
        assertEquals("RCPT TO:<roman@example.com>", log.lines()[5])
        assertEquals("DATA", log.lines()[6])
        assertTrue(log.contains("To: roman@example.com"))
        assertTrue(log.contains("Subject: Test"))
        assertTrue(log.contains("..line with leading dot"))
        assertTrue(log.lines().contains("."))
        assertTrue(log.endsWith("QUIT\n"))
    }

    @Test
    fun unconfiguredMailDoesNotConnect() {
        val error = runCatching {
            MailSender(MailConfig("", 25, "", "", "")).send("a@b.com", "t", "b")
        }.exceptionOrNull()
        assertEquals("Почта не настроена", error?.message)
    }
}
