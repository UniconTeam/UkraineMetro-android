package uniconteam.ukrainemetro.old

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.get
import uniconteam.ukrainemetro.core.repositories.MapsRepository
import uniconteam.ukrainemetro.core.repositories.PrefsRepository

/** Create viewmodel for viewmodelstoreowner
 * @param context Activity context
 * @param clazz Viewmodel class
 * @return Initialized viewmodel
 */
fun <T: ViewModel> ViewModelStoreOwner.createViewModel(context: Context, clazz: Class<T>): T {
    // get constructor and initialize
    val constructor = clazz.getConstructor(RepositoriesContainer::class.java)
    val instance = constructor.newInstance(
        RepositoriesContainer(
        MapsRepository(),
        PrefsRepository(context)
    )
    )

    // make factory
    val factory = object: ViewModelProvider.Factory {
        override fun <T: ViewModel> create(modelClass: Class<T>): T {
            return instance as T
        }
    }

    // make viewmodel and return
    return (ViewModelProvider(this, factory).get() as ViewModel) as T
}