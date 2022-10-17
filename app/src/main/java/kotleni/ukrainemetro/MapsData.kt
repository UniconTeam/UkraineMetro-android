package kotleni.ukrainemetro

import android.graphics.Color
import unicon.metro.kharkiv.R
import kotleni.ukrainemetro.types.Point
import kotleni.ukrainemetro.types.Vector
import kotleni.ukrainemetro.types.elements.Element
import kotleni.ukrainemetro.types.elements.BranchElement
import kotleni.ukrainemetro.types.elements.TransElement

object MapsData {
    val kharkiv: List<Element> = listOf(
        BranchElement(
            listOf(
                Point(Vector(173, 34), R.string.name_peremoga),
                Point(Vector(190, 51), R.string.name_olecsiivka),
                Point(Vector(209, 81), R.string.name_23serpnya),
                Point(Vector(209, 104), R.string.name_botanichniysad),
                Point(Vector(209, 127), R.string.name_naukova),
                Point(Vector(209, 151), R.string.name_dergprom),
                Point(Vector(255, 208), R.string.name_arh_beketova),
                Point(Vector(278, 231), R.string.name_zah_ukr),
                Point(Vector(302, 267), R.string.name_metrobud)
            ),
            Color.parseColor("#379926")
        ),
        BranchElement(
            listOf(
                Point(Vector(22, 279), R.string.name_holodnagora),
                Point(Vector(46, 256), R.string.name_pivdvokzal),
                Point(Vector(69, 233), R.string.name_centrarinok),
                Point(Vector(151, 208), R.string.name_maydankonst),
                Point(Vector(197, 243), R.string.name_prospect_gagarnina),
                Point(Vector(290, 278), R.string.name_sportivna),
                Point(Vector(337, 290), R.string.name_zavimenimalisheva),
                Point(Vector(360, 313), R.string.name_turboatom),
                Point(Vector(383, 337), R.string.name_palacsporta),
                Point(Vector(407, 359), R.string.name_armiyska),
                Point(Vector(419, 383), R.string.name_imosmaselscogo),
                Point(Vector(419, 407), R.string.name_traktorzavod),
                Point(Vector(419, 430), R.string.name_industrial)
            ),
            Color.parseColor("#f22718")
        ),
        BranchElement(
            listOf(
                Point(Vector(407, 58), R.string.name_garoivwork),
                Point(Vector(384, 81), R.string.name_studenstka),
                Point(Vector(359, 105), R.string.name_akadempavl),
                Point(Vector(336, 127), R.string.name_barabashova),
                Point(Vector(313, 151), R.string.name_kievska),
                Point(Vector(280, 163), R.string.name_pushkinska),
                Point(Vector(196, 163), R.string.name_univer),
                Point(Vector(167, 163), null),
                Point(Vector(163, 167), null),
                Point(Vector(162, 197), R.string.name_istormisei)
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
    val kyiv: List<Element> = listOf(
        BranchElement(
            listOf(
                Point(Vector(59, 198), R.string.name_akademistechko),
                Point(Vector(90, 230), R.string.name_zhytomyr),
                Point(Vector(122, 261), R.string.name_svyatoshyn),
                Point(Vector(152, 292), R.string.name_nykvy),
                Point(Vector(191, 331), R.string.name_beresteyska),
                Point(Vector(228, 367), R.string.name_shulyavska),
                Point(Vector(263,402 ), R.string.name_polytechnic_institute),
                Point(Vector(330, 416), R.string.name_station),
                Point(Vector(400,416 ), R.string.name_university),
                Point(Vector(469, 416), R.string.name_theatrical),
                Point(Vector(560, 416), R.string.name_khreshchatyk),
                Point(Vector(655, 455), R.string.name_arsenal),
                Point(Vector(685, 484), R.string.name_dnipro),
                Point(Vector(736, 502), R.string.name_hydropark),
                Point(Vector(795, 487), R.string.name_leftbank),
                Point(Vector(824, 457), R.string.name_darnytsy),
                Point(Vector(853, 428), R.string.name_chernihivska),
                Point(Vector(884, 399), R.string.name_lisova),
               /*  Point(Vector(, ), R.string.),*/
            ),
            Color.parseColor("#f22718")
        ),
        BranchElement(
            listOf(
                Point(Vector(540, 70), R.string.name_heroes_dnipro),
                Point(Vector(540, 116), R.string.name_minsk),
                Point(Vector(540, 162), R.string.name_obolon),
                Point(Vector(540, 206), R.string.name_petrivka),
                Point(Vector(540, 248), R.string.name_taras_sxhevchenko),
                Point(Vector(540, 293), R.string.name_contract_area),
                Point(Vector(540, 339), R.string.name_postal_square),
                Point(Vector(540, 395), R.string.name_independence_square),
                Point(Vector(478, 516), R.string.name_leotolstoysquare),
                Point(Vector(450, 568), R.string.name_olympic),
                Point(Vector(450, 609), R.string.name_palaceukraine),
                Point(Vector(450, 649), R.string.name_lybidska),
                Point(Vector(410, 712), R.string.name_demiivska),
                Point(Vector(380, 743), R.string.name_holosiivska),
                Point(Vector(350, 771), R.string.name_vasylkivska),
                Point(Vector(321, 801), R.string.name_exhibitioncenter),
                Point(Vector(266, 822), R.string.name_racetrack),
                Point(Vector(195, 822), R.string.name_teremka),
               /*  Point(Vector(, ), R.string.),*/
            ),
            Color.parseColor("#1261ff")//blue
        ),
        BranchElement(
            listOf(
                Point(Vector(316, 250), R.string.name_syrets),
                Point(Vector(364, 298), R.string.name_dorohozhychi),
                Point(Vector(401,332 ), R.string.name_lukyanivska),
                Point(Vector(448,395 ), R.string.name_zolotiVorota),
                Point(Vector(508,516 ), R.string.name_palatssportu),
                Point(Vector(540,569 ), R.string.name_klovska),
                Point(Vector(540,609 ), R.string.name_pecherska),
                Point(Vector(540,649 ), R.string.name_druzhbynarodiv),
                Point(Vector(563,699 ), R.string.name_vydubychi),
                Point(Vector(632,767 ), R.string.name_slavutych),
                Point(Vector(665,802 ), R.string.name_mushrooms),
                Point(Vector(709,822 ), R.string.name_poznyaks),
                Point(Vector(772,822 ), R.string.name_kharkivska),
                Point(Vector(819,805 ), R.string.name_vyrlitsa),
                Point(Vector(850,774 ), R.string.name_boryspilska),
                Point(Vector(881,743 ), R.string.name_chervonyykhutir),
            ),
            Color.parseColor("#379926")//green
        ),
        // TransElement
    )
    val dnipro: List<Element> = listOf(
        BranchElement(
            listOf(
                Point(Vector(372,207 ), R.string.name_pokrovska),
                Point(Vector(570, 207), R.string.name_prospektSvobody),
                Point(Vector(756,267 ), R.string.name_zavodska),
                Point(Vector(967,270 ), R.string.name_metalurhiv),
                Point(Vector(1162,310 ), R.string.name_metrobudivnykiv),
                Point(Vector(1370, 310), R.string.name_vokzalna),
               /*  Point(Vector(, ), R.string.),*/
            ),
            Color.parseColor("#f22718")//red
        ),
    )
     val kriviyrog: List<Element> = listOf(
        BranchElement(
            listOf(
                Point(Vector(71, 25), R.string.name_zarichna),
                Point(Vector(71, 46), R.string.name_elektrozavodska),
                Point(Vector(71,67 ), R.string.name_industrialna),
                Point(Vector(71, 97), R.string.name_soniachna),
                Point(Vector(71,117 ), R.string.name_miskaLiakrnia),
                Point(Vector(71,138 ), R.string.name_vechirniybulvar),
                Point(Vector(71,162 ), R.string.name_mudriona),
                Point(Vector(71,180 ), R.string.name_budynokrad),
                Point(Vector(71,206 ), R.string.name_prospektmetalurhiv),
                Point(Vector(77,236 ), R.string.name_universytet),
                Point(Vector(81,250 ), R.string.name_vulytsiavitcyzny),
                Point(Vector(94,266 ), R.string.name_tretiadilnytsia),
                Point(Vector(111,266 ), R.string.name_kiltseamkr),
                Point(Vector(123,280 ), R.string.name_maidanPratsi),
                Point(Vector(67, 237), R.string.name_kiltseva),
               /*  Point(Vector(, ), R.string.),*/
            ),
            Color.parseColor("#f22718")
        ),
    )
}