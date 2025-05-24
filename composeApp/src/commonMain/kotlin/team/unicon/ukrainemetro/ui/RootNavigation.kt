package team.unicon.ukrainemetro.ui

import kotlinx.serialization.Serializable

sealed class RootNavigation {
    @Serializable
    data object Main : RootNavigation()

    @Serializable
    data object Settings : RootNavigation()
}