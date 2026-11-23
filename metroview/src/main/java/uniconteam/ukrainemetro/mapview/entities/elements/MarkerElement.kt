package uniconteam.ukrainemetro.mapview.entities.elements

import uniconteam.ukrainemetro.mapview.entities.Vector

/** Station marker dot without a text label (drawn as a map marker). */
class MarkerElement(
    var pos: Vector,
    var color: Int
): Element {
    init {
        // fixme: hack for big map
        pos.x *= 2
        pos.y *= 2
    }
}
