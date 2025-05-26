package team.unicon.ukrainemetro.di

import org.koin.core.module.Module

val commonDiModules: List<Module>
    = listOf(
        localizationDiModule,
        dataSourcesDiModule,
        repositoriesDiModule,
        viewModelsDiModule,
        settingsDiModule,
    )