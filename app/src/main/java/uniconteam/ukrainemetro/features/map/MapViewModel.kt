package uniconteam.ukrainemetro.features.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uniconteam.ukrainemetro.core.repositories.MapsRepository
import uniconteam.ukrainemetro.core.repositories.PrefsRepository
import uniconteam.ukrainemetro.mapview.entities.City

class MapViewModel(
    private val mapsRepository: MapsRepository,
    private val prefsRepository: PrefsRepository
): ViewModel() {
    private val _city: MutableLiveData<City> = MutableLiveData()
    val city: LiveData<City> = _city

    fun fetchCityMap() {
        if(prefsRepository.isHasCityId) {
            val cityId = prefsRepository.cityId!!
            _city.value = mapsRepository.fetchCityById(cityId)
        }
    }

    fun resetCity() {
        prefsRepository.cityId = null
    }
}