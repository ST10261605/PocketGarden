package com.example.pocketgarden

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    fun setLocale(context: Context, language: String): Context {
        saveLanguagePreference(context, language)

        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    private fun saveLanguagePreference(context: Context, lang: String) {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        prefs.edit().putString("language", lang).apply()
    }

    fun loadLocale(context: Context): String? {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        return prefs.getString("language", "en")
    }
}
