package uniconteam.ukrainemetro.mapview.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import uniconteam.ukrainemetro.mapview.Const
import uniconteam.ukrainemetro.mapview.entities.Size
import uniconteam.ukrainemetro.mapview.entities.elements.BranchElement
import uniconteam.ukrainemetro.mapview.entities.elements.Element
import uniconteam.ukrainemetro.mapview.entities.elements.TransElement
import kotlin.math.max
import kotlin.math.min

/**
 * Vulkan-backed metro map view. New API replacing the legacy Canvas [MetroView]:
 * map lines and FreeType-rasterized station labels are drawn by a native
 * Vulkan renderer (libmetroview.so) into this SurfaceView.
 *
 * Kept API-compatible where it matters: [updateData] accepts the same
 * [Element] list, pan/zoom gestures behave like before.
 */
class VulkanMetroView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, View.OnTouchListener {

    private var nativeHandle: Long = 0L
    private var surfaceAvailable = false
    private var pendingData = false

    private val size = Size(240, 320)

    private var scrollXf = 0f
    private var scrollYf = 0f
    private var mapCenterX = 0f
    private var mapCenterY = 0f
    private var hasCenter = false

    // Touch state (ported from TouchControllableView).
    private val doubleTapTime = 300
    private var lock = false
    private var dX = 0f
    private var dY = 0f
    private var lastActionDownTime = 0L
    private var scaleFactor = 1.0f
    private val scaleDetector = MyScaleGestureDetector(context, ScaleListener { factor ->
        updateScale(factor)
    })

    private val colorBg = resolveBackground()
    private val colorBubble = Color.parseColor(Const.COLOR_RECT_BG)
    private val colorText = Color.parseColor(Const.COLOR_RECT_TEXT)

    init {
        holder.addCallback(this)
        setOnTouchListener(this)
    }

    // ------------------------------------------------------------------ data

    fun updateData(arr: List<Element>) {
        val branchColors = mutableListOf<Int>()
        val branchPts = mutableListOf<Float>()
        val branchCounts = mutableListOf<Int>()
        val transPts = mutableListOf<Float>()
        val labelTexts = mutableListOf<String>()
        val labelPos = mutableListOf<Float>()

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (el in arr) {
            if (el is BranchElement) {
                branchColors.add(el.color)
                branchCounts.add(el.points.size)
                for (p in el.points) {
                    val x = p.pos.x.toFloat()
                    val y = p.pos.y.toFloat()
                    branchPts.add(x)
                    branchPts.add(y)
                    if (p.name != null) {
                        labelTexts.add(resources.getString(p.name!!))
                        labelPos.add(x)
                        labelPos.add(y)
                    }
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }
        for (el in arr) {
            if (el is TransElement) {
                transPts.add(el.from.x.toFloat())
                transPts.add(el.from.y.toFloat())
                transPts.add(el.to.x.toFloat())
                transPts.add(el.to.y.toFloat())
            }
        }

        if (minX <= maxX && minY <= maxY) {
            mapCenterX = (maxX + minX) / 2f
            mapCenterY = (maxY + minY) / 2f
            hasCenter = true
            recenter()
        }

        if (nativeHandle == 0L) {
            pendingData = true
            pendingBranchColors = branchColors.toIntArray()
            pendingBranchPts = branchPts.toFloatArray()
            pendingBranchCounts = branchCounts.toIntArray()
            pendingTransPts = transPts.toFloatArray()
            pendingLabelTexts = labelTexts.toTypedArray()
            pendingLabelPos = labelPos.toFloatArray()
            return
        }
        pushData(
            branchColors.toIntArray(), branchPts.toFloatArray(),
            branchCounts.toIntArray(), transPts.toFloatArray(),
            labelTexts.toTypedArray(), labelPos.toFloatArray()
        )
    }

    private fun recenter() {
        if (!hasCenter) return
        scrollXf = size.w / 2f - mapCenterX
        scrollYf = size.h / 2f - mapCenterY
    }
    private var pendingBranchColors = IntArray(0)
    private var pendingBranchPts = FloatArray(0)
    private var pendingBranchCounts = IntArray(0)
    private var pendingTransPts = FloatArray(0)
    private var pendingLabelTexts = emptyArray<String>()
    private var pendingLabelPos = FloatArray(0)

    private fun pushData(
        colors: IntArray, pts: FloatArray, counts: IntArray,
        trans: FloatArray, texts: Array<String>, pos: FloatArray
    ) {
        nativeSetData(nativeHandle, colors, pts, counts, trans, texts, pos)
        pushView()
    }

    // ----------------------------------------------------------------- view

    private fun pushView() {
        if (nativeHandle == 0L || !surfaceAvailable) return
        val c = normal(colorText)
        nativeSetView(
            nativeHandle, scrollXf, scrollYf, scaleFactor,
            (width / 2f).coerceAtLeast(1f), (height / 2f).coerceAtLeast(1f),
            c[0], c[1], c[2], c[3]
        )
        nativeRender(nativeHandle)
    }

    private fun updateScale(factor: Float) {
        scaleFactor = max(Const.SCALE_FACTOR_MIN, min(scaleFactor * factor, Const.SCALE_FACTOR_MAX))
        pushView()
    }

    private fun forceUpdateScale(isMaximal: Boolean) {
        scaleFactor = if (isMaximal) Const.SCALE_FACTOR_MAX else Const.SCALE_FACTOR_MIN
        pushView()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.pointerCount > 1) {
            scaleDetector.onTouchEvent(event)
            lock = true
            return true
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (System.currentTimeMillis() - lastActionDownTime < doubleTapTime) {
                    if (scaleFactor == Const.SCALE_FACTOR_MAX) forceUpdateScale(false)
                    else forceUpdateScale(true)
                }
                lastActionDownTime = System.currentTimeMillis()
                dX = event.rawX
                dY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                if (!lock) {
                    val x = -(dX - event.rawX)
                    val y = -(dY - event.rawY)
                    scrollXf += x / scaleFactor
                    scrollYf += y / scaleFactor
                    dX = event.rawX
                    dY = event.rawY
                    pushView()
                }
            }
            MotionEvent.ACTION_UP -> lock = false
            else -> return false
        }
        return true
    }

