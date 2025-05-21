package team.unicon.ukrainemetro.localization

import team.unicon.ukrainemetro.localization.languages.EnglishStrings

/**
 * The common entry point to get the appropriate Strings implementation based on the platform's locale.
 */
fun getStrings(): Strings {
    return when (getPlatformLocale()) {
        Locale.EN -> EnglishStrings()
//        Locale.UK -> UkrainianStrings()
//        Locale.ES -> SpanishStrings()
        else -> EnglishStrings() // Fallback
    }
}