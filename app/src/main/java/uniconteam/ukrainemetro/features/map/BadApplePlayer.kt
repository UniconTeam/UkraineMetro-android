package uniconteam.ukrainemetro.features.map

import android.content.Context
import android.content.res.AssetManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.json.JSONObject
import uniconteam.ukrainemetro.mapview.entities.Point
import uniconteam.ukrainemetro.mapview.entities.Vector
import uniconteam.ukrainemetro.mapview.entities.elements.BranchElement
import uniconteam.ukrainemetro.mapview.entities.elements.Element
import uniconteam.ukrainemetro.mapview.entities.elements.MarkerElement
import uniconteam.ukrainemetro.mapview.entities.elements.TransElement
import uniconteam.ukrainemetro.mapview.view.VulkanMetroView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.hypot

/**
 * Plays Bad Apple as lineart through [VulkanMetroView].
 *
 * Frames come from `assets/badapple/frames.bin` (see `tools/bad_apple.py`):
 * each frame becomes a mini metro map — contours ride colored lines,
 * station dots sit along them, and near-touching endpoints are joined
 * with interchange links in the transfer color.
 *
 * Audio (`assets/badapple/audio.m4a`) autoplays with [MediaPlayer] at 40%
 * volume. No play/stop buttons by design: playback starts with the city
 * and stops when leaving it.
 */
