package kotleni.ukrainemetro.models

import androidx.lifecycle.ViewModel
import kotleni.ukrainemetro.RepositoriesContainer
import kotleni.ukrainemetro.repository.PrefsRepository
import kotleni.ukrainemetro.types.City

class SelectMapViewModel(
    private val repositoriesContainer: RepositoriesContainer
): ViewModel() {
    fun setCity(city: City) {
        repositoriesContainer.getPrefsRepository().setCity(city)
    }
}