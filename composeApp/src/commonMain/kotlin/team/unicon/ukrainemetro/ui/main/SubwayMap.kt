package team.unicon.ukrainemetro.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.unicon.ukrainemetro.entities.Point
import team.unicon.ukrainemetro.entities.elements.BranchElement
import team.unicon.ukrainemetro.entities.elements.Element
import team.unicon.ukrainemetro.entities.elements.TransElement
import team.unicon.ukrainemetro.entities.toOffset
import kotlin.math.pow

@Composable
fun SubwayMap(
    elements: List<Element>,
    modifier: Modifier = Modifier,
    selectedPoint: Point?,
    onStationClick: (Point) -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    // This is a base scale factor for your map's content.
    // All coordinates will be multiplied by this before dynamic zoom.
    val renderScale = 2.4f
    val scaleLimits = 0.6f..1.8f // Limits for the dynamic zoom

    // `scale` here is the dynamic zoom factor applied by user gestures.
    var scale by remember { mutableStateOf(1f) }
    // `offset` here is the pan offset in screen coordinates.
    var offset by remember { mutableStateOf(Offset.Zero) }

    val stationRadiusPx = with(LocalDensity.current) { 5.dp.toPx() }
    // Tap radius is slightly larger for easier tapping.
    val stationTapRadiusPx = stationRadiusPx * 1.5f

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        // Update dynamic scale
        val newScale = (scale * zoomChange).coerceIn(scaleLimits)
        // The offsetChange is in screen coordinates. We update our screen pan offset.
        // The amount the content appears to move is affected by zoomChange.
        // If zoom changes, the point under the gesture center should ideally stay the same.
        // For simplicity, many implementations adjust offset directly or adjust it
        // to keep the gesture center fixed. `offset += offsetChange` is a common approach
        // where `offsetChange` is the pan delta in screen space.
        offset += offsetChange
        scale = newScale
    }

    val branchPath = remember { Path() }

    // For debugging tap locations
    var lastTapOffsetInOriginalCoords by remember { mutableStateOf(Offset.Zero) }

    /**
     * Transforms an original map coordinate to a screen coordinate.
     * @param currentDynamicZoom The current dynamic zoom factor (from user interaction).
     * @receiver The original Offset (e.g., from `point.pos.toOffset()`).
     * @return The Offset transformed to screen coordinates.
     */
    fun Offset.transformToScreen(currentDynamicZoom: Float): Offset {
        // `this` refers to the original map coordinate (P_original)
        // `renderScale` is the base fixed scale
        // `currentDynamicZoom` is the user-controlled zoom (`scale` from state)
        // `offset` is the pan offset from state (`panOffset`)

        val totalEffectiveScale = renderScale * currentDynamicZoom
        val scaledPosition = this * totalEffectiveScale // (P_original * S_base * S_dynamic)
        return scaledPosition + offset // + P_pan_offset
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(
                elements, // Keying pointerInput to these ensures re-evaluation if they change
                scale,
                offset,
                // renderScale, // Constant, but good practice if it could change
                // stationRadiusPx, // Constant after composition, but good practice
                stationTapRadiusPx,
                onStationClick
            ) {
                detectTapGestures { tapViewOffset -> // tapViewOffset is in screen coordinates
                    // Convert tap screen coordinates to original map coordinates
                    val tapInOriginalCoords = (tapViewOffset - offset) / (scale * renderScale)
                    lastTapOffsetInOriginalCoords = tapInOriginalCoords // Store for debug drawing

                    var stationClicked: Point? = null
                    for (element in elements) {
                        if (element is BranchElement) {
                            for (point in element.points) {
                                if (point.name != null) {
                                    val stationCenterOriginal = point.pos.toOffset()
                                    // Calculate distance in original coordinate space
                                    val dx = tapInOriginalCoords.x - stationCenterOriginal.x
                                    val dy = tapInOriginalCoords.y - stationCenterOriginal.y
                                    val distSqInOriginal = dx * dx + dy * dy

                                    // Convert screen tap radius to original coordinate space radius
                                    val tapRadiusInOriginal = stationTapRadiusPx / (scale * renderScale)
                                    val tapRadiusSqInOriginal = tapRadiusInOriginal.pow(2)

                                    if (distSqInOriginal <= tapRadiusSqInOriginal) {
                                        stationClicked = point
                                        break
                                    }
                                }
                            }
                        }
                        if (stationClicked != null) break
                    }
                    stationClicked?.let(onStationClick)
                }
            }
            .transformable(state = transformState)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Debug draw: shows the tap location in original coords, transformed to screen
            // This should align with where you tapped on the map content.
//            drawCircle(
//                color = Color.Red,
//                radius = 12f, // Screen pixels
//                center = lastTapOffsetInOriginalCoords.transformToScreen(scale)
//            )

            val branchStrokeWidth = 3.dp.toPx() // Stroke width in screen pixels
            val transferStrokeWidth = 2.dp.toPx() // Stroke width in screen pixels

            elements.sortedWith(compareBy {
                when (it) {
                    is BranchElement -> 0
                    is TransElement -> 1
                    else -> Int.MAX_VALUE // Should not happen if you only have these two types
                }
            }).forEach { element ->
                when (element) {
                    is BranchElement -> {
                        branchPath.reset()
                        // Transform all points of the branch to screen coordinates
                        val scaledPoints = element.points.map {
                            it.pos.toOffset().transformToScreen(scale)
                        }

                        if (scaledPoints.size >= 2) {
                            branchPath.moveTo(scaledPoints[0].x, scaledPoints[0].y)
                            if (scaledPoints.size == 2) { // Straight line for two points
                                branchPath.lineTo(scaledPoints[1].x, scaledPoints[1].y)
                            } else { // Cubic Bezier for more than two points for smoothness
                                val smoothness = 0.2f // Adjust for desired curve smoothness
                                for (i in 0 until scaledPoints.size - 1) {
                                    val p0 = scaledPoints[i]
                                    val p1 = scaledPoints[i + 1]
                                    // Control points calculation for Catmull-Rom like curve
                                    val pM1 = if (i > 0) scaledPoints[i - 1] else p0 // Previous point or p0 if first segment
                                    val p2 = if (i < scaledPoints.size - 2) scaledPoints[i + 2] else p1 // Next point or p1 if last segment

                                    // First control point (for p0 to p1 segment)
                                    val cp1 = Offset(
                                        p0.x + (p1.x - pM1.x) * smoothness,
                                        p0.y + (p1.y - pM1.y) * smoothness
                                    )
                                    // Second control point (for p0 to p1 segment)
                                    val cp2 = Offset(
                                        p1.x - (p2.x - p0.x) * smoothness,
                                        p1.y - (p2.y - p0.y) * smoothness
                                    )
                                    branchPath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                                }
                            }
                            drawPath(
                                path = branchPath,
                                color = element.color,
                                style = Stroke(width = branchStrokeWidth, cap = StrokeCap.Round)
                            )
                        } else if (scaledPoints.size == 1) { // Draw a circle if only one point
                            drawCircle(
                                color = element.color,
                                radius = branchStrokeWidth / 2, // Use half stroke width as radius
                                center = scaledPoints[0]
                            )
                        }

                        // Draw stations and their names
                        element.points.forEach { point ->
                            val centerOnScreen = point.pos.toOffset().transformToScreen(scale)
                            point.name?.let { name ->
                                val isSelected = point == selectedPoint
                                // Draw station circle
                                drawCircle(
                                    color = primaryColor,
                                    radius = if(isSelected) stationRadiusPx * 1.5f else stationRadiusPx, // Screen pixels
                                    center = centerOnScreen
                                )
                                // Draw station border
                                drawCircle(
                                    color = primaryContainerColor,
                                    radius = stationRadiusPx, // Screen pixels
                                    center = centerOnScreen,
                                    style = Stroke(width = 1.dp.toPx()) // Screen pixels
                                )
                                // Measure and draw station name
                                val textLayoutResult: TextLayoutResult = textMeasurer.measure(
                                    text = name.resolve(), // Resolve StringResource if used
                                    style = TextStyle(fontSize = 10.sp, color = onBackgroundColor)
                                )
                                drawText(
                                    textLayoutResult = textLayoutResult,
                                    topLeft = Offset(
                                        centerOnScreen.x + stationRadiusPx + 2.dp.toPx(), // Position text to the right of station
                                        centerOnScreen.y - textLayoutResult.size.height / 2 // Center text vertically
                                    ),
                                    color = onBackgroundColor // Use onBackgroundColor for text
                                )
                            }
                        }
                    }

                    is TransElement -> {
                        // Transform transfer line points to screen coordinates
                        val fromOnScreen = element.from.toOffset().transformToScreen(scale)
                        val toOnScreen = element.to.toOffset().transformToScreen(scale)
                        drawLine(
                            color = primaryColor,
                            start = fromOnScreen,
                            end = toOnScreen,
                            strokeWidth = transferStrokeWidth // Screen pixels
                        )
                        // Draw small circles at the ends of transfer lines
                        drawCircle(
                            color = onPrimaryColor,
                            radius = 2.dp.toPx(), // Screen pixels
                            center = fromOnScreen
                        )
                        drawCircle(
                            color = onPrimaryColor,
                            radius = 2.dp.toPx(), // Screen pixels
                            center = toOnScreen
                        )
                    }
                }
            }
        }
    }
}