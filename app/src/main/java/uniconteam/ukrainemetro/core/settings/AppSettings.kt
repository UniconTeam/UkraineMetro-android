package uniconteam.ukrainemetro.core.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import uniconteam.ukrainemetro.core.repositories.PrefsRepository

/**
 * Applies persisted language + theme settings through AppCompatDelegate
 * (per-app locales, modern night-mode API). Called once at startup and
 * on every change in the settings screen.
 */
object AppSettings {
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    const val LANG_SYSTEM = "system"

    fun applyAll(prefs: PrefsRepository) {
        applyTheme(prefs.themeMode)
        applyLanguage(prefs.language)
    }

    fun applyTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun applyLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag == LANG_SYSTEM) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(tag)
        )
    }
}
