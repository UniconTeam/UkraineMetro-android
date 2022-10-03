package kotleni.ukrainemetro.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotleni.ukrainemetro.types.City

class PrefsRepository(private val context: Context) {
    object Keys {
        val cityId = "cityId"
    }
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    fun getCity(): City? {
        val cityId = sharedPreferences.getInt(Keys.cityId, -1)
        if(cityId == -1) return null

        City.values().forEach {
            if(it.id == cityId) return it
        }

        return null
    }

    fun setCity(city: City) {
        sharedPreferences.edit {
            putInt(Keys.cityId, city.id)
        }
    }
}