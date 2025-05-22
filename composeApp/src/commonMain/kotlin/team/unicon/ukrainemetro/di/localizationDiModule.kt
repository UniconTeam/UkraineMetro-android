package team.unicon.ukrainemetro.di

import org.koin.dsl.module
import team.unicon.ukrainemetro.localization.getStrings

val localizationDiModule = module {
    single { getStrings() }
}