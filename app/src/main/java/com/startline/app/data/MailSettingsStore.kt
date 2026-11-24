package com.startline.app.data

import android.content.Context
import com.startline.app.BuildConfig

/**
 * Настройки SMTP, которые пользователь может ввести прямо в приложении.
 * Пароль хранится шифрованным через Android Keystore (SecretStore).
 * Если настройки пустые, используется BuildConfig (mail.properties на сборке).
 */
class MailSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("startline_mail", Context.MODE_PRIVATE)
    private val secrets = SecretStore()

    fun load(): MailConfig {
        val stored = MailConfig(
            host = prefs.getString("host", "").orEmpty(),
            port = prefs.getInt("port", 0),
            user = prefs.getString("user", "").orEmpty(),
            password = secrets.decrypt(prefs.getString("password", "").orEmpty()),
            from = prefs.getString("from", "").orEmpty(),
            useStartTls = prefs.getBoolean("starttls", true),
        )
        return if (stored.isConfigured) stored else defaultConfig()
    }

    fun save(config: MailConfig) {
        prefs.edit()
            .putString("host", config.host.trim())
            .putInt("port", config.port)
            .putString("user", config.user.trim())
            .putString("password", secrets.encrypt(config.password))
            .putString("from", config.from.trim())
            .putBoolean("starttls", config.useStartTls)
            .apply()
    }

    private fun defaultConfig(): MailConfig = MailConfig(
        host = BuildConfig.MAIL_HOST,
        port = BuildConfig.MAIL_PORT,
        user = BuildConfig.MAIL_USER,
        password = BuildConfig.MAIL_PASSWORD,
        from = BuildConfig.MAIL_FROM,
        useStartTls = BuildConfig.MAIL_STARTTLS,
    )
}
