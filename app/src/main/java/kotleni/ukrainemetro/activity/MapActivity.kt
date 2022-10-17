package kotleni.ukrainemetro.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotleni.ukrainemetro.createViewModel
import kotleni.ukrainemetro.models.MapViewModel
import kotleni.ukrainemetro.models.SelectMapViewModel
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

        viewModel.getMapData().observe(this) {
            if(it != null) {
                metroView.updateData(it)
            }
        }
        viewModel.loadMapData()
    }

    private fun initAds() {
        MobileAds.initialize(this) { }
        val adRequest = AdRequest.Builder()
        adView.loadAd(adRequest.build())
    }
}