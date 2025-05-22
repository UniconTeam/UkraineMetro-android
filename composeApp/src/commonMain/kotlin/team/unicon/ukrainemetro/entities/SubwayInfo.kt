package team.unicon.ukrainemetro.entities

import team.unicon.ukrainemetro.entities.elements.Element
import team.unicon.ukrainemetro.localization.LocalizedString

data class SubwayInfo(
    val name: LocalizedString,
    val elements: List<Element>,
)
