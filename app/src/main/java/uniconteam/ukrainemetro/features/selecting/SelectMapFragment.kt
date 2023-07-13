package uniconteam.ukrainemetro.features.selecting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import uniconteam.ukrainemetro.core.navigation.Navigator
import uniconteam.ukrainemetro.core.platform.BaseFragment
import javax.inject.Inject
import androidx.fragment.app.viewModels
import com.google.android.material.radiobutton.MaterialRadioButton
import unicon.metro.kharkiv.R
import unicon.metro.kharkiv.databinding.FragmentSelectmapBinding;
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSelectmapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewModel) {
            observe(cities, ::handleCities)
            observe(isCitySelected, ::handleSelection)
            failure(failure, ::handleFailure)
        }

        viewModel.checkSelection()
        viewModel.fetchCities()

        binding.nextButton.setOnClickListener {
            if(binding.radioGroup.checkedRadioButtonId == -1)
                return@setOnClickListener

            val radioBtn = binding.root.findViewById<MaterialRadioButton>(binding.radioGroup.checkedRadioButtonId)
            val cityId = radioBtn.tag.toString()
            viewModel.setCity(cityId)

            navigator.showMap()
        }
    }

    private fun handleCities(cities: List<City>?) {
        cities?.forEach {
            addRadioButton(it)
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

    private fun addRadioButton(city: City, isChecked: Boolean = false) {
        val radioBtn = MaterialRadioButton(requireContext())
        radioBtn.id = View.generateViewId()
        radioBtn.text = resources.getString(city.name)
        radioBtn.tag = city.id
        radioBtn.isChecked = isChecked
        binding.radioGroup.addView(radioBtn)
    }
}