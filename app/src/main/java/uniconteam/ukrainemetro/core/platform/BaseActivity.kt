package uniconteam.ukrainemetro.core.platform

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import uniconteam.ukrainemetro.core.extensions.inTransaction
import unicon.metro.kharkiv.databinding.ActivityLayoutBinding

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLayoutBinding.inflate(layoutInflater)

        setContentView(binding.root)
        addFragment(savedInstanceState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        (supportFragmentManager.findFragmentById(binding.fragmentContainer.id) as BaseFragment).onBackPressed()
        super.onBackPressed()
    }

    fun fragmentContainer() = binding.fragmentContainer

    private fun addFragment(savedInstanceState: Bundle?) =
        savedInstanceState ?: supportFragmentManager.inTransaction {
            add(binding.fragmentContainer.id, fragment())
        }

    abstract fun fragment(): BaseFragment
}