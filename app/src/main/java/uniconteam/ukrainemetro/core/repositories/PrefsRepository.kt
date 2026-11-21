package uniconteam.ukrainemetro.core.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

class PrefsRepository(context: Context) {
    object Keys {
        const val CITY_ID = "CITY_ID"
        const val THEME_MODE = "THEME_MODE"
        const val LANGUAGE = "LANGUAGE"
    }
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("prefs_2", Context.MODE_PRIVATE)

    var cityId: String?
        get() {
            return sharedPreferences.getString(Keys.CITY_ID, null)
        }
        set(value) {
            sharedPreferences.edit { putString(Keys.CITY_ID, value) }
        }
    val isHasCityId: Boolean = sharedPreferences.contains(Keys.CITY_ID)

    /** 0 = system, 1 = light, 2 = dark. */
    var themeMode: Int
        get() = sharedPreferences.getInt(Keys.THEME_MODE, 0)
        set(value) {
            sharedPreferences.edit { putInt(Keys.THEME_MODE, value) }
        }

    /** "system", "en", "uk" or "ru". */
    var language: String
        get() = sharedPreferences.getString(Keys.LANGUAGE, "system") ?: "system"
        set(value) {
            sharedPreferences.edit { putString(Keys.LANGUAGE, value) }
        }

    /** Two-finger map rotation. */
    var rotationEnabled: Boolean
        get() = sharedPreferences.getBoolean("ROTATION_ENABLED", true)
        set(value) {
            sharedPreferences.edit { putBoolean("ROTATION_ENABLED", value) }
        }

    /** Clamp zoom to the default 1.0–3.2 range. */
    var zoomLimitsEnabled: Boolean
        get() = sharedPreferences.getBoolean("ZOOM_LIMITS_ENABLED", true)
        set(value) {
            sharedPreferences.edit { putBoolean("ZOOM_LIMITS_ENABLED", value) }
        }

    /** Ad banner at the bottom of the map. */
    var adsEnabled: Boolean
        get() = sharedPreferences.getBoolean("ADS_ENABLED", true)
        set(value) {
            sharedPreferences.edit { putBoolean("ADS_ENABLED", value) }
        }

    /** Vulkan performance overlay (FPS, frame graph, draw stats). */
    var debugOverlayEnabled: Boolean
        get() = sharedPreferences.getBoolean("DEBUG_OVERLAY", false)
        set(value) {
            sharedPreferences.edit { putBoolean("DEBUG_OVERLAY", value) }
        }
}