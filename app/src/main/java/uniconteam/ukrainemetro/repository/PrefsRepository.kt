package uniconteam.ukrainemetro.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import uniconteam.ukrainemetro.mapview.entities.City

class PrefsRepository(private val context: Context) {
    object Keys {
        const val CITY_ID = "CITY_ID"
    }
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    var cityId: String?
        get() {
            return sharedPreferences.getString(Keys.CITY_ID, null)
        }
        set(value) {
            sharedPreferences.edit { putString(Keys.CITY_ID, value) }
        }
    val isHasCityId: Boolean = sharedPreferences.contains(Keys.CITY_ID)
}