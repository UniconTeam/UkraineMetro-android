package team.unicon.ukrainemetro

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import team.unicon.ukrainemetro.di.commonDiModules
import team.unicon.ukrainemetro.di.initKoin

class MetroApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(applicationContext)
            androidLogger()
        }
    }
}