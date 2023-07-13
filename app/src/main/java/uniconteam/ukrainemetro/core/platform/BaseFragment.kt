package uniconteam.ukrainemetro.core.platform

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import uniconteam.ukrainemetro.core.extensions.appContext

/**
 * Base Fragment class with helper methods for handling views and back button events.
 *
 * @see Fragment
 */
@AndroidEntryPoint
abstract class BaseFragment : Fragment() {

    open fun onBackPressed() {}

    internal fun firstTimeCreated(savedInstanceState: Bundle?) = savedInstanceState == null

    internal fun notify(viewContainer: View, @StringRes message: Int) =
        Snackbar.make(viewContainer, message, Snackbar.LENGTH_SHORT).show()

    internal fun notifyWithAction(
        viewContainer: View,
        @StringRes message: Int,
        @StringRes actionText: Int,
        action: () -> Unit
    ) {
        val snackBar = Snackbar.make(viewContainer, message, Snackbar.LENGTH_INDEFINITE)
        snackBar.setAction(actionText) { _ -> action.invoke() }
        //snackBar.setActionTextColor(ContextCompat.getColor(appContext, color.colorTextPrimary))
        snackBar.show()
    }
}
