package kotleni.ukrainemetro

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.get
import kotleni.ukrainemetro.repository.MapsRepository
import kotleni.ukrainemetro.repository.PrefsRepository

/** Get background rect of pos and text+textPaint
 * @param x X position
 * @param y Y position
 * @param text String of text
 * @param paint Text paint from canvas
 * @return Rect of text background
 */
fun getTextBackgroundSize(
    x: Float,
    y: Float,
    text: String,
    paint: TextPaint
): RectF {
    val fontMetrics: Paint.FontMetrics = paint.fontMetrics
    val halfTextLength = paint.measureText(text) / 2 + 5
    return RectF(
        (x - halfTextLength),
        (y + fontMetrics.top),
        (x + halfTextLength),
        (y + fontMetrics.bottom)
    )
}

/** Get color by attribute
 * @param id Attribute resource
 * @return Rgb color in int
 */
fun Context.getColorByAttr(@AttrRes id: Int): Int {
    val typedValue: TypedValue = TypedValue()
    theme.resolveAttribute(id, typedValue, true)
    return typedValue.data
}

/** Create viewmodel for viewmodelstoreowner
 * @param context Activity context
 * @param clazz Viewmodel class
 * @return Initialized viewmodel
 */
fun <T: ViewModel> ViewModelStoreOwner.createViewModel(context: Context, clazz: Class<T>): T {
    // get constructor and initialize
    val constructor = clazz.getConstructor(RepositoriesContainer::class.java)
    val instance = constructor.newInstance(RepositoriesContainer(
        MapsRepository(),
        PrefsRepository(context)
    ))

    // make factory
    val factory = object: ViewModelProvider.Factory {
        override fun <T: ViewModel> create(modelClass: Class<T>): T {
            return instance as T
        }
    }

    // make viewmodel and return
    return (ViewModelProvider(this, factory).get() as ViewModel) as T
}