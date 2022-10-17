package kotleni.ukrainemetro.types.elements

import kotleni.ukrainemetro.types.Vector

class TransElement(
    var from: Vector,
    var to: Vector
): Element {
    init {
        // fixme: hack for big map
        from.x *= 2
        from.y *= 2

        to.x *= 2
        to.y *= 2
    }
}