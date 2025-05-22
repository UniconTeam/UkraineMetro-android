package team.unicon.ukrainemetro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import team.unicon.ukrainemetro.localization.getStrings
import team.unicon.ukrainemetro.ui.main.MainScreen
import team.unicon.ukrainemetro.ui.main.MainViewModel
import team.unicon.ukrainemetro.ui.theme.AppTheme

import ukrainemetro.composeapp.generated.resources.Res
import ukrainemetro.composeapp.generated.resources.compose_multiplatform

@Composable
fun App() {
    LocalizationProvider {
        AppTheme {
            val mainViewModel = koinViewModel<MainViewModel>()
            MainScreen(mainViewModel)
        }
    }
}