package kotleni.ukrainemetro.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotleni.ukrainemetro.*
import kotleni.ukrainemetro.types.*
import kotleni.ukrainemetro.types.elements.BranchElement
import kotleni.ukrainemetro.types.elements.Element
import kotleni.ukrainemetro.types.elements.TransElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import unicon.metro.kharkiv.R

class MetroView(context: Context, attr: AttributeSet): TouchControllableView(context, attr) {
    private var data = ArrayList<Element>()

    private val size = Size(240, 320)
    private val paint = Paint()
    private val textPaint = TextPaint()

    // colors
    private val colorTextA = context.getColorByAttr(R.attr.colorAccent)    // text rect
    private val colorTextB = context.getColorByAttr(R.attr.colorOnPrimary) // text color
    private val colorTrans = Color.parseColor(COLOR_TRANS)

    // for draw
    private var defVector = Vector(-1, -1)
    private var lastBranchVec = defVector

    init {
        textPaint.color = colorTextB
        textPaint.strokeWidth = 3f
        textPaint.textAlign = Paint.Align.CENTER
    }

    fun updateData(arr: List<Element>) {
        data.clear()
        data.addAll(arr)

        invalidate()
    }

    private fun updateMapVectors() = CoroutineScope(Dispatchers.IO).launch {
        val vectorMax = VectorF(0f, 0f)
        val vectorMin = VectorF(9999f, 9999f)

        // Find max x and y
        data.forEach {
            if(it is BranchElement) {
                it.points.forEach {
                    if(it.pos.x > vectorMax.x) {
                        vectorMax.x = it.pos.x.toFloat()
                    } else if(it.pos.x < vectorMin.x) {
                        vectorMin.x = it.pos.x.toFloat()
                    } else if(it.pos.y > vectorMax.y) {
                        vectorMax.y = it.pos.y.toFloat()
                    } else if(it.pos.y < vectorMin.y) {
                        vectorMin.y = it.pos.y.toFloat()
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
            if (it is BranchElement) {
                lastBranchVec = defVector

                it.points.forEach { p: Point ->
                    paint.style = Paint.Style.FILL
                    paint.color = it.color
                    paint.strokeWidth = LINE_WIDTH

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
            if (el is TransElement) {
                paint.style = Paint.Style.FILL
                paint.color = colorTrans
                paint.strokeWidth = LINE_WIDTH

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
            if (it is BranchElement) {
                it.points.forEach { p: Point ->
                    if(p.name != null) {
                        paint.color = colorTextA
                        paint.style = Paint.Style.FILL
                        paint.textSize = 5f

                        val rect = getTextBackgroundSize((scrollX + p.pos.x + 0f), (scrollY + p.pos.y + 0f), resources.getString(p.name!!), textPaint)
                        canvas.drawRoundRect(
                            RectF(rect.left - BUBLE_SCALE,
                                rect.top - BUBLE_SCALE,
                                rect.right + BUBLE_SCALE,
                                rect.bottom + BUBLE_SCALE
                            ),
                            28f, 28f, paint)
                        canvas.drawText(resources.getString(p.name!!), (scrollX + p.pos.x + 0f), (scrollY + p.pos.y + 0f), textPaint)
                    }
                }
            }
        }

        invalidate()
    }
}