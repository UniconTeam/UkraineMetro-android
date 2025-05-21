package team.unicon.ukrainemetro.localization

class SimpleLocalizedString(private val value: String) : LocalizedString {
    override fun resolve(): String = value
}