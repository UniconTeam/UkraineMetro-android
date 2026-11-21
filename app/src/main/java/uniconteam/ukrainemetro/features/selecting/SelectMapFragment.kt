package uniconteam.ukrainemetro.features.selecting

import android.animation.ValueAnimator
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import dagger.hilt.android.AndroidEntryPoint
import uniconteam.ukrainemetro.core.navigation.Navigator
import uniconteam.ukrainemetro.core.platform.BaseFragment
import javax.inject.Inject
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import unicon.metro.kharkiv.R
import unicon.metro.kharkiv.databinding.FragmentSelectmapBinding;
import unicon.metro.kharkiv.databinding.ItemCityBinding
import uniconteam.ukrainemetro.core.exceptions.Failure
import uniconteam.ukrainemetro.core.extensions.failure
import uniconteam.ukrainemetro.core.extensions.observe
import uniconteam.ukrainemetro.features.map.MapFailure
import uniconteam.ukrainemetro.mapview.entities.City

@AndroidEntryPoint
class SelectMapFragment: BaseFragment() {
    @Inject
    lateinit var navigator: Navigator

    private val viewModel: SelectMapViewModel by viewModels()

    private var _binding: FragmentSelectmapBinding? = null
    private val binding get() = _binding!!

    private var cities: List<City> = emptyList()
    private val cards = mutableListOf<ItemCityBinding>()
    private var selectedIndex = 0
    private val saturation = mutableMapOf<ImageView, Float>()
    private val satAnimators = mutableMapOf<ImageView, ValueAnimator>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSelectmapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedIndex = savedInstanceState?.getInt(KEY_SELECTION, 0) ?: 0

        with(viewModel) {
            observe(cities, ::handleCities)
            observe(isCitySelected, ::handleSelection)
            failure(failure, ::handleFailure)
        }

        viewModel.checkSelection()
        viewModel.fetchCities()

        ViewCompat.setOnApplyWindowInsetsListener(binding.buttonBar) { v, insets ->
            val base = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(
                bottom = base + insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
                ).bottom
            )
            insets
        }

        binding.selectButton.setOnClickListener {
            val city = cities.getOrNull(selectedIndex) ?: return@setOnClickListener
            viewModel.setCity(city.id)
            navigator.showMap()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTION, selectedIndex)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleCities(cities: List<City>?) {
        this.cities = cities ?: emptyList()
        cards.clear()
        binding.citiesContainer.removeAllViews()
        this.cities.forEachIndexed { index, city ->
            addCityCard(city, index)
        }
        if (this.cities.isNotEmpty()) {
            select(selectedIndex.coerceIn(this.cities.indices), animate = false)
        }
    }

    private fun handleSelection(isSelected: Boolean?) {
        if(isSelected == true)
            navigator.showMap()
    }

    private fun handleFailure(failure: Failure?) {
        when (failure) {
            else -> {
                notify(binding.root, R.string.failure_unknown)
            }
        }
    }

    private fun imageFor(city: City): Int = when (city.name) {
        R.string.city_kharkiv -> R.drawable.city_kharkiv
        R.string.city_kyiv -> R.drawable.city_kyiv
        R.string.city_dnipro -> R.drawable.city_dnipro
        R.string.city_kriviyrig -> R.drawable.city_kriviy_rih
        else -> 0 // no photo: text-only card
    }

    private fun addCityCard(city: City, index: Int) {
        val card = ItemCityBinding.inflate(layoutInflater, binding.citiesContainer, false)
        card.cityName.text = resources.getString(city.name)
        val image = imageFor(city)
        if (image != 0) {
            card.cityImage.setImageResource(image)
            card.cityImage.visibility = View.VISIBLE
        } else {
            card.cityImage.visibility = View.GONE
        }
        card.root.setOnClickListener {
            select(index, animate = true)
        }
        cards.add(card)
        binding.citiesContainer.addView(card.root)
    }

    private fun select(index: Int, animate: Boolean) {
        if (index !in cards.indices) return
        selectedIndex = index
        cards.forEachIndexed { i, card ->
            val active = i == index
            card.cityRadio.isChecked = active
            card.root.isSelected = active
            if (card.cityImage.visibility != View.GONE) {
                setImageHeight(card, if (active) IMG_EXPANDED_DP else IMG_COLLAPSED_DP, animate)
                setImageSaturation(card.cityImage, if (active) 1f else 0f, animate)
            }
        }
    }

    private fun setImageSaturation(view: ImageView, target: Float, animate: Boolean) {
        satAnimators.remove(view)?.cancel()
        if (!animate) {
            saturation[view] = target
            view.colorFilter = filterFor(target)
            return
        }
        val start = saturation[view] ?: target
        if (start == target) {
            view.colorFilter = filterFor(target)
            return
        }
        val animator = ValueAnimator.ofFloat(start, target).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val v = it.animatedValue as Float
                saturation[view] = v
                view.colorFilter = filterFor(v)
            }
        }
        satAnimators[view] = animator
        animator.start()
    }

    private fun filterFor(sat: Float) =
        if (sat >= 1f) null
        else ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(sat) })

    private fun setImageHeight(card: ItemCityBinding, heightDp: Int, animate: Boolean) {
        val target = (heightDp * resources.displayMetrics.density).toInt()
        val params = card.cityImage.layoutParams
        if (!animate) {
            params.height = target
            card.cityImage.layoutParams = params
            return
        }
        if (params.height == target) return
        ValueAnimator.ofInt(params.height, target).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                params.height = it.animatedValue as Int
                card.cityImage.layoutParams = params
            }
            start()
        }
    }

    companion object {
        private const val KEY_SELECTION = "selected_city"
        private const val IMG_COLLAPSED_DP = 80
        private const val IMG_EXPANDED_DP = 144
    }
}
