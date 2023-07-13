package uniconteam.ukrainemetro.mapview.extensions

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes

/** Get color by attribute
 * @param id Attribute resource
 * @return Rgb color in int
 */
// @AttrRes
fun Context.getColorByAttr(id: Int): Int {
    val typedValue: TypedValue = TypedValue()
    theme.resolveAttribute(id, typedValue, true)
    return typedValue.data
}