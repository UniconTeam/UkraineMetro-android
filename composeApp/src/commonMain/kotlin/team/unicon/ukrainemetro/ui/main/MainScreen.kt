package team.unicon.ukrainemetro.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import team.unicon.ukrainemetro.LocalStrings
import ukrainemetro.composeapp.generated.resources.Res
import ukrainemetro.composeapp.generated.resources.compose_multiplatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "UkraineMetro") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
                .padding(16.dp)
        ) {
            Image(
                painterResource(Res.drawable.compose_multiplatform),
                null
            )
            Text(
                text = strings.cityKharkiv.resolve()
            )
        }
    }
}