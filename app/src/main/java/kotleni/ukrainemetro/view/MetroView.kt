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
import unicon.metro.kharkiv.R

class MetroView(context: Context, attr: AttributeSet): View(context, attr) {
    private class MyOnTouchListener(
        private val gestureTouchEvent: (event: MotionEvent) -> Unit,
        private val updateGestureScrollEvent: (x: Float, y: Float) -> Unit,
        private val updateCanvas: () -> Unit
    ): OnTouchListener {
        private var lock = false
        private var mod = false

        private var dX = 0f
        private var dY = 0f

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(p0: View, event: MotionEvent): Boolean {
            if (event.pointerCount > 1) {
                gestureTouchEvent.invoke(event)
                lock = true
                return true
            } else {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = event.rawX
                        dY = event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!lock) {
                            mod = true

                            val x = -(dX - event.rawX)
                            val y = -(dY - event.rawY)

                            updateGestureScrollEvent.invoke(x, y)

                            dX = event.rawX
                            dY = event.rawY

                            updateCanvas.invoke() // invalidate
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        // reset
                        lock = false
                        mod = false
                    }
                    else -> return false
                }
            }

            return true
        }
    }

    private class ScaleListener(var metroView: MetroView) : MyScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: MyScaleGestureDetector?): Boolean {
            metroView.mScaleFactor *= detector!!.getScaleFactor()
            metroView.mScaleFactor = Math.max(SCALE_FACTOR_MIN, Math.min(metroView.mScaleFactor, SCALE_FACTOR_MAX))

            metroView.scaleX = metroView.mScaleFactor
            metroView.scaleY = metroView.mScaleFactor

            return super.onScale(detector)
        }
    }

    private var data = ArrayList<Element>()

    private val size = Size(240, 320)
    private val paint = Paint()
    private val textPaint = TextPaint()

    // colors
    private val colorTextA = context.getColorByAttr(R.attr.colorAccent)    // text rect
    private val colorTextB = context.getColorByAttr(R.attr.colorOnPrimary) // text color
    private val colorTrans = Color.parseColor(COLOR_TRANS)

    // drawing
    private val padding = 0f // todo: remove
    private var scale = 2f

    // scroll
    private var scrollX = 0f
    private var scrollY = 0f

    private var mScaleGestureDetector: MyScaleGestureDetector? = null
    private var mScaleFactor = 1.0f

    // for draw
    private var defVector = Vector(-1, -1)
    private var lastBranchVec = defVector

    init {
        textPaint.color = colorTextB
        textPaint.strokeWidth = 3f
        textPaint.textAlign = Paint.Align.CENTER

        mScaleGestureDetector = MyScaleGestureDetector(context, ScaleListener(this))
        mScaleGestureDetector?.isQuickScaleEnabled = SCALE_QUICK_ENABLE

        MyOnTouchListener(
            gestureTouchEvent = {
                mScaleGestureDetector?.onTouchEvent(it)
            },
            updateCanvas = {
                invalidate()
            },
            updateGestureScrollEvent = { x, y ->
                scrollX += x / mScaleFactor
                scrollY += y / mScaleFactor
            }
        ).also { setOnTouchListener(it) }
    }

    fun updateData(arr: List<Element>) {
        data.clear()
        data.addAll(arr)

        invalidate()
    }

    // todo: move to background
    private fun mathMapVectors(): MinMaxVectorF {
        val vectorMax = VectorF(0f, 0f)
        val vectorMin = VectorF(9999f, 9999f)

        // find max x and y
        data.forEach {
            if(it is BranchElement) {
                it.points.forEach {
                    if(it.pos.x > vectorMax.x) {
                        vectorMax.x = it.pos.x.toFloat()
                    }
                    if(it.pos.x < vectorMin.x) {
                        vectorMin.x = it.pos.x.toFloat()
                    }

                    if(it.pos.y > vectorMax.y) {
                        vectorMax.y = it.pos.y.toFloat()
                    }

                    if(it.pos.y < vectorMin.y) {
                        vectorMin.y = it.pos.y.toFloat()
                    }
                }
            }
        }

        return MinMaxVectorF(vectorMin, vectorMax)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        size.w = w
        size.h = h

        mathMapVectors().also {
            scrollX = (w / 2) - (((it.maxVector.x + it.minVector.x) / 2))
            scrollY = (h / 2) - (((it.maxVector.y + it.minVector.y) / 2))

//            scrollX /= 2
//            scrollY /= 2
        }

        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas?) {
        var scale = 1
        // draw lines
        data.forEach {
            if (it is BranchElement) {
                lastBranchVec = defVector

                it.points.forEach { p: Point ->
                    paint.style = Paint.Style.FILL
                    paint.color = it.color
                    paint.strokeWidth = LINE_WIDTH

                    paint.strokeCap = Paint.Cap.ROUND

                    if(lastBranchVec.x > 0)
                        canvas?.drawLine(
                            scale * (lastBranchVec.x.toFloat() + scrollX),
                            scale * (lastBranchVec.y.toFloat() + scrollY),
                            scale * (p.pos.x.toFloat() + scrollX),
                            scale * (p.pos.y.toFloat() + scrollY),
                            paint)

                    lastBranchVec = p.pos
                }
            }
        }
        // draw trans
        data.forEach { el ->
            if (el is TransElement) {
                paint.style = Paint.Style.FILL
                paint.color = colorTrans
                paint.strokeWidth = LINE_WIDTH

                canvas?.drawLine(
                    scale * (scrollX + el.from.x.toFloat()),
                    scale * (scrollY + el.from.y.toFloat()),
                    scale * (scrollX + el.to.x.toFloat()),
                    scale * (scrollY + el.to.y.toFloat()),
                    paint)
            }
        }

        // draw stations
        data.forEach {
            if (it is BranchElement) {
                it.points.forEach { p: Point ->
                    if(p.name != null) {
                        paint.color = colorTextA
                        paint.style = Paint.Style.FILL
                        paint.textSize = 5f

                        val rect = getTextBackgroundSize((scrollX + p.pos.x + 0f) * scale, (scrollY + p.pos.y + 0f) * scale, resources.getString(p.name!!), textPaint)
                        canvas!!.drawRoundRect(
                            RectF(rect.left.toFloat() - BUBLE_SCALE,
                                rect.top.toFloat() - BUBLE_SCALE,
                                rect.right.toFloat() + BUBLE_SCALE,
                                rect.bottom.toFloat() + BUBLE_SCALE
                            ),
                            28f, 28f, paint)
                        canvas.drawText(resources.getString(p.name!!), (scrollX + p.pos.x + 0f) * scale, (scrollY + p.pos.y + 0f) * scale, textPaint)
                    }
                }
            }
        }
    }
}