package com.example.fitnesstracker.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Sprache der App: wie das System, oder fest Deutsch bzw. Englisch. */
enum class AppLanguage(val code: String) {
    SYSTEM("system"),
    GERMAN("de"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

/**
 * Aktuelle Sprache der App. Die Texte stehen direkt im Code als Paar (siehe [tr]); ohne
 * [init] (z. B. in Unit-Tests) ist alles Deutsch.
 */
object L10n {

    private const val PREFS = "language_prefs"
    private const val KEY_LANGUAGE = "language"

    private val _english = MutableStateFlow(false)

    /** true, solange die App Englisch spricht; ändert sich beim Wechsel in den Einstellungen. */
    val englishFlow: StateFlow<Boolean> = _english.asStateFlow()

    val isEnglish: Boolean get() = _english.value

    /** Zahlen- und Datumsformat passend zur Sprache: 82,5 kg bzw. 82.5 kg. */
    val locale: Locale get() = if (isEnglish) Locale.UK else Locale.GERMANY

    val languageCode: String get() = if (isEnglish) "en" else "de"

    fun init(context: Context) {
        _english.value = resolve(load(context))
    }

    fun load(context: Context): AppLanguage =
        AppLanguage.fromCode(prefs(context).getString(KEY_LANGUAGE, null))

    fun save(context: Context, language: AppLanguage) {
        prefs(context).edit().putString(KEY_LANGUAGE, language.code).apply()
        _english.value = resolve(language)
    }

    /** Nur für Tests: Sprache ohne Android-Kontext festlegen. */
    internal fun setEnglishForTest(english: Boolean) {
        _english.value = english
    }

    /** Kontext mit der gewählten Sprache, damit auch Android-Dialoge (Datum, Uhrzeit) passen. */
    fun wrap(base: Context): Context {
        init(base)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }

    private fun resolve(language: AppLanguage): Boolean = when (language) {
        AppLanguage.GERMAN -> false
        AppLanguage.ENGLISH -> true
        AppLanguage.SYSTEM -> Resources.getSystem().configuration.locales[0].language != "de"
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** Text in der aktuellen Sprache: `tr("Speichern", "Save")`. */
fun tr(de: String, en: String): String = if (L10n.isEnglish) en else de
