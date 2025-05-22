package team.unicon.ukrainemetro

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import team.unicon.ukrainemetro.di.commonDiModules

class MetroApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(applicationContext)
            androidLogger()

            modules(commonDiModules)
        }
    }
}