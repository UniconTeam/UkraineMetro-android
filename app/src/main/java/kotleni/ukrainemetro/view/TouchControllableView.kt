package kotleni.ukrainemetro.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotleni.ukrainemetro.SCALE_FACTOR_MAX
import kotleni.ukrainemetro.SCALE_FACTOR_MIN
import kotleni.ukrainemetro.SCALE_QUICK_ENABLE

open class TouchControllableView(context: Context, attrs: AttributeSet) : View(context, attrs), View.OnTouchListener {
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
        mScaleFactor = Math.max(SCALE_FACTOR_MIN, Math.min(mScaleFactor, SCALE_FACTOR_MAX))

        scaleX = mScaleFactor
        scaleY = mScaleFactor
    }

    private fun forceUpdateScale(isMaximal: Boolean) {
        mScaleFactor = if (isMaximal) SCALE_FACTOR_MAX else SCALE_FACTOR_MIN

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
                    if(System.currentTimeMillis() - lastActionDownTime < 400) {
                        if(mScaleFactor == SCALE_FACTOR_MAX)
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