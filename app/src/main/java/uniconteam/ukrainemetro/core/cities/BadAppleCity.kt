package uniconteam.ukrainemetro.core.cities

import uniconteam.ukrainemetro.mapview.entities.City
import unicon.metro.kharkiv.R
import uniconteam.ukrainemetro.mapview.entities.elements.Element

/**
 * Joke city: plays Bad Apple as map lineart through the Vulkan renderer.
 * The actual frames are streamed per-frame by [BadApplePlayer] as
 * TransElement segments, so there is no static map data here.
 */
class BadAppleCity: City {
    override val id: String
        get() = javaClass.name
    override val name: Int
        get() = R.string.city_bad_apple
    override val mapElements: List<Element>
        get() = emptyList()
}
