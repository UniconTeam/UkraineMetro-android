package uniconteam.ukrainemetro.old

import uniconteam.ukrainemetro.core.repositories.MapsRepository
import uniconteam.ukrainemetro.core.repositories.PrefsRepository

class RepositoriesContainer(
    private val mapsRepository: MapsRepository,
    private val prefsRepository: PrefsRepository
) {
    fun getMapsRepository(): MapsRepository = mapsRepository
    fun getPrefsRepository(): PrefsRepository = prefsRepository
}