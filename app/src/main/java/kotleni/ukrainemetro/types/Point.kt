package kotleni.ukrainemetro.types

class Point(var pos: Vector, var name: Int?) {
    init {
        // fixme: hack for big map
        pos.x *= 2
        pos.y *= 2
    }
}