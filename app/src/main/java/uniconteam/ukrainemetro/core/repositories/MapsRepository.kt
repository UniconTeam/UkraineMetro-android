package uniconteam.ukrainemetro.core.repositories

import uniconteam.ukrainemetro.old.cities.DniproCity
import uniconteam.ukrainemetro.old.cities.KharkivCity
import uniconteam.ukrainemetro.old.cities.KriviyRogCity
import uniconteam.ukrainemetro.old.cities.KyivCity
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