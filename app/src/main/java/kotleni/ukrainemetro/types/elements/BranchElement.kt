package kotleni.ukrainemetro.types.elements

import kotleni.ukrainemetro.types.Point

/** Branch element of map
 * @param points List of points
 * @param color Rgb color
 */
class BranchElement(
    var points: List<Point>,
    var color: Int
): Element