package uniconteam.ukrainemetro.repository

import uniconteam.ukrainemetro.cities.DniproCity
import uniconteam.ukrainemetro.cities.KharkivCity
import uniconteam.ukrainemetro.cities.KriviyRogCity
import uniconteam.ukrainemetro.cities.KyivCity
import uniconteam.ukrainemetro.mapview.entities.City

class MapsRepository {
    private val cities = listOf(
        KharkivCity(),
        KyivCity(),
        DniproCity(),
        KriviyRogCity()
    )

    fun fetchAllCities(): List<City> {
        return cities
    }

    fun fetchCityById(id: String): City? {
        return fetchAllCities().find { it.id == id }
    }
}