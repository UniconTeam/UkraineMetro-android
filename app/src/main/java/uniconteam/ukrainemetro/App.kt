package uniconteam.ukrainemetro

import android.app.Application
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable dynamic colors (material 3)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}