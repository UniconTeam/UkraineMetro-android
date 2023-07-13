package uniconteam.ukrainemetro.core.navigation

import android.content.Context
import android.view.View
import uniconteam.ukrainemetro.old.activity.MapActivity
import uniconteam.ukrainemetro.features.selecting.SelectMapActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Navigator
@Inject constructor() {

    fun showSelectMap(context: Context) =
        context.startActivity(SelectMapActivity.callingIntent(context))

    fun showMap(context: Context) =
        context.startActivity(MapActivity.callingIntent(context))

    class Extras(val transitionSharedElement: View)
}
