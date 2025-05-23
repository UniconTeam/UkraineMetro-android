package team.unicon.ukrainemetro.di

import team.unicon.ukrainemetro.di.commonDiModules
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration(this)
    modules(commonDiModules)
}