package com.startline.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Ссылки приложения (экран «О программе» и кнопка «Поддержать» на главном
 * экране). Держатся в одном месте, чтобы не плодить копии в экранах.
 */
object AppLinks {
    const val SUPPORT_URL = "https://t.tb.ru/pm_short/AOdA22MAdsS"
    const val DEVELOPER_URL = "https://github.com/RomanLyavin"

    fun openUrl(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_LONG).show()
        }
    }
}
