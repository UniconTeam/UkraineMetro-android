package uniconteam.ukrainemetro.old.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uniconteam.ukrainemetro.old.RepositoriesContainer
import uniconteam.ukrainemetro.mapview.entities.City

class SelectMapViewModel(
    private val repositoriesContainer: RepositoriesContainer
): ViewModel() {
    private val _cities: MutableLiveData<List<City>> = MutableLiveData()
    val cities: LiveData<List<City>> = _cities

    fun setCity(cityId: String) {
        repositoriesContainer.getPrefsRepository().cityId = cityId
    }

    fun isHasCity(): Boolean {
        return repositoriesContainer.getPrefsRepository().isHasCityId
    }

    fun fetchCities() {
        _cities.value = repositoriesContainer.getMapsRepository().fetchAllCities()
    }
}