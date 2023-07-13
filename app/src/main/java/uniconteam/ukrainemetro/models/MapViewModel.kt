package uniconteam.ukrainemetro.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uniconteam.ukrainemetro.RepositoriesContainer
import uniconteam.ukrainemetro.mapview.entities.City
import uniconteam.ukrainemetro.mapview.entities.elements.Element

class MapViewModel(
    private val repositoriesContainer: RepositoriesContainer
): ViewModel() {
    private val _city: MutableLiveData<City> = MutableLiveData()
    val city: LiveData<City> = _city

    fun fetchCityMap() {
        if(repositoriesContainer.getPrefsRepository().isHasCityId) {
            val cityId = repositoriesContainer.getPrefsRepository().cityId!!
            _city.value = repositoriesContainer.getMapsRepository().fetchCityById(cityId)
        }
    }

    fun resetCity() {
        repositoriesContainer.getPrefsRepository().cityId = null
    }
}