package uniconteam.ukrainemetro.mapview.entities.elements

import uniconteam.ukrainemetro.mapview.entities.Point

/** Branch element of map
 * @param points List of points
 * @param color Rgb color
 */
class BranchElement(
    var points: List<Point>,
    var color: Int
): Element