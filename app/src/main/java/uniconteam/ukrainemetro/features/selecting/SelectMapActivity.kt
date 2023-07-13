package uniconteam.ukrainemetro.features.selecting

import android.content.Context
import android.content.Intent
import uniconteam.ukrainemetro.core.platform.BaseActivity
import uniconteam.ukrainemetro.core.platform.BaseFragment

class SelectMapActivity: BaseActivity() {
    override fun fragment(): BaseFragment =
        SelectMapFragment()

    companion object {
        fun callingIntent(context: Context): Intent =
            Intent(context, SelectMapActivity::class.java)
    }
}