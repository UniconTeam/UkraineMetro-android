package uniconteam.ukrainemetro.features.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import uniconteam.ukrainemetro.core.interactor.UseCase
import uniconteam.ukrainemetro.core.platform.BaseViewModel
import uniconteam.ukrainemetro.features.map.usecases.GetCity
import uniconteam.ukrainemetro.features.map.usecases.ResetCity
import uniconteam.ukrainemetro.mapview.entities.City
import javax.inject.Inject
@HiltViewModel
class MapViewModel @Inject constructor(
    private val getCity: GetCity,
    private val resetCity: ResetCity
): BaseViewModel() {
    private val _city: MutableLiveData<City> = MutableLiveData()
    val city: LiveData<City> = _city

    private fun handleCity(city: City) {
        _city.value = city
    }

    fun fetchCityMap() {
        getCity(UseCase.None()) { it.fold(::handleFailure, ::handleCity) }
    }

    fun resetCity() {
        resetCity(UseCase.None())
    }
}