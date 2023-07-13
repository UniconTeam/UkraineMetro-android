package uniconteam.ukrainemetro.features.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.ItemListener
import com.afollestad.materialdialogs.list.listItems
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.floatingactionbutton.FloatingActionButton
import uniconteam.ukrainemetro.mapview.view.MetroView
import unicon.metro.kharkiv.R
import uniconteam.ukrainemetro.features.selecting.SelectMapActivity
import uniconteam.ukrainemetro.Const
import uniconteam.ukrainemetro.core.platform.BaseActivity
import uniconteam.ukrainemetro.core.platform.BaseFragment

class MapActivity : BaseActivity() {
    override fun fragment(): BaseFragment =
        MapFragment()

    companion object {
        fun callingIntent(context: Context): Intent =
            Intent(context, MapActivity::class.java)
    }
}