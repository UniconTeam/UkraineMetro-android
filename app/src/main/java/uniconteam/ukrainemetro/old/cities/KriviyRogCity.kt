package uniconteam.ukrainemetro.old.cities

import android.graphics.Color
import unicon.metro.kharkiv.R
import uniconteam.ukrainemetro.mapview.entities.City
import uniconteam.ukrainemetro.mapview.entities.Point
import uniconteam.ukrainemetro.mapview.entities.Vector
import uniconteam.ukrainemetro.mapview.entities.elements.BranchElement
import uniconteam.ukrainemetro.mapview.entities.elements.Element

class KriviyRogCity: City {
    override val id: String
        get() = javaClass.name
    override val name: Int
        get() = R.string.city_kriviyrig
    override val mapElements: List<Element>
        get() = listOf(
            BranchElement(
                listOf(
                    Point(
                        Vector(71, 25),
                        R.string.name_zarichna
                    ),
                    Point(
                        Vector(71, 46),
                        R.string.name_elektrozavodska
                    ),
                    Point(
                        Vector(71, 67),
                        R.string.name_industrialna
                    ),
                    Point(
                        Vector(71, 97),
                        R.string.name_soniachna
                    ),
                    Point(
                        Vector(
                            71,
                            117
                        ), R.string.name_miskaLiakrnia
                    ),
                    Point(
                        Vector(
                            71,
                            138
                        ), R.string.name_vechirniybulvar
                    ),
                    Point(
                        Vector(
                            71,
                            162
                        ), R.string.name_mudriona
                    ),
                    Point(
                        Vector(
                            71,
                            180
                        ), R.string.name_budynokrad
                    ),
                    Point(
                        Vector(
                            71,
                            206
                        ), R.string.name_prospektmetalurhiv
                    ),
                    Point(
                        Vector(
                            77,
                            236
                        ), R.string.name_universytet
                    ),
                    Point(
                        Vector(
                            81,
                            250
                        ), R.string.name_vulytsiavitcyzny
                    ),
                    Point(
                        Vector(
                            94,
                            266
                        ), R.string.name_tretiadilnytsia
                    ),
                    Point(
                        Vector(
                            111,
                            280
                        ), R.string.name_kiltseamkr
                    ),
                    Point(
                        Vector(
                            123,
                            300
                        ), R.string.name_maidanPratsi
                    ),
                    Point(
                        Vector(
                            123,
                            320
                        ), R.string.name_kiltseva
                    ),
                ),
                Color.parseColor("#f22718")
            ),
        )
}