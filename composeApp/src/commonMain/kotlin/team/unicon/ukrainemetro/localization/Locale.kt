package team.unicon.ukrainemetro.localization

/**
 * Represents a language locale. We'll define specific instances for each supported language.
 * This can be expanded to include region codes if needed (e.g., EN_US, EN_GB).
 */
enum class Locale {
    EN, // English
    UK, // Ukrainian
    RU; // Russian

    companion object {
        fun fromString(localeCode: String): Locale {
            return when (localeCode.lowercase()) {
                "en", "en-us", "en-gb" -> EN
                // Disabled temporary
//                "uk", "uk-ua" -> UK
//                "ru" -> RU
                else -> EN // Default fallback locale
            }
        }
    }
}