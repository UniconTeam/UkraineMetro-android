package uniconteam.ukrainemetro.old.activity

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
import uniconteam.ukrainemetro.old.createViewModel
import uniconteam.ukrainemetro.old.models.MapViewModel
import uniconteam.ukrainemetro.mapview.view.MetroView
import unicon.metro.kharkiv.R
import uniconteam.ukrainemetro.features.selecting.SelectMapActivity
import uniconteam.ukrainemetro.old.Const

class MapActivity : AppCompatActivity() {
    private val metroView: MetroView by lazy { findViewById(R.id.metroView) }
    private val fab: FloatingActionButton by lazy { findViewById(R.id.fab) }
    private val adView: AdView by lazy { findViewById(R.id.adView) }
    private val viewModel: MapViewModel by lazy { createViewModel(this, MapViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_map)

        initAds()

        viewModel.city.observe(this) {
            it?.let { metroView.updateData(it.mapElements) }
        }
        viewModel.fetchCityMap()

        fab.setOnClickListener {
            MaterialDialog(this)
                .title(R.string.app_name)
                .message(R.string.about_text)
                .cornerRadius(16f)
                .listItems(items = listOf(getString(R.string.btn_resetcity), getString(R.string.btn_googleplay), getString(R.string.btn_github)), selection = object: ItemListener {
                    override fun invoke(dialog: MaterialDialog, index: Int, text: CharSequence) {
                        when(index) {
                            0 -> {
                                viewModel.resetCity()
                                metroView.updateData(viewModel.city.value!!.mapElements)

                                val intent = Intent(this@MapActivity, SelectMapActivity::class.java)
                                startActivity(intent)
                                finishAffinity()
                            }
                            1 -> {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(Const.MARKET_URL)
                                startActivity(intent)
                            }
                            2 -> {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(Const.GITHUB_URL)
                                startActivity(intent)
                            }
                        }
                    }
                })
                .show()
        }
    }

    private fun initAds() {
        MobileAds.initialize(this) { }
        val adRequest = AdRequest.Builder()
        adView.loadAd(adRequest.build())
    }

    companion object {
        fun callingIntent(context: Context): Intent? {
            TODO("Not impl")
        }
    }
}