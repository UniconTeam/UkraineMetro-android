package uniconteam.ukrainemetro.core.cities

import android.graphics.Color
import unicon.metro.kharkiv.R
import uniconteam.ukrainemetro.mapview.entities.City
import uniconteam.ukrainemetro.mapview.entities.Point
import uniconteam.ukrainemetro.mapview.entities.Vector
import uniconteam.ukrainemetro.mapview.entities.elements.BranchElement
import uniconteam.ukrainemetro.mapview.entities.elements.Element
import uniconteam.ukrainemetro.mapview.entities.elements.TransElement

class KyivCity: City {
    override val id: String
        get() = javaClass.name
    override val name: Int
        get() = R.string.city_kyiv
    override val mapElements: List<Element>
        get() = listOf(
            BranchElement(
                listOf(
                    Point(
                        Vector(59, 198),
                        R.string.name_akademistechko
                    ),
                    Point(
                        Vector(90, 230),
                        R.string.name_zhytomyr
                    ),
                    Point(
                        Vector(
                            122,
                            261
                        ), R.string.name_svyatoshyn
                    ),
                    Point(
                        Vector(
                            152,
                            292
                        ), R.string.name_nykvy
                    ),
                    Point(
                        Vector(
                            191,
                            331
                        ), R.string.name_beresteyska
                    ),
                    Point(
                        Vector(
                            228,
                            367
                        ), R.string.name_shulyavska
                    ),
                    Point(
                        Vector(
                            263,
                            402
                        ), R.string.name_polytechnic_institute
                    ),
                    Point(
                        Vector(
                            330,
                            416
                        ), R.string.name_station
                    ),
                    Point(
                        Vector(
                            400,
                            416
                        ), R.string.name_university
                    ),
                    Point(
                        Vector(
                            469,
                            416
                        ), R.string.name_theatrical
                    ),
                    Point(
                        Vector(
                            560,
                            416
                        ), R.string.name_khreshchatyk
                    ),
                    Point(
                        Vector(
                            655,
                            455
                        ), R.string.name_arsenal
                    ),
                    Point(
                        Vector(
                            685,
                            484
                        ), R.string.name_dnipro
                    ),
                    Point(
                        Vector(
                            736,
                            502
                        ), R.string.name_hydropark
                    ),
                    Point(
                        Vector(
                            795,
                            487
                        ), R.string.name_leftbank
                    ),
                    Point(
                        Vector(
                            824,
                            457
                        ), R.string.name_darnytsy
                    ),
                    Point(
                        Vector(
                            853,
                            428
                        ), R.string.name_chernihivska
                    ),
                    Point(
                        Vector(
                            884,
                            399
                        ), R.string.name_lisova
                    ),
                ),
                Color.parseColor("#f22718")
            ),
            BranchElement(
                listOf(
                    Point(
                        Vector(540, 70),
                        R.string.name_heroes_dnipro
                    ),
                    Point(
                        Vector(
                            540,
                            116
                        ), R.string.name_minsk
                    ),
                    Point(
                        Vector(
                            540,
                            162
                        ), R.string.name_obolon
                    ),
                    Point(
                        Vector(
                            540,
                            206
                        ), R.string.name_petrivka
                    ),
                    Point(
                        Vector(
                            540,
                            248
                        ), R.string.name_taras_sxhevchenko
                    ),
                    Point(
                        Vector(
                            540,
                            293
                        ), R.string.name_contract_area
                    ),
                    Point(
                        Vector(
                            540,
                            339
                        ), R.string.name_postal_square
                    ),
                    Point(
                        Vector(
                            540,
                            395
                        ), R.string.name_independence_square
                    ),
                    Point(
                        Vector(
                            478,
                            516 - 10
                        ), R.string.name_leotolstoysquare
                    ),
                    Point(
                        Vector(
                            478,
                            526 - 10
                        ), null
                    ), // for trans
                    Point(
                        Vector(
                            450,
                            568
                        ), R.string.name_olympic
                    ),
                    Point(
                        Vector(
                            450,
                            609
                        ), R.string.name_palaceukraine
                    ),
                    Point(
                        Vector(
                            450,
                            649
                        ), R.string.name_lybidska
                    ),
                    Point(
                        Vector(
                            410,
                            712
                        ), R.string.name_demiivska
                    ),
                    Point(
                        Vector(
                            380,
                            743
                        ), R.string.name_holosiivska
                    ),
                    Point(
                        Vector(
                            350,
                            771
                        ), R.string.name_vasylkivska
                    ),
                    Point(
                        Vector(
                            321,
                            801
                        ), R.string.name_exhibitioncenter
                    ),
                    Point(
                        Vector(
                            266,
                            822
                        ), R.string.name_racetrack
                    ),
                    Point(
                        Vector(
                            195,
                            822
                        ), R.string.name_teremka
                    ),
                ),
                Color.parseColor("#1261ff")
            ),
            BranchElement(
                listOf(
                    Point(
                        Vector(
                            316,
                            250
                        ), R.string.name_syrets
                    ),
                    Point(
                        Vector(
                            364,
                            298
                        ), R.string.name_dorohozhychi
                    ),
                    Point(
                        Vector(
                            401,
                            332
                        ), R.string.name_lukyanivska
                    ),
                    Point(
                        Vector(
                            448,
                            395
                        ), R.string.name_zolotiVorota
                    ),
                    Point(
                        Vector(
                            508,
                            516
                        ), R.string.name_palatssportu
                    ),
                    Point(
                        Vector(
                            508,
                            526
                        ), null
                    ), // for trans
                    Point(
                        Vector(
                            540,
                            569
                        ), R.string.name_klovska
                    ),
                    Point(
                        Vector(
                            540,
                            609
                        ), R.string.name_pecherska
                    ),
                    Point(
                        Vector(
                            540,
                            649
                        ), R.string.name_druzhbynarodiv
                    ),
                    Point(
                        Vector(
                            563,
                            699
                        ), R.string.name_vydubychi
                    ),
                    Point(
                        Vector(
                            632,
                            767
                        ), R.string.name_slavutych
                    ),
                    Point(
                        Vector(
                            665,
                            802
                        ), R.string.name_mushrooms
                    ),
                    Point(
                        Vector(
                            709,
                            822
                        ), R.string.name_poznyaks
                    ),
                    Point(
                        Vector(
                            772,
                            822
                        ), R.string.name_kharkivska
                    ),
                    Point(
                        Vector(
                            819,
                            805
                        ), R.string.name_vyrlitsa
                    ),
                    Point(
                        Vector(
                            850,
                            774
                        ), R.string.name_boryspilska
                    ),
                    Point(
                        Vector(
                            881,
                            743
                        ), R.string.name_chervonyykhutir
                    ),
                ),
                Color.parseColor("#379926")
            ),
            TransElement(
                Vector(
                    448,
                    395
                ), Vector(469, 416)
            ),
            TransElement(
                Vector(
                    540,
                    395
                ), Vector(560, 416)
            ),
            TransElement(
                Vector(
                    508,
                    526
                ), Vector(478, 526 - 10)
            )
        )
}