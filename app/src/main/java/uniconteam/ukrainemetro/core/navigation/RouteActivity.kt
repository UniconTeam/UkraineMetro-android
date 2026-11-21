package uniconteam.ukrainemetro.core.navigation

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import unicon.metro.kharkiv.databinding.ActivityLayoutBinding
import uniconteam.ukrainemetro.core.extensions.inTransaction
import uniconteam.ukrainemetro.core.platform.BaseFragment
import uniconteam.ukrainemetro.core.repositories.PrefsRepository

@AndroidEntryPoint
class RouteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLayoutBinding

    @Inject
    internal lateinit var navigator: Navigator

    @Inject
    internal lateinit var prefs: PrefsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparent status + nav bars; the map draws underneath them.
        enableEdgeToEdge()

        binding = ActivityLayoutBinding.inflate(layoutInflater)

        setContentView(binding.root)
        // addFragment(savedInstanceState)

        navigator.setRouteActivity(this)
        // Go straight to the map when a city is already picked: bouncing
        // through the select screen on every recreation (rotation, theme,
        // locale change) tears down the Vulkan view for nothing.
        if (prefs.isHasCityId) navigator.showMap()
        else navigator.showSelectMap()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // With mixed add()/replace() transactions findFragmentById can
        // return a covered fragment, so resolve the topmost one directly.
        // A fragment returning true consumed the press (e.g. closing an
        // inner tab); otherwise the system handles it.
        val fragments = supportFragmentManager.fragments
        var top: BaseFragment? = null
        for (i in fragments.size - 1 downTo 0) {
            val f = fragments[i]
            if (f is BaseFragment && f.isAdded && !f.isDetached && !f.isRemoving) {
                top = f
                break
            }
        }
        if (top?.onBackPressed() == true) return
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
                add(binding.fragmentContainer.id, route.fragment).addToBackStack(route.javaClass.name)
            else
                replace(binding.fragmentContainer.id, route.fragment)
        }
    }
}
