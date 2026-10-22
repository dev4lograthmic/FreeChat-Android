package io.github.freechat.android.core.ui.locale

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import io.github.freechat.android.core.model.AppLanguage
import java.util.Locale

/**
 * Applies the user's language choice. Android 13+ uses the platform per-app
 * locale API; Android 12/12L fall back to wrapping the base context.
 */
object LocaleController {
    private const val PREFS = "app_locale"
    private const val KEY = "language_tag"
    private const val DEFAULT_TAG = "en"

    fun ensureDefault(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (localeManager.applicationLocales.isEmpty) {
                localeManager.applicationLocales = LocaleList.forLanguageTags(DEFAULT_TAG)
            }
            val tag = localeManager.applicationLocales
                .takeIf { !it.isEmpty }
                ?.get(0)
                ?.toLanguageTag()
                ?: DEFAULT_TAG
            prefs.edit().putString(KEY, tag).apply()
        } else if (!prefs.contains(KEY)) {
            prefs.edit().putString(KEY, DEFAULT_TAG).apply()
        }
    }

    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val locale = Locale.forLanguageTag(storedTag(base))
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, language.tag)
            .apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(language.tag)
        } else {
            (context as? Activity)?.recreate()
        }
    }

    fun currentLanguage(context: Context): AppLanguage =
        AppLanguage.fromTag(storedTag(context))

    private fun storedTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (!locales.isEmpty) {
                val tag = locales.get(0)?.toLanguageTag()
                if (!tag.isNullOrBlank()) return tag
            }
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, DEFAULT_TAG)
            ?: DEFAULT_TAG
    }
}
