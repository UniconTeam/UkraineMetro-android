package uniconteam.ukrainemetro.features.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.ItemListener
import com.afollestad.materialdialogs.list.listItems
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import unicon.metro.kharkiv.R
import uniconteam.ukrainemetro.core.navigation.Navigator
import uniconteam.ukrainemetro.core.platform.BaseFragment
import javax.inject.Inject
import unicon.metro.kharkiv.databinding.FragmentMapBinding

@AndroidEntryPoint
class MapFragment: BaseFragment() {
    @Inject
    lateinit var navigator: Navigator

    private val viewModel: MapViewModel by viewModels()

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAds()

        viewModel.city.observe(viewLifecycleOwner) {
            it?.let { binding.metroView.updateData(it.mapElements) }
        }
        viewModel.fetchCityMap()

        binding.fab.setOnClickListener {
            MaterialDialog(requireContext())
                .title(R.string.app_name)
                .message(R.string.about_text)
                .cornerRadius(16f)
                .listItems(items = listOf(getString(R.string.btn_resetcity), getString(R.string.btn_googleplay), getString(
                    R.string.btn_github)), selection = object: ItemListener {
                    override fun invoke(dialog: MaterialDialog, index: Int, text: CharSequence) {
                        when(index) {
                            0 -> {
                                viewModel.resetCity()
                                navigator.showSelectMap()
                            }
                            1 -> { navigator.openMarketLink(requireContext()) }
                            2 -> { navigator.openGithubLink(requireContext()) }
                        }
                    }
                })
                .show()
        }
    }

    private fun initAds() {
        MobileAds.initialize(requireContext()) { }
        val adRequest = AdRequest.Builder()
        binding.adView.loadAd(adRequest.build())
    }
}