package uniconteam.ukrainemetro

import uniconteam.ukrainemetro.repository.MapsRepository
import uniconteam.ukrainemetro.repository.PrefsRepository

class RepositoriesContainer(
    private val mapsRepository: MapsRepository,
    private val prefsRepository: PrefsRepository
) {
    fun getMapsRepository(): MapsRepository = mapsRepository
    fun getPrefsRepository(): PrefsRepository = prefsRepository
}