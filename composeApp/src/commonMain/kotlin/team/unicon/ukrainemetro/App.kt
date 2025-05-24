package team.unicon.ukrainemetro

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.compose.viewmodel.koinViewModel
import team.unicon.ukrainemetro.ui.main.MainScreen
import team.unicon.ukrainemetro.ui.main.MainViewModel
import team.unicon.ukrainemetro.ui.settings.SettingsViewModel
import team.unicon.ukrainemetro.ui.theme.AppTheme

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import team.unicon.ukrainemetro.ui.RootNavigation
import team.unicon.ukrainemetro.ui.settings.SettingsScreen

@Composable
fun App() {
    LocalizationProvider {
        AppTheme {
            val mainViewModel = koinViewModel<MainViewModel>()
            val settingsViewModel = koinViewModel<SettingsViewModel>()

            val backStack = remember { mutableStateListOf<RootNavigation>(RootNavigation.Main) }

            NavDisplay(
                backStack = backStack,
                onBack = { numToPop ->
                    repeat(numToPop) { backStack.removeLastOrNull() }
                },
                entryProvider = { route ->
                    when(route) {
                        is RootNavigation.Main -> NavEntry(route) {
                            MainScreen(
                                viewModel = mainViewModel,
                                onNavigateToSettings = { backStack.add(RootNavigation.Settings) },
                            )
                        }
                        is RootNavigation.Settings -> NavEntry(route) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { backStack.removeLastOrNull() },
                            )
                        }
                    }
                }
            )
        }
    }
}