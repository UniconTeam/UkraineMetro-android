package team.unicon.ukrainemetro.datasource

import team.unicon.ukrainemetro.entities.elements.Element
import team.unicon.ukrainemetro.localization.LocalizedString
import team.unicon.ukrainemetro.localization.Strings

interface SubwayMapDataSource {
    val name: LocalizedString
    val elements: List<Element>
}