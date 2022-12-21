package kotleni.ukrainemetro.types

/** Map point
 * @param pos Position vector
 * @param name Name of point
 */
class Point(var pos: Vector, var name: Int?) {
    init {
        // fixme: hack for big map
        pos.x *= 2
        pos.y *= 2
    }
}