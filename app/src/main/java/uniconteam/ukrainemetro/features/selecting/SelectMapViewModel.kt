package uniconteam.ukrainemetro.features.selecting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uniconteam.ukrainemetro.core.repositories.MapsRepository
import uniconteam.ukrainemetro.core.repositories.PrefsRepository
import uniconteam.ukrainemetro.mapview.entities.City
import javax.inject.Inject

class SelectMapViewModel @Inject constructor(
    private val mapsRepository: MapsRepository,
    private val prefsRepository: PrefsRepository
): ViewModel() {
    private val _cities: MutableLiveData<List<City>> = MutableLiveData()
    val cities: LiveData<List<City>> = _cities

    fun setCity(cityId: String) {
        prefsRepository.cityId = cityId
    }

    fun isHasCity(): Boolean {
        return prefsRepository.isHasCityId
    }

    fun fetchCities() {
        _cities.value = mapsRepository.fetchAllCities()
    }
}