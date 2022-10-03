package kotleni.ukrainemetro.repository

import kotleni.ukrainemetro.MapsData
import kotleni.ukrainemetro.types.City
import kotleni.ukrainemetro.types.elements.Element

class MapsRepository {
    fun getMap(city: City): List<Element> {
        return when(city) {
            City.Kharkiv -> MapsData.kharkiv
            City.Kyiv -> MapsData.kyiv
            City.KriviyRig -> MapsData.kriviyrog
            City.Dnipro -> MapsData.dnipro
        }
    }
}