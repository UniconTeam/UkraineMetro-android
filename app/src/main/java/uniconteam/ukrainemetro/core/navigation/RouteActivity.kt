package uniconteam.ukrainemetro.core.navigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import unicon.metro.kharkiv.databinding.ActivityLayoutBinding
import uniconteam.ukrainemetro.core.extensions.inTransaction
import uniconteam.ukrainemetro.core.platform.BaseFragment

@AndroidEntryPoint
class RouteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLayoutBinding

    @Inject
    internal lateinit var navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLayoutBinding.inflate(layoutInflater)

        setContentView(binding.root)
        // addFragment(savedInstanceState)

        navigator.setRouteActivity(this)
        navigator.showSelectMap()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        (supportFragmentManager.findFragmentById(binding.fragmentContainer.id) as BaseFragment).onBackPressed()
        super.onBackPressed()
    }

    fun fragmentContainer() = binding.fragmentContainer

    // TODO: impl instance states for fragments
//    private fun addFragment(savedInstanceState: Bundle?) =
//        savedInstanceState ?: supportFragmentManager.inTransaction {
//            add(binding.fragmentContainer.id, fragment())
//        }

    fun routeTo(route: Route) {
        supportFragmentManager.inTransaction {
            if(route.isNeedBackStack)
                add(binding.fragmentContainer.id, route.fragment)
            else
                replace(binding.fragmentContainer.id, route.fragment)
        }
    }
}
