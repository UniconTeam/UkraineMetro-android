package uniconteam.ukrainemetro.features.selecting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import uniconteam.ukrainemetro.core.interactor.UseCase
import uniconteam.ukrainemetro.core.platform.BaseViewModel
import uniconteam.ukrainemetro.features.selecting.usecases.CheckCity
import uniconteam.ukrainemetro.features.selecting.usecases.GetCities
import uniconteam.ukrainemetro.features.selecting.usecases.UpdateCity
import uniconteam.ukrainemetro.mapview.entities.City
import javax.inject.Inject

@HiltViewModel
class SelectMapViewModel @Inject constructor(
    private val getCities: GetCities,
    private val updateCity: UpdateCity,
    private val checkCity: CheckCity
): BaseViewModel() {
    private val _cities: MutableLiveData<List<City>> = MutableLiveData()
    val cities: LiveData<List<City>> = _cities

    private val _isCitySelected: MutableLiveData<Boolean> = MutableLiveData()
    val isCitySelected: LiveData<Boolean> = _isCitySelected

    private fun handleCities(cities: List<City>) {
        _cities.value = cities
    }

    private fun handleSelection(isSelected: Boolean) {
        _isCitySelected.value = isSelected
    }

    fun setCity(cityId: String) {
        updateCity(UpdateCity.Params(cityId))
    }

    fun checkSelection() {
        checkCity(UseCase.None()) { it.fold(::handleFailure, ::handleSelection) }
    }

    fun fetchCities() {
        getCities(UseCase.None()) { it.fold(::handleFailure, ::handleCities) }
    }
}