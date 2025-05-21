package team.unicon.ukrainemetro.localization

/**
 * Represents a localizable string. In this approach, it just holds the actual string value.
 * We can still use a sealed interface if we want different types of localization (e.g., plurals).
 */
sealed interface LocalizedString {
    fun resolve(): String
}