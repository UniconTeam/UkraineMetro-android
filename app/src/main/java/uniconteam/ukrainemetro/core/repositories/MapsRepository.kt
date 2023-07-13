package uniconteam.ukrainemetro.core.repositories

import uniconteam.ukrainemetro.core.cities.DniproCity
import uniconteam.ukrainemetro.core.cities.KharkivCity
import uniconteam.ukrainemetro.core.cities.KriviyRogCity
import uniconteam.ukrainemetro.core.cities.KyivCity
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