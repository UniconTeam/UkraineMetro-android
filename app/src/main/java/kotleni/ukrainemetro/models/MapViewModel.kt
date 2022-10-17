package kotleni.ukrainemetro.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotleni.ukrainemetro.RepositoriesContainer
import kotleni.ukrainemetro.repository.PrefsRepository
import kotleni.ukrainemetro.types.elements.Element

class MapViewModel(
    private val repositoriesContainer: RepositoriesContainer
): ViewModel() {
    private val mapData: MutableLiveData<List<Element>> = MutableLiveData()

    fun getMapData(): LiveData<List<Element>> {
        return mapData
    }

    fun loadMapData() {
        val city = repositoriesContainer.getPrefsRepository().getCity()
        if(city != null) {
            mapData.value = repositoriesContainer.getMapsRepository().getMap(city)
        }
    }

    fun resetCity() {
        repositoriesContainer.getPrefsRepository().resetCity()
    }
}