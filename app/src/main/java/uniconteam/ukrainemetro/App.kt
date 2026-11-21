package uniconteam.ukrainemetro

import android.app.Application
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import uniconteam.ukrainemetro.core.repositories.PrefsRepository
import uniconteam.ukrainemetro.core.settings.AppSettings

@HiltAndroidApp
class App: Application() {
    override fun onCreate() {
        super.onCreate()

        // Apply saved language + theme before any UI is created.
        AppSettings.applyAll(PrefsRepository(this))

        // Enable dynamic colors (material 3)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}