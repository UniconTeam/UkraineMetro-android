package uniconteam.ukrainemetro.mapview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import androidx.core.content.res.getIntOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uniconteam.ukrainemetro.*
import uniconteam.ukrainemetro.mapview.Const
import uniconteam.ukrainemetro.mapview.R
import uniconteam.ukrainemetro.mapview.extensions.getColorByAttr
import uniconteam.ukrainemetro.mapview.extensions.getTextBackgroundSize

class MetroView(context: Context, attr: AttributeSet): TouchControllableView(context, attr) {
    private var data = ArrayList<uniconteam.ukrainemetro.mapview.entities.elements.Element>()

    private val size = uniconteam.ukrainemetro.mapview.entities.Size(240, 320)
    private val paint = Paint()
    private val textPaint = TextPaint()

    // Colors
    private var colorTextA: Int = Color.parseColor(Const.COLOR_RECT_BG) // text rect
    private var colorTextB: Int = Color.parseColor(Const.COLOR_RECT_TEXT) // text color
    private val colorTrans = Color.parseColor(Const.COLOR_TRANS)

    // For draw
    private var defVector = uniconteam.ukrainemetro.mapview.entities.Vector(-1, -1)
    private var lastBranchVec = defVector

    init {
        textPaint.color = colorTextB
        textPaint.strokeWidth = 3f
        textPaint.textAlign = Paint.Align.CENTER
    }

    fun updateData(arr: List<uniconteam.ukrainemetro.mapview.entities.elements.Element>) {
        data.clear()
        data.addAll(arr)

        invalidate()
    }

    private fun updateMapVectors() = CoroutineScope(Dispatchers.IO).launch {
        val vectorMax = uniconteam.ukrainemetro.mapview.entities.VectorF(0f, 0f)
        val vectorMin = uniconteam.ukrainemetro.mapview.entities.VectorF(9999f, 9999f)

        // FIXME: stupid code for fix bug
        // If map have only one branch
        // Map not showing anything
        if(data.size == 1)
            data.addAll(data)

        // Find max x and y
        data.forEach { element ->
            if(element is uniconteam.ukrainemetro.mapview.entities.elements.BranchElement) {
                element.points.forEach { point ->
                    if(point.pos.x > vectorMax.x) {
                        vectorMax.x = point.pos.x.toFloat()
                    } else if(point.pos.x < vectorMin.x) {
                        vectorMin.x = point.pos.x.toFloat()
                    } else if(point.pos.y > vectorMax.y) {
                        vectorMax.y = point.pos.y.toFloat()
                    } else if(point.pos.y < vectorMin.y) {
                        vectorMin.y = point.pos.y.toFloat()
                    }
                }
            }
        }

        scrollX = (size.w / 2) - (((vectorMax.x + vectorMin.x) / 2))
        scrollY = (size.h / 2) - (((vectorMax.y + vectorMin.y) / 2))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        size.w = w
        size.h = h

        updateMapVectors()

        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        // Draw lines
        data.forEach {
            if (it is uniconteam.ukrainemetro.mapview.entities.elements.BranchElement) {
                lastBranchVec = defVector

                it.points.forEach { p: uniconteam.ukrainemetro.mapview.entities.Point ->
                    paint.style = Paint.Style.FILL
                    paint.color = it.color
                    paint.strokeWidth = Const.LINE_WIDTH

                    paint.strokeCap = Paint.Cap.ROUND

                    if(lastBranchVec.x > 0)
                        canvas.drawLine(
                            (lastBranchVec.x.toFloat() + scrollX),
                            (lastBranchVec.y.toFloat() + scrollY),
                            (p.pos.x.toFloat() + scrollX),
                            (p.pos.y.toFloat() + scrollY),
                            paint)

                    lastBranchVec = p.pos
                }
            }
        }

        // Draw trans
        data.forEach { el ->
            if (el is uniconteam.ukrainemetro.mapview.entities.elements.TransElement) {
                paint.style = Paint.Style.FILL
                paint.color = colorTrans
                paint.strokeWidth = Const.LINE_WIDTH

                canvas.drawLine(
                    (scrollX + el.from.x.toFloat()),
                    (scrollY + el.from.y.toFloat()),
                    (scrollX + el.to.x.toFloat()),
                    (scrollY + el.to.y.toFloat()),
                    paint)
            }
        }

        // Draw stations
        data.forEach {
            if (it is uniconteam.ukrainemetro.mapview.entities.elements.BranchElement) {
                it.points.forEach { p: uniconteam.ukrainemetro.mapview.entities.Point ->
                    if(p.name != null) {
                        paint.color = colorTextA
                        paint.style = Paint.Style.FILL
                        paint.textSize = 5f

                        val rect = resources.getString(p.name!!).getTextBackgroundSize((scrollX + p.pos.x + 0f), (scrollY + p.pos.y + 0f), textPaint)
                        rect.set(
                            rect.left - Const.BUBLE_SCALE, rect.top - Const.BUBLE_SCALE,
                            rect.right + Const.BUBLE_SCALE, rect.bottom + Const.BUBLE_SCALE
                        )
                        canvas.drawRoundRect(
                            rect,
                            28f, 28f, paint)
                        canvas.drawText(resources.getString(p.name!!), (scrollX + p.pos.x + 0f), (scrollY + p.pos.y + 0f), textPaint)
                    }
                }
            }
        }

        invalidate()
    }
}