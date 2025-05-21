package team.unicon.ukrainemetro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import team.unicon.ukrainemetro.localization.Strings
import team.unicon.ukrainemetro.localization.getStrings

/**
 * CompositionLocal for providing the current Strings instance to the Composable tree.
 */
val LocalStrings = staticCompositionLocalOf<Strings> {
    error("No Strings provided. Ensure LocalizationProvider is used.")
}

/**
 * A Composable that provides the correct Strings instance to its content.
 * This should typically wrap your main application Composable.
 */
@Composable
fun LocalizationProvider(
    content: @Composable () -> Unit
) {
    // We use remember here to ensure the Strings instance is stable
    // across recompositions, unless getStrings() itself changes due to
    // a language change trigger (which we'll handle outside this Composable).
    val strings = remember { getStrings() } // Get the appropriate Strings based on platform locale

    CompositionLocalProvider(LocalStrings provides strings) {
        content()
    }
}