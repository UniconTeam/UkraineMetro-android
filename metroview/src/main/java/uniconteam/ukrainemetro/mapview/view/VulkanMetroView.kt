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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Vulkan-backed metro map view. New API replacing the legacy Canvas [MetroView]:
 * map lines and FreeType-rasterized station labels are drawn by a native
 * Vulkan renderer (libmetroview.so) into this SurfaceView.
 *
 * Kept API-compatible where it matters: [updateData] accepts the same
 * [Element] list. Gestures: one-finger drag pans, two-finger drag pans,
 * pinch zooms, twist rotates — all anchored so content stays glued to
 * the fingers; double tap smoothly zooms in/out at the tap point.
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

    // Touch + camera state. The camera is (scroll, scale, angle); the shader
    // renders screen = R(angle) * ((map + scroll - pivot) * scale) + pivot.
    private val doubleTapTime = 300
    private var lastActionDownTime = 0L
    private var scaleFactor = 1.0f
    private var angleRad = 0f
    var rotationEnabled = true
    var zoomLimitsEnabled = true
    private var zoomAnim: android.animation.ValueAnimator? = null

    private var mode = MODE_NONE
    private var idA = -1
    private var idB = -1
    private var dX = 0f
    private var dY = 0f
    private var pendingSync = false
    private var prevFx = 0f
    private var prevFy = 0f
    private var prevSpan = 0f
    private var prevAngle = 0f

    private val colorBg = resolveBackground()

    // Google Maps style: plain text labels, no background bubbles.
    // White on dark map backgrounds, dark on light ones.
    private val labelText: FloatArray by lazy {
        if (isDark(colorBg)) floatArrayOf(1f, 1f, 1f, 1f)
        else normal(Color.parseColor(Const.COLOR_RECT_TEXT))
    }

    private fun isDark(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return (0.299f * r + 0.587f * g + 0.114f * b) < 128f
    }

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
        val labelColors = mutableListOf<Int>()

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
                        labelColors.add(el.color)
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
            if (width > 0) size.w = width
            if (height > 0) size.h = height
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
            pendingLabelColors = labelColors.toIntArray()
            return
        }
        pushData(
            branchColors.toIntArray(), branchPts.toFloatArray(),
            branchCounts.toIntArray(), transPts.toFloatArray(),
            labelTexts.toTypedArray(), labelPos.toFloatArray(),
            labelColors.toIntArray()
        )
    }

    private var centeredOnce = false

    private fun recenter() {
        if (!hasCenter) return
        // After a recreation (rotation, theme, locale change) the saved
        // camera wins over the default centering, translated into the
        // current size so the same map point stays under the center.
        val saved = pendingCamera
        if (saved != null) {
            pendingCamera = null
            scrollXf = size.w / 2f - saved.centerX
            scrollYf = size.h / 2f - saved.centerY
            scaleFactor = saved.scale
            angleRad = saved.angle
        } else {
            scrollXf = size.w / 2f - mapCenterX
            scrollYf = size.h / 2f - mapCenterY
        }
        centeredOnce = true
    }

    private data class SavedCamera(
        val centerX: Float,
        val centerY: Float,
        val scale: Float,
        val angle: Float
    )
    private var pendingCamera: SavedCamera? = null

    override fun onSaveInstanceState(): android.os.Parcelable {
        val superState = super.onSaveInstanceState()
        return android.os.Bundle().apply {
            putParcelable("super", superState)
            // Persist the map point under the screen center, not the raw
            // scroll: the view size may differ after recreation.
            putFloat("centerX", size.w / 2f - scrollXf)
            putFloat("centerY", size.h / 2f - scrollYf)
            putFloat("scale", scaleFactor)
            putFloat("angle", angleRad)
        }
    }

    override fun onRestoreInstanceState(state: android.os.Parcelable?) {
        if (state is android.os.Bundle) {
            pendingCamera = SavedCamera(
                state.getFloat("centerX", size.w / 2f - scrollXf),
                state.getFloat("centerY", size.h / 2f - scrollYf),
                state.getFloat("scale", scaleFactor),
                state.getFloat("angle", angleRad)
            )
            super.onRestoreInstanceState(state.getParcelable("super"))
        } else {
            super.onRestoreInstanceState(state)
        }
    }
    private var pendingBranchColors = IntArray(0)
    private var pendingBranchPts = FloatArray(0)
    private var pendingBranchCounts = IntArray(0)
    private var pendingTransPts = FloatArray(0)
    private var pendingLabelTexts = emptyArray<String>()
    private var pendingLabelPos = FloatArray(0)
    private var pendingLabelColors = IntArray(0)

    private fun pushData(
        colors: IntArray, pts: FloatArray, counts: IntArray,
        trans: FloatArray, texts: Array<String>, pos: FloatArray,
        labelColors: IntArray
    ) {
        nativeSetData(nativeHandle, colors, pts, counts, trans, texts, pos, labelColors)
        pushView()
    }

    // ----------------------------------------------------------------- view

    var debugOverlayEnabled = false
        private set
    private var debugOverlayTopMargin = 0

    fun setDebugOverlay(enabled: Boolean, topMarginPx: Int = 0) {
        debugOverlayEnabled = enabled
        debugOverlayTopMargin = topMarginPx
        if (nativeHandle != 0L) {
            nativeSetDebugOverlay(nativeHandle, enabled, topMarginPx)
        }
        // Overlay on = continuous rendering for live FPS/graph.
        pushView()
    }

    private fun pushView() {
        if (nativeHandle == 0L || !surfaceAvailable) return
        nativeSetView(
            nativeHandle, scrollXf, scrollYf, scaleFactor, angleRad,
            (width / 2f).coerceAtLeast(1f), (height / 2f).coerceAtLeast(1f),
            labelText[0], labelText[1], labelText[2], labelText[3]
        )
        if (nativeRender(nativeHandle) && !animPending) {
            // Labels still easing toward their slots (or the debug
            // overlay is live): keep rendering until the native side
            // reports everything settled.
            animPending = true
            postOnAnimation {
                animPending = false
                if (surfaceAvailable && nativeHandle != 0L) pushView()
            }
        }
    }
    private var animPending = false

    // ------------------------------------------------------------ camera

    private fun rot(x: Float, y: Float, a: Float): Pair<Float, Float> {
        val c = cos(a)
        val s = sin(a)
        return Pair(c * x - s * y, s * x + c * y)
    }

    private fun wrapAngle(a: Float): Float {
        val twoPi = PI.toFloat() * 2f
        var v = a % twoPi
        if (v > PI.toFloat()) v -= twoPi
        if (v < -PI.toFloat()) v += twoPi
        return v
    }

    // Screen (view-local px) -> map units under the current camera.
    private fun mapFromScreen(sx: Float, sy: Float): Pair<Float, Float> {
        val cx = width / 2f
        val cy = height / 2f
        if (cx <= 0f || cy <= 0f || scaleFactor <= 0f) {
            return Pair(sx - scrollXf, sy - scrollYf)
        }
        val (rx, ry) = rot((sx - cx) / scaleFactor, (sy - cy) / scaleFactor, -angleRad)
        return Pair(rx + cx - scrollXf, ry + cy - scrollYf)
    }

    // Move scroll so map point (mx, my) sits under screen point (sx, sy)
    // at the given scale/angle.
    private fun anchorScroll(mx: Float, my: Float, sx: Float, sy: Float, k: Float, a: Float) {
        val cx = width / 2f
        val cy = height / 2f
        if (cx <= 0f || cy <= 0f || k <= 0f) return
        val (rx, ry) = rot((sx - cx) / k, (sy - cy) / k, -a)
        scrollXf = rx + cx - mx
        scrollYf = ry + cy - my
    }

    private fun panBy(dxScreen: Float, dyScreen: Float) {
        if ((dxScreen != 0f || dyScreen != 0f) && scaleFactor > 0f) {
            val (rx, ry) = rot(dxScreen / scaleFactor, dyScreen / scaleFactor, -angleRad)
            scrollXf += rx
            scrollYf += ry
        }
    }

    private fun applyScale(newScale: Float, focusX: Float, focusY: Float) {
        val k = coerceZoom(newScale)
        if (k == scaleFactor) return
        val (mx, my) = mapFromScreen(focusX, focusY)
        scaleFactor = k
        anchorScroll(mx, my, focusX, focusY, k, angleRad)
        pushView()
    }

    fun setMapInteraction(rotation: Boolean, zoomLimited: Boolean) {
        rotationEnabled = rotation
        zoomLimitsEnabled = zoomLimited
        // Turning rotation off straightens the map; turning limits back
        // on pulls an out-of-range zoom back inside.
        if (!rotation) angleRad = 0f
        if (zoomLimited) scaleFactor = scaleFactor.coerceIn(
            Const.SCALE_FACTOR_MIN, Const.SCALE_FACTOR_MAX)
        pushView()
    }

    fun resetView() {
        cancelZoomAnim()
        if (hasCenter) recenter()
        scaleFactor = 1.0f
        angleRad = 0f
        pushView()
    }

    private fun coerceZoom(v: Float): Float =
        if (zoomLimitsEnabled) v.coerceIn(Const.SCALE_FACTOR_MIN, Const.SCALE_FACTOR_MAX)
        else v.coerceIn(ZOOM_ABS_MIN, ZOOM_ABS_MAX)

    private fun cancelZoomAnim() {
        zoomAnim?.cancel()
        zoomAnim = null
    }

    private fun animateZoomTo(target: Float, focusX: Float, focusY: Float) {
        cancelZoomAnim()
        val start = scaleFactor
        if (start == target) return
        zoomAnim = android.animation.ValueAnimator.ofFloat(start, target).apply {
            duration = 220
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { anim ->
                applyScale(anim.animatedValue as Float, focusX, focusY)
            }
            start()
        }
    }

    // ------------------------------------------------------------ gestures

    private fun dualFrame(e: MotionEvent, i0: Int, i1: Int): FloatArray {
        val x0 = e.getX(i0)
        val y0 = e.getY(i0)
        val x1 = e.getX(i1)
        val y1 = e.getY(i1)
        return floatArrayOf(
            (x0 + x1) / 2f, (y0 + y1) / 2f,
            hypot(x1 - x0, y1 - y0),
            atan2(y1 - y0, x1 - x0)
        )
    }

    private fun baselineDual(e: MotionEvent) {
        val i0 = e.findPointerIndex(idA)
        val i1 = e.findPointerIndex(idB)
        if (i0 < 0 || i1 < 0) return
        val f = dualFrame(e, i0, i1)
        prevFx = f[0]
        prevFy = f[1]
        prevSpan = f[2]
        prevAngle = f[3]
    }

    private fun handleDualMove(e: MotionEvent, i0: Int, i1: Int) {
        val f = dualFrame(e, i0, i1)
        val fx = f[0]
        val fy = f[1]
        val span = f[2]
        val ang = f[3]
        // 1. Two-finger drag pans the map (free panning while pinching).
        panBy(fx - prevFx, fy - prevFy)
        // 2. Pinch zoom + twist rotate around the current focal point.
        val (mx, my) = mapFromScreen(fx, fy)
        var k = scaleFactor
        if (prevSpan > 0f && span > 0f) {
            k = coerceZoom(scaleFactor * (span / prevSpan))
        }
        scaleFactor = k
        val a = if (rotationEnabled) wrapAngle(angleRad + wrapAngle(ang - prevAngle)) else angleRad
        angleRad = a
        anchorScroll(mx, my, fx, fy, k, a)
        prevFx = fx
        prevFy = fy
        prevSpan = span
        prevAngle = ang
        pushView()
    }

    private fun dropToSingle() {
        mode = MODE_SINGLE
        idB = -1
        // Resync single-finger deltas on the next MOVE so the map doesn't jump.
        pendingSync = true
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelZoomAnim()
                mode = MODE_SINGLE
                idA = event.getPointerId(0)
                idB = -1
                pendingSync = false
                // Double tap: smooth zoom in/out anchored at the tap point.
                if (System.currentTimeMillis() - lastActionDownTime < doubleTapTime) {
                    val target = if (zoomLimitsEnabled) {
                        if (scaleFactor == Const.SCALE_FACTOR_MAX)
                            Const.SCALE_FACTOR_MIN else Const.SCALE_FACTOR_MAX
                    } else {
                        if (scaleFactor < ZOOM_ABS_MAX * 0.9f) scaleFactor * 2f
                        else ZOOM_ABS_MIN
                    }
                    animateZoomTo(target, event.x, event.y)
                }
                lastActionDownTime = System.currentTimeMillis()
                dX = event.rawX
                dY = event.rawY
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                cancelZoomAnim()
                if (event.pointerCount >= 2) {
                    if (mode != MODE_DUAL) {
                        idA = event.getPointerId(0)
                        idB = event.getPointerId(1)
                        mode = MODE_DUAL
                    }
                    baselineDual(event)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == MODE_DUAL && event.pointerCount >= 2) {
                    val i0 = event.findPointerIndex(idA)
                    val i1 = event.findPointerIndex(idB)
                    if (i0 < 0 || i1 < 0) {
                        dropToSingle()
                    } else {
                        handleDualMove(event, i0, i1)
                    }
                } else if (mode == MODE_SINGLE && event.pointerCount == 1) {
                    if (pendingSync) {
                        dX = event.rawX
                        dY = event.rawY
                        pendingSync = false
                    } else {
                        val dx = event.rawX - dX
                        val dy = event.rawY - dY
                        dX = event.rawX
                        dY = event.rawY
                        panBy(dx, dy)
                        pushView()
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val lifted = event.getPointerId(event.actionIndex)
                if (mode == MODE_DUAL && (lifted == idA || lifted == idB)) {
                    idA = if (lifted == idA) idB else idA
                    dropToSingle()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = MODE_NONE
                idA = -1
                idB = -1
                pendingSync = false
            }
        }
        return true
    }

    // --------------------------------------------------------------- surface

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (nativeHandle == 0L) {
            val bg = normal(colorBg)
            nativeHandle = nativeInit(
                context.assets,
                floatArrayOf(
                    bg[0], bg[1], bg[2], bg[3],
                    labelText[0], labelText[1], labelText[2], labelText[3]
                )
            )
        }
        surfaceAvailable = true
        nativeSurfaceCreated(nativeHandle, holder.surface)
        nativeResize(nativeHandle, width.coerceAtLeast(1), height.coerceAtLeast(1))
        nativeSetDebugOverlay(nativeHandle, debugOverlayEnabled, debugOverlayTopMargin)
        if (pendingData) {
            pendingData = false
            pushData(
                pendingBranchColors, pendingBranchPts, pendingBranchCounts,
                pendingTransPts, pendingLabelTexts, pendingLabelPos,
                pendingLabelColors
            )
        } else {
            pushView()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        // Preserve the map point under the screen center across resizes
        // (panel resize, rotation) instead of jumping to the default view.
        // Before the first centering there is nothing to preserve yet.
        val cx = size.w / 2f - scrollXf
        val cy = size.h / 2f - scrollYf
        size.w = w
        size.h = h
        if (centeredOnce) {
            scrollXf = size.w / 2f - cx
            scrollYf = size.h / 2f - cy
        }
        if (nativeHandle != 0L && surfaceAvailable) {
            nativeResize(nativeHandle, w.coerceAtLeast(1), h.coerceAtLeast(1))
            pushView()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceAvailable = false
        val handle = nativeHandle
        if (handle != 0L) {
            nativeSurfaceDestroyed(handle)
        }
    }

    override fun onDetachedFromWindow() {
        // Clear the handle first: a late surfaceDestroyed must never touch
        // the deleted native renderer (use-after-free native crash).
        val handle = nativeHandle
        nativeHandle = 0L
        surfaceAvailable = false
        if (handle != 0L) {
            nativeDestroy(handle)
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
        labelTexts: Array<String>, labelPos: FloatArray, labelColors: IntArray
    )
    private external fun nativeSetView(
        handle: Long, scrollX: Float, scrollY: Float, scale: Float, angle: Float,
        pivotX: Float, pivotY: Float,
        textR: Float, textG: Float, textB: Float, textA: Float
    )
    private external fun nativeRender(handle: Long): Boolean
    private external fun nativeSetDebugOverlay(handle: Long, enabled: Boolean, topMarginPx: Int)

    companion object {
        init {
            System.loadLibrary("metroview")
        }

        const val MODE_NONE = 0
        const val MODE_SINGLE = 1
        const val MODE_DUAL = 2

        // Absolute safety caps when the user disables zoom limits
        // (the map must never hit 0 or infinity).
        const val ZOOM_ABS_MIN = 0.1f
        const val ZOOM_ABS_MAX = 32.0f
    }
}
