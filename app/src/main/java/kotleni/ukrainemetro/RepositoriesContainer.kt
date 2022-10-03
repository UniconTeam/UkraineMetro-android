package kotleni.ukrainemetro

import kotleni.ukrainemetro.repository.MapsRepository
import kotleni.ukrainemetro.repository.PrefsRepository

class RepositoriesContainer(
    private val mapsRepository: MapsRepository,
    private val prefsRepository: PrefsRepository
) {
    fun getMapsRepository(): MapsRepository = mapsRepository
    fun getPrefsRepository(): PrefsRepository = prefsRepository
}