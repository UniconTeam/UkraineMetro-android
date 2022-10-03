package kotleni.ukrainemetro.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import com.google.android.material.radiobutton.MaterialRadioButton
import kotleni.ukrainemetro.createViewModel
import kotleni.ukrainemetro.models.SelectMapViewModel
import kotleni.ukrainemetro.types.City
import unicon.metro.kharkiv.R

class SelectMapActivity : AppCompatActivity() {
    private val radioGroup: RadioGroup by lazy { findViewById(R.id.radio_group) }
    private val nextButton: Button by lazy { findViewById(R.id.next_button) }
    private val viewModel: SelectMapViewModel by lazy { createViewModel(this, SelectMapViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selectmap)

        nextButton.setOnClickListener {
            val radioBtn = findViewById<MaterialRadioButton>(radioGroup.checkedRadioButtonId)
            City.values().forEach {
                if(it.id == radioGroup.indexOfChild(radioBtn)) {
                    viewModel.setCity(it)
                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
}