class BadApplePlayer(
    private val context: Context,
    private val view: VulkanMetroView,
) {
    companion object {
        private const val VOLUME = 0.4f // 40% of max, no UI to change it
        private const val ASSET_BIN = "badapple/frames.bin"
        private const val ASSET_AUDIO = "badapple/audio.m4a"

        /** Map units per video pixel (Point applies the x2 big-map hack). */
        private const val SCALE = 2 // 256x192 -> 1024x768 map units after x2

        /** Metro palette cycled across a frame's contours, biggest first. */
        private val PALETTE = intArrayOf(
            0xFFE57373.toInt(), // red line
            0xFF2196F3.toInt(), // blue line
            0xFF81C784.toInt(), // green line
        )

        /** Arc-length spacing between station dots, in video px. */
        private const val STATION_EVERY_PX = 14.0

        /** Max endpoint gap closed by an interchange link, in video px. */
        private const val LINK_PX = 9.0
        private const val MAX_LINKS = 40

        /** Fixed pool of station names, one label slot per name per frame. */
        private val NAMES = arrayOf(
            "Apple", "Shadow", "Mirror", "Spirit",
            "Night", "Dream", "Circle", "Star",
            "Moon", "Wind", "Leaf", "River",
        )

        /** Parking spot for unused label slots (map units, far offscreen). */
        private const val OFFSCREEN = -1e9f

        /** Centroid match radius between frames, in video px. */
        private const val MATCH_PX = 30.0
        private const val MAX_TRACKS = 12

        private val MAGIC = byteArrayOf(0x42, 0x41, 0x50, 0x4C) // "BAPL"
    }

    private var frameOffsets = IntArray(0)
    private var frameData: ByteBuffer? = null
    private var frameCount = 0
    private var fps = 30
    private var mapW = 0f
    private var mapH = 0f
    private var fitted = false

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var startUptime = 0L
    private var running = false

    /** Contour tracks from the previous frame: centroid + palette slot. */
    private var tracks = mutableListOf<Triple<Double, Double, Int>>()

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            // Follow the content: recenter on every frame unless the user
            // is mid-gesture (then their fingers win until release).
            view.centerLocked = view.isUserInteracting
            pushCurrentFrame()
            fitOnce()
            // Schedule from the wall clock so a slow frame doesn't drift us.
            val elapsed = SystemClock.uptimeMillis() - startUptime
            val next = startUptime + ((elapsed * fps / 1000L) + 1L) * 1000L / fps
            handler.postAtTime(this, next)
        }
    }

    /** Fill the screen with the video and center it (once, when laid out). */
    private fun fitOnce() {
        if (!fitted && mapW > 0f && view.width > 0 && view.height > 0) {
            view.zoomToFit(mapW, mapH)
            fitted = true
        }
    }

    fun isAvailable(assets: AssetManager = context.assets): Boolean = try {
        // NOTE: openFd() only works for STORED (uncompressed) assets, and
        // frames.bin ships compressed — plain open() works either way.
        assets.open(ASSET_BIN).close()
        assets.open(ASSET_AUDIO).close()
        true
    } catch (_: Exception) {
        false
    }

    fun start() {
        if (running) return
        if (!loadFrames()) return
        // Glyph atlas for the fixed label pool, built once: every later
        // frame only moves anchors, so labels cost no re-rasterization.
        view.setFixedLabels(NAMES)
        running = true
        fitted = false
        tracks.clear()
        // First frame centers via updateData; tick() then recenters on
        // every frame's content (unless the user is interacting).
        // Afterwards the camera is the user's.
        view.centerLocked = false
        pushCurrentFrame()
        fitOnce()
        startAudio()
        startUptime = SystemClock.uptimeMillis()
        handler.post(tick)
    }

    fun stop() {
        running = false
        fitted = false
        handler.removeCallbacks(tick)
        view.centerLocked = false
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // ------------------------------------------------------------- internals

    private fun loadFrames(): Boolean {
        if (frameCount > 0) return true
        try {
            // Frame dimensions + rate come from meta.json (tiny); the .bin
            // header is validated against it below.
            context.assets.open("badapple/meta.json").use { input ->
                val meta = JSONObject(input.readBytes().toString(Charsets.UTF_8))
                val w = meta.getInt("width")
                val h = meta.getInt("height")
                fps = meta.getInt("fps").coerceIn(1, 60)
                mapW = (w * SCALE * 2).toFloat()
                mapH = (h * SCALE * 2).toFloat()
            }
            context.assets.open(ASSET_BIN).use { input ->
                val bytes = input.readBytes()
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                if (bytes.size < 14) return false
                val magic = ByteArray(4).also { buf.get(it) }
                if (!magic.contentEquals(MAGIC)) return false
                val version = buf.get().toInt() and 0xFF
                if (version != 2) return false // this build only plays v2 polys
                buf.short // width (dims come from meta.json)
                buf.short // height
                buf.get() // fps
                frameCount = buf.int
                if (frameCount <= 0 || bytes.size < 14 + frameCount * 4) return false
                frameOffsets = IntArray(frameCount) { buf.int }
                frameData = buf
                return true
            }
        } catch (_: Exception) {
            return false
        }
    }

    private fun pushCurrentFrame() {
        val data = frameData ?: return
        if (frameCount == 0) return
        val elapsed = SystemClock.uptimeMillis() - startUptime
        val base = if (startUptime == 0L) 0 else (elapsed * fps / 1000L)
        val index = (((base % frameCount) + frameCount) % frameCount).toInt()
        val plan = decodeFrame(data, index)
        view.updateFrameData(plan.branches, plan.markers, plan.transfers, plan.labelPos)
    }

    private class FramePlan(
        val branches: List<BranchElement>,
        val markers: List<MarkerElement>,
        val transfers: List<TransElement>,
        /** One anchor per fixed label slot ([NAMES] order), offscreen-padded. */
        val labelPos: FloatArray,
    )

    private fun decodeFrame(data: ByteBuffer, index: Int): FramePlan {
        val dup = data.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        dup.position(frameOffsets[index])
        val polyCount = dup.short.toInt() and 0xFFFF
        if (polyCount == 0) {
            return FramePlan(emptyList(), emptyList(), emptyList(), blankAnchors())
        }

        // Raw contours in video pixels.
        val raw = ArrayList<IntArray>(polyCount)
        repeat(polyCount) {
            val n = dup.short.toInt() and 0xFFFF
            if (n == 0) {
                raw.add(IntArray(0))
                return@repeat
            }
            val arr = IntArray(n * 2)
            for (i in 0 until n) {
                arr[2 * i] = dup.get().toInt() and 0xFF
                arr[2 * i + 1] = dup.get().toInt() and 0xFF
            }
            raw.add(arr)
        }

        // Biggest contour first; colors stick to contours across frames
        // via centroid tracking below, so lines don't strobe.
        val order = raw.indices.sortedByDescending { raw[it].size }
        val slots = trackSlots(raw)
        val branches = ArrayList<BranchElement>(polyCount)
        val markers = ArrayList<MarkerElement>()
        val transfers = ArrayList<TransElement>()
        val ends = ArrayList<Triple<Int, Int, Int>>() // x, y, polyIdx
        val labelPos = blankAnchors()
        var labelsPlaced = 0
        order.forEachIndexed { _, pi ->
            val arr = raw[pi]
            if (arr.isEmpty()) return@forEachIndexed
            val color = PALETTE[slots[pi] % PALETTE.size]
            val pts = ArrayList<Point>(arr.size / 2)
            for (i in arr.indices step 2) {
                pts.add(Point(Vector(arr[i] * SCALE, arr[i + 1] * SCALE), null))
            }
            // No point names (name = null): the native renderer smooths each
            // polyline with Catmull-Rom, so curves stay curves.
            branches.add(BranchElement(pts, color))
            // Stations: a marker dot every few px of arc length. The first
            // dots of the biggest contours feed the fixed label slots, which
            // the renderer draws with leader arrows.
            var firstStation = true
            var acc = 0.0
            for (i in arr.indices step 2) {
                if (i > 0) {
                    val dx = (arr[i] - arr[i - 2]).toDouble()
                    val dy = (arr[i + 1] - arr[i - 1]).toDouble()
                    acc += hypot(dx, dy)
                    if (acc < STATION_EVERY_PX) continue
                    acc = 0.0
                }
                if (firstStation && labelsPlaced < NAMES.size) {
                    // Map units: Point/MarkerElement double coords (big-map
                    // hack), so double the anchor to match.
                    labelPos[2 * labelsPlaced] = (arr[i] * SCALE * 2).toFloat()
                    labelPos[2 * labelsPlaced + 1] = (arr[i + 1] * SCALE * 2).toFloat()
                    labelsPlaced++
                }
                firstStation = false
                markers.add(MarkerElement(Vector(arr[i] * SCALE, arr[i + 1] * SCALE), color))
            }
            ends.add(Triple(arr[0], arr[1], pi))
            ends.add(Triple(arr[arr.size - 2], arr[arr.size - 1], pi))
        }
        // Interchange connections: link endpoints of different contours
        // that nearly touch, drawn in the transfer color.
        var links = 0
        for (i in ends.indices) {
            if (links >= MAX_LINKS) break
            for (j in i + 1 until ends.size) {
                if (links >= MAX_LINKS) break
                val a = ends[i]
                val b = ends[j]
                if (a.third == b.third) continue
                if (hypot((a.first - b.first).toDouble(), (a.second - b.second).toDouble()) > LINK_PX) continue
                transfers.add(
                    TransElement(
                        Vector(a.first * SCALE, a.second * SCALE),
                        Vector(b.first * SCALE, b.second * SCALE),
                    )
                )
                links++
            }
        }
        return FramePlan(branches, markers, transfers, labelPos)
    }

    private fun blankAnchors(): FloatArray =
        FloatArray(NAMES.size * 2) { OFFSCREEN }

    /**
     * Palette slot per contour, stable across frames: each contour keeps
     * the slot of the nearest previous-frame contour within [MATCH_PX],
     * so lines hold their color instead of strobing with size rank.
     * New contours take the least-used free slot (or the nearest slot's
     * color when the palette is exhausted, for spatial coherence).
     */
    private fun trackSlots(raw: List<IntArray>): IntArray {
        val n = raw.size
        val cx = DoubleArray(n)
        val cy = DoubleArray(n)
        for (i in 0 until n) {
            val arr = raw[i]
            if (arr.isEmpty()) continue
            var sx = 0.0
            var sy = 0.0
            val m = arr.size / 2
            for (j in arr.indices step 2) {
                sx += arr[j]
                sy += arr[j + 1]
            }
            cx[i] = sx / m
            cy[i] = sy / m
        }
        val slots = IntArray(n) { -1 }
        val used = BooleanArray(tracks.size)
        val usage = IntArray(PALETTE.size)
        // Largest contours pick first: they matter most visually.
        val order = (0 until n).filter { raw[it].isNotEmpty() }
            .sortedByDescending { raw[it].size }
        for (i in order) {
            var best = -1
            var bestD = MATCH_PX
            for (t in tracks.indices) {
                if (used[t]) continue
                val d = hypot(cx[i] - tracks[t].first, cy[i] - tracks[t].second)
                if (d < bestD) {
                    bestD = d
                    best = t
                }
            }
            if (best >= 0) {
                used[best] = true
                slots[i] = tracks[best].third
                usage[slots[i] % PALETTE.size]++
            } else {
                // Fresh contour: least-used slot, tie-broken by rank so a
                // brand-new scene still spreads across the palette.
                var slot = 0
                for (s in 1 until PALETTE.size) {
                    if (usage[s] < usage[slot]) slot = s
                }
                slots[i] = slot
                usage[slot]++
            }
        }
        // Refresh tracks from this frame (cap: biggest contours win).
        val next = order.take(MAX_TRACKS).map { Triple(cx[it], cy[it], slots[it]) }
        tracks = next.toMutableList()
        // Anything unmatched (empty or beyond cap) borrows the nearest
        // tracked color for spatial coherence.
        for (i in 0 until n) {
            if (slots[i] >= 0) continue
            if (raw[i].isEmpty()) {
                slots[i] = 0
                continue
            }
            var best = 0
            var bestD = Double.MAX_VALUE
            for (t in tracks.indices) {
                val d = hypot(cx[i] - tracks[t].first, cy[i] - tracks[t].second)
                if (d < bestD) {
                    bestD = d
                    best = tracks[t].third
                }
            }
            slots[i] = best
        }
        return slots
    }

    private fun startAudio() {
        try {
            // openFd needs an uncompressed asset (true for .m4a, but fall
            // back to a cache copy if a build ever compresses it).
            mediaPlayer?.release()
            mediaPlayer = try {
                val fd = context.assets.openFd(ASSET_AUDIO)
                MediaPlayer().apply {
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    fd.close()
                    prepareAndStart()
                }
            } catch (_: Exception) {
                val cached = context.cacheDir.resolve("badapple_audio.m4a")
                if (!cached.exists()) {
                    context.assets.open(ASSET_AUDIO).use { input ->
                        cached.outputStream().use { input.copyTo(it) }
                    }
                }
                MediaPlayer().apply {
                    setDataSource(cached.absolutePath)
                    prepareAndStart()
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun MediaPlayer.prepareAndStart() {
        setVolume(VOLUME, VOLUME)
        isLooping = true
        prepare()
        start()
    }
}
