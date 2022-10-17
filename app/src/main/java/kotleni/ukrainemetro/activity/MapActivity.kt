package kotleni.ukrainemetro.activity

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
import kotleni.ukrainemetro.createViewModel
import kotleni.ukrainemetro.models.MapViewModel
import kotleni.ukrainemetro.view.MetroView
import unicon.metro.kharkiv.R

class MapActivity : AppCompatActivity() {
    private val metroView: MetroView by lazy { findViewById(R.id.metroView) }
    private val fab: FloatingActionButton by lazy { findViewById(R.id.fab) }
    private val adView: AdView by lazy { findViewById(R.id.adView) }
    private val viewModel: MapViewModel by lazy { createViewModel(this, MapViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        initAds()

        viewModel.getMapData().observe(this) {
            it?.let { metroView.updateData(it) }
        }
        viewModel.loadMapData()

        fab.setOnClickListener {
            MaterialDialog(this).show {
                title(R.string.app_name)
                message(R.string.about_text)
                cornerRadius(16f)
                listItems(items = listOf(getString(R.string.btn_resetcity), getString(R.string.btn_googleplay), getString(R.string.btn_github)), selection = object: ItemListener {
                    override fun invoke(dialog: MaterialDialog, index: Int, text: CharSequence) {
                        if(index == 0) {
                            viewModel.resetCity()
                            val intent = Intent(this@MapActivity, SelectMapActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else if(index == 1) {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=unicon.metro.kharkiv&hl=en&gl=US"))
                            startActivity(browserIntent)
                        } else if(index == 2) {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kotleni/UkraineMetro-android"))
                            startActivity(browserIntent)
                        }
                    }

                })
            }
        }
    }

    private fun initAds() {
        MobileAds.initialize(this) { }
        val adRequest = AdRequest.Builder()
        adView.loadAd(adRequest.build())
    }
}