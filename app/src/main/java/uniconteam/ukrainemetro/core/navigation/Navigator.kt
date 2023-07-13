package uniconteam.ukrainemetro.core.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import uniconteam.ukrainemetro.Const
import uniconteam.ukrainemetro.features.map.MapRoute
import uniconteam.ukrainemetro.features.selecting.SelectMapRoute
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Navigator
@Inject constructor() {
    private var routeActivity: RouteActivity? = null

    fun setRouteActivity(routeActivity: RouteActivity) {
        this.routeActivity = routeActivity
    }

    fun showSelectMap() = routeActivity?.routeTo(SelectMapRoute())

    fun showMap() = routeActivity?.routeTo(MapRoute())

    fun openGithubLink(context: Context) =
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(Const.GITHUB_URL)
        })

    fun openMarketLink(context: Context) =
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(Const.MARKET_URL)
        })
}
