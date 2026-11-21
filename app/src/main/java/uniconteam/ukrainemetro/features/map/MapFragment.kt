package uniconteam.ukrainemetro.features.map

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.viewModels
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.radiobutton.MaterialRadioButton
import dagger.hilt.android.AndroidEntryPoint
import unicon.metro.kharkiv.BuildConfig
import unicon.metro.kharkiv.R
import uniconteam.ukrainemetro.core.navigation.Navigator
import uniconteam.ukrainemetro.core.platform.BaseFragment
import uniconteam.ukrainemetro.core.repositories.PrefsRepository
import uniconteam.ukrainemetro.core.settings.AppSettings
import javax.inject.Inject
import unicon.metro.kharkiv.databinding.FragmentMapBinding
import unicon.metro.kharkiv.databinding.LayoutChoiceSheetBinding
import uniconteam.ukrainemetro.core.exceptions.Failure
import uniconteam.ukrainemetro.core.extensions.failure
import uniconteam.ukrainemetro.core.extensions.observe
import uniconteam.ukrainemetro.mapview.entities.City
import uniconteam.ukrainemetro.mapview.entities.elements.BranchElement

@AndroidEntryPoint
class MapFragment: BaseFragment() {
    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var prefs: PrefsRepository

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

        binding.metroView.setMapInteraction(prefs.rotationEnabled, prefs.zoomLimitsEnabled)
        applyDebugOverlay()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            overlayTopMargin = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            applyDebugOverlay()
            insets
        }

        with(viewModel) {
            observe(city, ::handleCity)
            failure(failure, ::handleFailure)
        }

        viewModel.fetchCityMap()

        initBottomNav(savedInstanceState?.getInt(KEY_TAB, R.id.tabExplore) ?: R.id.tabExplore)
        initSettings()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (_binding != null) {
            outState.putInt(KEY_TAB, binding.bottomNav.selectedItemId)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-apply in case the user changed map settings and came back.
        if (_binding != null) {
            applyMapSettings()
            refreshSummaries()
            syncSwitches()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onBackPressed(): Boolean {
        // Settings tab consumes back by returning to Explore.
        if (_binding != null && binding.bottomNav.selectedItemId != R.id.tabExplore) {
            binding.bottomNav.selectedItemId = R.id.tabExplore
            return true
        }
        return false
    }

    // ------------------------------------------------------------------ tabs

    private fun initBottomNav(startTab: Int) {
        binding.bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }
        binding.bottomNav.selectedItemId = startTab
        showTab(startTab)
    }

    private fun showTab(tabId: Int) {
        val settings = tabId == R.id.tabSettings
        binding.settingsScroll.visibility = if (settings) View.VISIBLE else View.GONE
        binding.metroView.visibility = if (settings) View.INVISIBLE else View.VISIBLE
        // Opaque system nav bar on the map so it never draws underneath
        // it; the settings list keeps the transparent edge-to-edge bar.
        updateNavBar(transparent = settings)
        // Tab switches don't trigger onResume: apply here so toggles
        // take effect the moment the user returns to Explore.
        if (!settings) applyMapSettings()
    }

    private fun applyMapSettings() {
        if (_binding == null) return
        binding.metroView.setMapInteraction(prefs.rotationEnabled, prefs.zoomLimitsEnabled)
        binding.metroView.setBigLabels(prefs.biggerLabels)
        applyAdsVisibility()
        applyDebugOverlay()
        pushMapData(force = false)
    }

    private fun updateNavBar(transparent: Boolean) {
        val win = activity?.window ?: return
        if (transparent) {
            win.navigationBarColor = Color.TRANSPARENT
        } else {
            // Same token as the BottomNavigationView background so the
            // system gesture area reads as one continuous bottom bar.
            val tv = TypedValue()
            requireContext().theme.resolveAttribute(
                com.google.android.material.R.attr.colorSurfaceContainer, tv, true
            )
            win.navigationBarColor = tv.data
        }
        val night =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(win, win.decorView)
            .isAppearanceLightNavigationBars = !night
    }

    // -------------------------------------------------------------- settings

    private fun initSettings() {
        refreshSummaries()
        syncSwitches()

        binding.languageRow.setOnClickListener {
            showChoiceSheet(
                titleRes = R.string.settings_language,
                entries = listOf(
                    AppSettings.LANG_SYSTEM to getString(R.string.option_system),
                    "en" to "English",
                    "uk" to "Українська",
                    "ru" to "Русский"
                ),
                current = prefs.language
            ) { tag ->
                prefs.language = tag
                AppSettings.applyLanguage(tag)
                refreshSummaries()
            }
        }
        binding.themeRow.setOnClickListener {
            showChoiceSheet(
                titleRes = R.string.settings_theme,
                entries = listOf(
                    AppSettings.THEME_SYSTEM to getString(R.string.option_system),
                    AppSettings.THEME_LIGHT to getString(R.string.option_light),
                    AppSettings.THEME_DARK to getString(R.string.option_dark)
                ),
                current = prefs.themeMode
            ) { mode ->
                prefs.themeMode = mode
                AppSettings.applyTheme(mode)
                refreshSummaries()
            }
        }

        binding.rotationSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.rotationEnabled = checked
        }
        binding.zoomLimitsSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.zoomLimitsEnabled = checked
        }
        binding.adsSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.adsEnabled = checked
        }
        binding.originalColorsSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.originalColors = checked
        }
        binding.biggerLabelsSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.biggerLabels = checked
        }
        binding.debugOverlaySwitch.setOnCheckedChangeListener { _, checked ->
            prefs.debugOverlayEnabled = checked
        }

        binding.changeCityRow.setOnClickListener {
            viewModel.resetCity()
            navigator.showSelectMap()
        }
        binding.googlePlayRow.setOnClickListener {
            navigator.openMarketLink(requireContext())
        }
        binding.githubRow.setOnClickListener {
            navigator.openGithubLink(requireContext())
        }

        binding.versionText.text =
            "${getString(R.string.app_name)} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    private fun refreshSummaries() {
        if (_binding == null) return
        binding.languageValue.text = when (prefs.language) {
            "en" -> "English"
            "uk" -> "Українська"
            "ru" -> "Русский"
            else -> getString(R.string.option_system)
        }
        binding.themeValue.text = when (prefs.themeMode) {
            AppSettings.THEME_LIGHT -> getString(R.string.option_light)
            AppSettings.THEME_DARK -> getString(R.string.option_dark)
            else -> getString(R.string.option_system)
        }
    }

    private fun syncSwitches() {
        if (_binding == null) return
        binding.rotationSwitch.isChecked = prefs.rotationEnabled
        binding.zoomLimitsSwitch.isChecked = prefs.zoomLimitsEnabled
        binding.adsSwitch.isChecked = prefs.adsEnabled
        binding.originalColorsSwitch.isChecked = prefs.originalColors
        binding.biggerLabelsSwitch.isChecked = prefs.biggerLabels
        binding.debugOverlaySwitch.isChecked = prefs.debugOverlayEnabled
    }

    private fun <T> showChoiceSheet(
        @StringRes titleRes: Int,
        entries: List<Pair<T, String>>,
        current: T,
        onPick: (T) -> Unit
    ) {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = LayoutChoiceSheetBinding.inflate(layoutInflater)
        sheet.choiceTitle.setText(titleRes)
        entries.forEach { (value, label) ->
            val radio = MaterialRadioButton(requireContext()).apply {
                text = label
                minHeight = resources.getDimensionPixelSize(R.dimen.choice_row_min_height)
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.choice_row_padding),
                    0,
                    resources.getDimensionPixelSize(R.dimen.choice_row_padding),
                    0
                )
            }
            radio.id = View.generateViewId()
            radio.tag = value
            radio.isChecked = value == current
            sheet.choiceGroup.addView(radio)
        }
        sheet.choiceGroup.setOnCheckedChangeListener { group, checkedId ->
            val radio = group.findViewById<MaterialRadioButton>(checkedId) ?: return@setOnCheckedChangeListener
            @Suppress("UNCHECKED_CAST")
            onPick(radio.tag as T)
            dialog.dismiss()
        }
        dialog.setContentView(sheet.root)
        dialog.show()
    }

    // ------------------------------------------------------------------- map

    private fun handleCity(city: City?) {
        if (city == null) return
        currentCity = city
        pushMapData(force = true)
    }

    private fun pushMapData(force: Boolean) {
        val city = currentCity ?: return
        val original = prefs.originalColors
        if (!force && lastColorMode == original) return
        lastColorMode = original
        binding.metroView.setTransferColor(
            if (original) 0xFFFF00FF.toInt() else 0xFFCE93D8.toInt()
        )
        val elements = if (original) {
            city.mapElements.map { el ->
                if (el is BranchElement) BranchElement(el.points, mapToOriginal(el.color))
                else el
            }
        } else {
            city.mapElements
        }
        binding.metroView.updateData(elements)
    }

    private fun mapToOriginal(color: Int): Int = when (color) {
        0xFFE57373.toInt() -> 0xFFF22718.toInt()
        0xFF2196F3.toInt() -> 0xFF1261FF.toInt()
        0xFF81C784.toInt() -> 0xFF379926.toInt()
        else -> color
    }

    private fun handleFailure(failure: Failure?) {
        when (failure) {
            is MapFailure.CityNotSelected -> {
                notifyWithAction(binding.root, R.string.failure_map_notselected, R.string.action_select) {
                    navigator.showSelectMap()
                }
            }
            else -> {
                notify(binding.root, R.string.failure_unknown)
            }
        }
    }

    private fun initAds() {
        applyAdsVisibility()
        if (!prefs.adsEnabled) return
        MobileAds.initialize(requireContext()) { }
        val adRequest = AdRequest.Builder()
        binding.adView.loadAd(adRequest.build())
    }

    private fun applyAdsVisibility() {
        binding.adView.visibility = if (prefs.adsEnabled) View.VISIBLE else View.GONE
    }

    private var currentCity: City? = null
    private var lastColorMode: Boolean? = null
    private var overlayTopMargin = 0

    private fun applyDebugOverlay() {
        binding.metroView.setDebugOverlay(prefs.debugOverlayEnabled, overlayTopMargin)
    }

    companion object {
        private const val KEY_TAB = "bottom_tab"
    }
}
