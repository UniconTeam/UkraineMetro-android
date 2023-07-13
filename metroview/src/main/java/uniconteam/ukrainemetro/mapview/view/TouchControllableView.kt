package uniconteam.ukrainemetro.mapview.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import uniconteam.ukrainemetro.mapview.Const
import kotlin.math.max
import kotlin.math.min

open class TouchControllableView(context: Context, attrs: AttributeSet) : View(context, attrs), View.OnTouchListener {
    private val doubleTapTime = 300

    private var lock = false
    private var mod = false

    private var dX = 0f
    private var dY = 0f

    protected var scrollX = 0f
    protected var scrollY = 0f

    private var lastActionDownTime = 0L
    private var mScaleFactor = 1.0f

    private var mScaleGestureDetector: MyScaleGestureDetector = MyScaleGestureDetector(context, ScaleListener { factor ->
        updateScale(factor)
    })

    init {
        // TODO: Fix leaking
        setOnTouchListener(this)
    }

    private fun updateScale(factor: Float) {
        mScaleFactor *= factor
        mScaleFactor = max(Const.SCALE_FACTOR_MIN, min(mScaleFactor, Const.SCALE_FACTOR_MAX))

        scaleX = mScaleFactor
        scaleY = mScaleFactor
    }

    private fun forceUpdateScale(isMaximal: Boolean) {
        mScaleFactor = if (isMaximal) Const.SCALE_FACTOR_MAX else Const.SCALE_FACTOR_MIN

        scaleX = mScaleFactor
        scaleY = mScaleFactor
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.pointerCount > 1) {
            mScaleGestureDetector.onTouchEvent(event)
            lock = true
            return true
        } else {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Check double tap
                    if(System.currentTimeMillis() - lastActionDownTime < doubleTapTime) {
                        if(mScaleFactor == Const.SCALE_FACTOR_MAX)
                            forceUpdateScale(false)
                        else
                            forceUpdateScale(true)
                    }

                    lastActionDownTime = System.currentTimeMillis()

                    dX = event.rawX
                    dY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!lock) {
                        mod = true

                        val x = -(dX - event.rawX)
                        val y = -(dY - event.rawY)

                        // Update gesture scroll event
                        scrollX += x / mScaleFactor
                        scrollY += y / mScaleFactor

                        dX = event.rawX
                        dY = event.rawY

                        invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    // Reset
                    lock = false
                    mod = false
                }
                else -> return false
            }
        }

        return true
    }
}