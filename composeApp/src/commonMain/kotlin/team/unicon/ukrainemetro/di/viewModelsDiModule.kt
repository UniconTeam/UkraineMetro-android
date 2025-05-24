package team.unicon.ukrainemetro.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import team.unicon.ukrainemetro.ui.main.MainViewModel
import team.unicon.ukrainemetro.ui.settings.SettingsViewModel

val viewModelsDiModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { SettingsViewModel() }
}