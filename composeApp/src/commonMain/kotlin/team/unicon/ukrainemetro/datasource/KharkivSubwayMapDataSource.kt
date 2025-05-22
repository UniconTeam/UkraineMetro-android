package team.unicon.ukrainemetro.datasource

import team.unicon.ukrainemetro.entities.Point
import team.unicon.ukrainemetro.entities.Vector
import team.unicon.ukrainemetro.entities.elements.*
import team.unicon.ukrainemetro.localization.Strings
import team.unicon.ukrainemetro.localization.getStrings

class KharkivSubwayMapDataSource(
    val strings: Strings = getStrings()
) : SubwayMapDataSource {
    override fun getMapElements(): List<Element> {
        return listOf(
            BranchElement(
                listOf(
                    Point(
                        Vector(173, 34),
                        strings.namePeremoha
                    ),
                    Point(
                        Vector(190, 51),
                        strings.nameOleksiivska
                    ),
                    Point(
                        Vector(209, 81),
                        strings.name23Serpnia
                    ),
                    Point(
                        Vector(209, 104),
                        strings.nameBotanichnyiSad
                    ),
                    Point(
                        Vector(209, 127),
                        strings.nameNaukova
                    ),
                    Point(
                        Vector(209, 151),
                        strings.nameDerzhprom
                    ),
                    Point(
                        Vector(255, 208),
                        strings.nameArkhitektoraBeketova
                    ),
                    Point(
                        Vector(278, 231),
                        strings.nameZakhysnykivUkrainy
                    ),
                    Point(
                        Vector(302, 267),
                        strings.nameMetrobudivnykivKharkiv
                    )
                ),
                0 // Color.parseColor("#379926")
            ),
            BranchElement(
                listOf(
                    Point(
                        Vector(22, 279),
                        strings.nameKholodnaHora
                    ),
                    Point(
                        Vector(46, 256),
                        strings.namePivdennyiVokzal
                    ),
                    Point(
                        Vector(69, 233),
                        strings.nameTsentralnyiRynok
                    ),
                    Point(
                        Vector(151, 208),
                        strings.nameMaidanKonstytutsii
                    ),
                    Point(
                        Vector(197, 243),
                        strings.nameProspektHaharina
                    ),
                    Point(
                        Vector(290, 278),
                        strings.nameSportyvna
                    ),
                    Point(
                        Vector(337, 290),
                        strings.nameZavodskaKharkiv
                    ),
                    Point(
                        Vector(360, 313),
                        strings.nameTurboatom
                    ),
                    Point(
                        Vector(383, 337),
                        strings.namePalatsSportuKharkiv
                    ),
                    Point(
                        Vector(407, 359),
                        strings.nameArmiiska
                    ),
                    Point(
                        Vector(419, 383),
                        strings.nameImOSMaselskoho
                    ),
                    Point(
                        Vector(419, 407),
                        strings.nameTraktornyiZavod
                    ),
                    Point(
                        Vector(419, 430),
                        strings.nameIndustrialnaKharkiv
                    )
                ),
                0 //Color.parseColor("#f22718")
            ),
            BranchElement(
                listOf(
                    Point(
                        Vector(407, 58),
                        strings.nameSaltivska
                    ),
                    Point(
                        Vector(384, 81),
                        strings.nameStudentska
                    ),
                    Point(
                        Vector(359, 105),
                        strings.nameAkademikaPavlova
                    ),
                    Point(
                        Vector(336, 127),
                        strings.nameAkademikaBarabashova
                    ),
                    Point(
                        Vector(313, 151),
                        strings.nameKyivska
                    ),
                    Point(
                        Vector(280, 163),
                        strings.namePushkinska
                    ),
                    Point(
                        Vector(196, 163),
                        strings.nameUniversytetKharkiv
                    ),
                    Point(
                        Vector(167, 163),
                        null
                    ),
                    Point(
                        Vector(163, 167),
                        null
                    ),
                    Point(
                        Vector(162, 197),
                        strings.nameIstorychnyiMuzei
                    )
                ),
                0 // Color.parseColor("#1261ff")
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
}