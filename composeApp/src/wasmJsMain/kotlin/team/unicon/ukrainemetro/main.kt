package team.unicon.ukrainemetro

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import team.unicon.ukrainemetro.di.initKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin {

    }
    ComposeViewport(document.body!!) {
        App()
    }
}