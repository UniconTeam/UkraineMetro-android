package team.unicon.ukrainemetro.entities

import team.unicon.ukrainemetro.localization.LocalizedString

data class Point(
    var pos: Vector,
    var name: LocalizedString?
)