package team.unicon.ukrainemetro.datasource

import team.unicon.ukrainemetro.entities.elements.Element
import team.unicon.ukrainemetro.localization.Strings

interface SubwayMapDataSource {
    fun getMapElements(strings: Strings): List<Element>
}