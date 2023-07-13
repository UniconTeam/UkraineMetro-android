package uniconteam.ukrainemetro.core.cities

import android.graphics.Color
import unicon.metro.kharkiv.R
import uniconteam.ukrainemetro.mapview.entities.City
import uniconteam.ukrainemetro.mapview.entities.Point
import uniconteam.ukrainemetro.mapview.entities.Vector
import uniconteam.ukrainemetro.mapview.entities.elements.BranchElement
import uniconteam.ukrainemetro.mapview.entities.elements.Element
import uniconteam.ukrainemetro.mapview.entities.elements.TransElement

class KharkivCity: City {
    override val id: String
        get() = javaClass.name
    override val name: Int
        get() = R.string.city_kharkiv
    override val mapElements: List<Element>
        get() = listOf(
            BranchElement(
                listOf(
                    Point(
                        Vector(173, 34),
                        R.string.name_peremoga
                    ),
                    Point(
                        Vector(190, 51),
                        R.string.name_olecsiivka
                    ),
                    Point(
                        Vector(209, 81),
                        R.string.name_23serpnya
                    ),
                    Point(
                        Vector(
                            209,
                            104
                        ), R.string.name_botanichniysad
                    ),
                    Point(
                        Vector(
                            209,
                            127
                        ), R.string.name_naukova
                    ),
                    Point(
                        Vector(
                            209,
                            151
                        ), R.string.name_dergprom
                    ),
                    Point(
                        Vector(
                            255,
                            208
                        ), R.string.name_arh_beketova
                    ),
                    Point(
                        Vector(
                            278,
                            231
                        ), R.string.name_zah_ukr
                    ),
                    Point(
                        Vector(
                            302,
                            267
                        ), R.string.name_metrobud
                    )
                ),
                Color.parseColor("#379926")
            ),
            BranchElement(
                listOf(
                    Point(
                        Vector(22, 279),
                        R.string.name_holodnagora
                    ),
                    Point(
                        Vector(46, 256),
                        R.string.name_pivdvokzal
                    ),
                    Point(
                        Vector(69, 233),
                        R.string.name_centrarinok
                    ),
                    Point(
                        Vector(
                            151,
                            208
                        ), R.string.name_maydankonst
                    ),
                    Point(
                        Vector(
                            197,
                            243
                        ), R.string.name_prospect_gagarnina
                    ),
                    Point(
                        Vector(
                            290,
                            278
                        ), R.string.name_sportivna
                    ),
                    Point(
                        Vector(
                            337,
                            290
                        ), R.string.name_zavimenimalisheva
                    ),
                    Point(
                        Vector(
                            360,
                            313
                        ), R.string.name_turboatom
                    ),
                    Point(
                        Vector(
                            383,
                            337
                        ), R.string.name_palacsporta
                    ),
                    Point(
                        Vector(
                            407,
                            359
                        ), R.string.name_armiyska
                    ),
                    Point(
                        Vector(
                            419,
                            383
                        ), R.string.name_imosmaselscogo
                    ),
                    Point(
                        Vector(
                            419,
                            407
                        ), R.string.name_traktorzavod
                    ),
                    Point(
                        Vector(
                            419,
                            430
                        ), R.string.name_industrial
                    )
                ),
                Color.parseColor("#f22718")
            ),
            BranchElement(
                listOf(
                    Point(
                        Vector(407, 58),
                        R.string.name_garoivwork
                    ),
                    Point(
                        Vector(384, 81),
                        R.string.name_studenstka
                    ),
                    Point(
                        Vector(
                            359,
                            105
                        ), R.string.name_akadempavl
                    ),
                    Point(
                        Vector(
                            336,
                            127
                        ), R.string.name_barabashova
                    ),
                    Point(
                        Vector(
                            313,
                            151
                        ), R.string.name_kievska
                    ),
                    Point(
                        Vector(
                            280,
                            163
                        ), R.string.name_pushkinska
                    ),
                    Point(
                        Vector(
                            196,
                            163
                        ), R.string.name_univer
                    ),
                    Point(
                        Vector(
                            167,
                            163
                        ), null
                    ),
                    Point(
                        Vector(
                            163,
                            167
                        ), null
                    ),
                    Point(
                        Vector(
                            162,
                            197
                        ), R.string.name_istormisei
                    )
                ),
                Color.parseColor("#1261ff")
            ),

            TransElement(
                Vector(151, 208),
                Vector(162, 197)
            ),

            TransElement(
                Vector(290, 278),
                Vector(302, 267)
            )
        )

}