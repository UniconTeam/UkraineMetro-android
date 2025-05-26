package team.unicon.ukrainemetro.settings

import team.unicon.ukrainemetro.localization.Strings

interface SettingsProvider {
    fun registerSetting(setting: Setting): SettingsProviderImpl.ProvidedSetting
}

class SettingsProviderImpl(
    private val strings: Strings
) : SettingsProvider {
    data class ProvidedSetting(
        val setting: Setting
    )

    override fun registerSetting(setting: Setting): ProvidedSetting {
        TODO("Not yet implemented")
    }
}