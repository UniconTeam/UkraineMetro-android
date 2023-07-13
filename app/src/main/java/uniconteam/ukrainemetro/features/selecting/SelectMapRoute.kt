package uniconteam.ukrainemetro.features.selecting

import uniconteam.ukrainemetro.core.navigation.Route
import uniconteam.ukrainemetro.core.platform.BaseFragment
import uniconteam.ukrainemetro.features.map.MapFragment

class SelectMapRoute: Route {
    override val fragment: BaseFragment get() = SelectMapFragment()
    override val isNeedBackStack: Boolean = false
}