package uniconteam.ukrainemetro.features.selecting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import com.google.android.material.radiobutton.MaterialRadioButton
import uniconteam.ukrainemetro.old.createViewModel
import uniconteam.ukrainemetro.old.models.SelectMapViewModel
import uniconteam.ukrainemetro.mapview.entities.City
import unicon.metro.kharkiv.R
import uniconteam.ukrainemetro.core.platform.BaseActivity
import uniconteam.ukrainemetro.core.platform.BaseFragment
import uniconteam.ukrainemetro.old.activity.MapActivity

class SelectMapActivity: BaseActivity() {
    override fun fragment(): BaseFragment =
        SelectMapFragment()

    companion object {
        fun callingIntent(context: Context): Intent =
            Intent(context, SelectMapActivity::class.java)
    }
}