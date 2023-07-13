package uniconteam.ukrainemetro.features.map

import android.content.Context
import android.content.Intent
import uniconteam.ukrainemetro.core.navigation.Route
import uniconteam.ukrainemetro.core.platform.BaseFragment

class MapRoute: Route {
    override val fragment: BaseFragment get() = MapFragment()
    override val isNeedBackStack: Boolean = false
}