    // --------------------------------------------------------------- surface

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (nativeHandle == 0L) {
            val bg = normal(colorBg)
            val bubble = normal(colorBubble)
            val text = normal(colorText)
            nativeHandle = nativeInit(
                context.assets,
                floatArrayOf(
                    bg[0], bg[1], bg[2], bg[3],
                    bubble[0], bubble[1], bubble[2], bubble[3],
                    text[0], text[1], text[2], text[3]
                )
            )
        }
        surfaceAvailable = true
        nativeSurfaceCreated(nativeHandle, holder.surface)
        nativeResize(nativeHandle, width.coerceAtLeast(1), height.coerceAtLeast(1))
        if (pendingData) {
            pendingData = false
            pushData(
                pendingBranchColors, pendingBranchPts, pendingBranchCounts,
                pendingTransPts, pendingLabelTexts, pendingLabelPos
            )
        } else {
            pushView()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        size.w = w
        size.h = h
        recenter()
        if (nativeHandle != 0L && surfaceAvailable) {
            nativeResize(nativeHandle, w.coerceAtLeast(1), h.coerceAtLeast(1))
            pushView()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceAvailable = false
        if (nativeHandle != 0L) {
            nativeSurfaceDestroyed(nativeHandle)
        }
    }

    override fun onDetachedFromWindow() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0L
        }
        super.onDetachedFromWindow()
    }

    // ----------------------------------------------------------------- misc

    private fun resolveBackground(): Int {
        val tv = android.util.TypedValue()
        return if (context.theme.resolveAttribute(android.R.attr.colorBackground, tv, true)) {
            if (tv.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT &&
                tv.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT
            ) {
                tv.data
            } else {
                Color.WHITE
            }
        } else {
            Color.WHITE
        }
    }

    private fun normal(color: Int): FloatArray = floatArrayOf(
        Color.red(color) / 255f,
        Color.green(color) / 255f,
        Color.blue(color) / 255f,
        Color.alpha(color) / 255f
    )

    private external fun nativeInit(assetManager: android.content.res.AssetManager, colors: FloatArray): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeSurfaceCreated(handle: Long, surface: android.view.Surface)
    private external fun nativeSurfaceDestroyed(handle: Long)
    private external fun nativeResize(handle: Long, w: Int, h: Int)
    private external fun nativeSetData(
        handle: Long, branchColors: IntArray, branchPts: FloatArray,
        branchCounts: IntArray, transPts: FloatArray,
        labelTexts: Array<String>, labelPos: FloatArray
    )
    private external fun nativeSetView(
        handle: Long, scrollX: Float, scrollY: Float, scale: Float,
        pivotX: Float, pivotY: Float,
        textR: Float, textG: Float, textB: Float, textA: Float
    )
    private external fun nativeRender(handle: Long)

    companion object {
        init {
            System.loadLibrary("metroview")
        }
    }
}
