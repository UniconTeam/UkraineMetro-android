package uniconteam.ukrainemetro.core.navigation

import uniconteam.ukrainemetro.core.platform.BaseFragment

interface Route {
    val fragment: BaseFragment
    val isNeedBackStack: Boolean
}