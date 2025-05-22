package team.unicon.ukrainemetro.di

import org.koin.dsl.module
import team.unicon.ukrainemetro.datasource.KharkivSubwayMapDataSource

val dataSourcesDiModule = module {
    single { KharkivSubwayMapDataSource() }
}