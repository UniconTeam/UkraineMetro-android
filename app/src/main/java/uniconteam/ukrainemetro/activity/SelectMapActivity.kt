package uniconteam.ukrainemetro.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import androidx.core.view.children
import com.google.android.material.radiobutton.MaterialRadioButton
import uniconteam.ukrainemetro.createViewModel
import uniconteam.ukrainemetro.models.SelectMapViewModel
import uniconteam.ukrainemetro.mapview.entities.City
import unicon.metro.kharkiv.R

class SelectMapActivity : AppCompatActivity() {
    private val radioGroup: RadioGroup by lazy { findViewById(R.id.radio_group) }
    private val nextButton: Button by lazy { findViewById(R.id.next_button) }
    private val viewModel: SelectMapViewModel by lazy { createViewModel(this, SelectMapViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selectmap)

        if(viewModel.isHasCity()) {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
        }

        viewModel.cities.observe(this) { cities ->
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

        nextButton.setOnClickListener {
            if(radioGroup.checkedRadioButtonId == -1)
                return@setOnClickListener

            val radioBtn = findViewById<MaterialRadioButton>(radioGroup.checkedRadioButtonId)
            val cityId = radioBtn.tag.toString()

            viewModel.setCity(cityId)

            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun addRadioButton(city: City, isChecked: Boolean = false) {
        val radioBtn = MaterialRadioButton(this)
        radioBtn.id = View.generateViewId()
        radioBtn.text = resources.getString(city.name)
        radioBtn.tag = city.id
        radioBtn.isChecked = isChecked
        radioGroup.addView(radioBtn)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selected_city_index", radioGroup.indexOfChild(findViewById<MaterialRadioButton>(radioGroup.checkedRadioButtonId)))
        super.onSaveInstanceState(outState)
    }
}