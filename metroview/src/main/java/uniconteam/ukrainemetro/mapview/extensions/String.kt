package uniconteam.ukrainemetro.mapview.extensions

import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint

/** Get string background rect of pos and textPaint for Canvas
 * @param x X position
 * @param y Y position
 * @param text String of text
 * @param paint Text paint from canvas
 * @return Rect of text background
 */
fun String.getTextBackgroundSize(
    x: Float,
    y: Float,
    paint: TextPaint
): RectF {
    val fontMetrics: Paint.FontMetrics = paint.fontMetrics
    val halfTextLength = paint.measureText(this) / 2 + 5
    return RectF(
        (x - halfTextLength),
        (y + fontMetrics.top),
        (x + halfTextLength),
        (y + fontMetrics.bottom)
    )
}