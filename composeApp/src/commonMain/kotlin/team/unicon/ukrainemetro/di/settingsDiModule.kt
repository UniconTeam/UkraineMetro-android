package team.unicon.ukrainemetro.di

import org.koin.dsl.module
import team.unicon.ukrainemetro.localization.Strings
import team.unicon.ukrainemetro.settings.SettingsProvider
import team.unicon.ukrainemetro.settings.SettingsProviderImpl

val settingsDiModule = module {
    single<SettingsProvider> {
        val strings = get<Strings>()
        SettingsProviderImpl(strings)
    }
}