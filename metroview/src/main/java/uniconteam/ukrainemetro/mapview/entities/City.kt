package uniconteam.ukrainemetro.mapview.entities

import androidx.annotation.StringRes
import uniconteam.ukrainemetro.mapview.entities.elements.Element

interface City {
    val id: String
    val name: Int
    val mapElements: List<Element>
}