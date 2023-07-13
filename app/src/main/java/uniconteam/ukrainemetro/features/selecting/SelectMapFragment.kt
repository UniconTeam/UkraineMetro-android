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
import unicon.metro.kharkiv.databinding.FragmentSelectmapBinding;
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

        if(viewModel.isHasCity()) {
            navigator.showMap(requireContext())
        }

        viewModel.cities.observe(viewLifecycleOwner) { cities ->
            cities.forEach {
                if(savedInstanceState != null) {
                    val savedCityId = savedInstanceState.getString("selected_city_id")
                    if(savedCityId == it.id)
                        addRadioButton(it, true)
                    else
                        addRadioButton(it)
                } else addRadioButton(it)
            }
        }
        viewModel.fetchCities()

        binding.nextButton.setOnClickListener {
            if(binding.radioGroup.checkedRadioButtonId == -1)
                return@setOnClickListener

            val radioBtn = binding.root.findViewById<MaterialRadioButton>(binding.radioGroup.checkedRadioButtonId)
            val cityId = radioBtn.tag.toString()

            viewModel.setCity(cityId)

            navigator.showMap(requireContext())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // FIXME: broken, outState.putString("selected_city_id", binding.root.findViewById<MaterialRadioButton>(binding.radioGroup.checkedRadioButtonId).tag.toString())
        super.onSaveInstanceState(outState)
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