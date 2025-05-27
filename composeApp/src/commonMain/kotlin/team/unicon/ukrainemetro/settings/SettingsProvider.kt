package team.unicon.ukrainemetro.settings

import team.unicon.ukrainemetro.localization.Strings
import team.unicon.ukrainemetro.settings.SettingsProviderImpl.ProvidedSetting

interface SettingsProvider {
    val allSettings: List<Setting>

    var isUseAlternativeColors: Boolean

   // val appLanguage: String
    fun <T> defineSetting(setting: Setting, defaultValue: T): ProvidedSetting<T>
    fun commit()
}

class SettingsProviderImpl(
    private val strings: Strings
) : SettingsProvider {
    private val useAlternativeColorsSetting = defineBooleanSetting<Boolean>(
        title = "Alternative colors",
        description = "Use styled colors for map branches.",
        key = "alternative_colors",
        defaultValue = false
    )

    override val allSettings: List<Setting> = listOf(
        useAlternativeColorsSetting.setting
    )

    data class ProvidedSetting<T>(
        val setting: Setting,
        var value: T
    )

    override fun <T> defineSetting(setting: Setting, defaultValue: T): ProvidedSetting<T> {
        return ProvidedSetting<T>(setting, defaultValue)
    }

    override fun commit() {
        // TODO
    }

    override var isUseAlternativeColors: Boolean
        get() = useAlternativeColorsSetting.value
        set(it) { useAlternativeColorsSetting.value = it }
}

fun <T : Boolean> SettingsProvider.defineBooleanSetting(
    title: String,
    description: String,
    key: String,
    defaultValue: T
): ProvidedSetting<T> {
    return defineSetting<T>(
        setting = Setting.Switch(
            title = title,
            description = description,
            key = key,
            defaultValue = defaultValue
        ),
        defaultValue = defaultValue
    )
}