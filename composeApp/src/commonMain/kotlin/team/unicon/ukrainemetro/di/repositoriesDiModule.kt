package team.unicon.ukrainemetro.di

import org.koin.dsl.module
import team.unicon.ukrainemetro.repositories.SubwaysRepository
import team.unicon.ukrainemetro.repositories.SubwaysRepositoryImpl

val repositoriesDiModule = module {
    single<SubwaysRepository> { SubwaysRepositoryImpl(get()) }
}