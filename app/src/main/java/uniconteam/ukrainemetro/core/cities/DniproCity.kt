package uniconteam.ukrainemetro.core.cities

import android.graphics.Color
import unicon.metro.kharkiv.R
import uniconteam.ukrainemetro.mapview.entities.City
import uniconteam.ukrainemetro.mapview.entities.Point
import uniconteam.ukrainemetro.mapview.entities.Vector
import uniconteam.ukrainemetro.mapview.entities.elements.BranchElement
import uniconteam.ukrainemetro.mapview.entities.elements.Element

class DniproCity: City {
    override val id: String
        get() = javaClass.name
    override val name: Int
        get() = R.string.name_dnipro
    override val mapElements: List<Element>
        get() = listOf(
            BranchElement(
                listOf(
                    Point(
                        Vector(
                            372,
                            207
                        ), R.string.name_pokrovska
                    ),
                    Point(
                        Vector(
                            570,
                            207
                        ), R.string.name_prospektSvobody
                    ),
                    Point(
                        Vector(
                            756,
                            267
                        ), R.string.name_zavodska
                    ),
                    Point(
                        Vector(
                            967,
                            270
                        ), R.string.name_metalurhiv
                    ),
                    Point(
                        Vector(
                            1162,
                            310
                        ), R.string.name_metrobudivnykiv
                    ),
                    Point(
                        Vector(
                            1370,
                            310
                        ), R.string.name_vokzalna
                    ),
                ),
                Color.parseColor("#f22718")
            ),
        )
}