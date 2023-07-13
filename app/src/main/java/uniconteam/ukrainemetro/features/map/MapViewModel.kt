package uniconteam.ukrainemetro.features.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import uniconteam.ukrainemetro.core.platform.BaseViewModel
import uniconteam.ukrainemetro.core.repositories.MapsRepository
import uniconteam.ukrainemetro.core.repositories.PrefsRepository
import uniconteam.ukrainemetro.mapview.entities.City
import javax.inject.Inject
@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapsRepository: MapsRepository,
    private val prefsRepository: PrefsRepository
): BaseViewModel() {
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