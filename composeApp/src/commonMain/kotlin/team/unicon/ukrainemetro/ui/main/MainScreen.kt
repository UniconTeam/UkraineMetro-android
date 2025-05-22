package team.unicon.ukrainemetro.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import team.unicon.ukrainemetro.LocalStrings
import ukrainemetro.composeapp.generated.resources.Res
import ukrainemetro.composeapp.generated.resources.compose_multiplatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    LaunchedEffect(Unit)  {
        viewModel.loadSubway()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "UkraineMetro") }
            )
        }
    ) { paddingValues ->
        when(uiState) {
            is MainViewModel.UIState.Loading -> {
                Column(
                    modifier = Modifier.padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            is MainViewModel.UIState.Present -> {
                val presentUiState = (uiState as MainViewModel.UIState.Present)

                Column(
                    modifier = Modifier.padding(paddingValues)
                        .padding(16.dp)
                ) {
                    SubwayMap(
                        modifier = Modifier.fillMaxSize(),
                        elements = presentUiState.subwayInfo.elements
                    )
                }
            }
        }
    }
}