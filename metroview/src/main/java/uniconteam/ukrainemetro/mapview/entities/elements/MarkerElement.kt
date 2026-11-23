package uniconteam.ukrainemetro.mapview.entities.elements

import uniconteam.ukrainemetro.mapview.entities.Vector

/** Station marker dot, optionally with a text label (leader arrows included). */
class MarkerElement(
    var pos: Vector,
    var color: Int,
    var label: String? = null
): Element {
    init {
        // fixme: hack for big map
        pos.x *= 2
        pos.y *= 2
    }
}
