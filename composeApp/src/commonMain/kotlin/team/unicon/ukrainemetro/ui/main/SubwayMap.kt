package team.unicon.ukrainemetro.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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

@Composable
fun SubwayMap(
    elements: List<Element>,
    modifier: Modifier = Modifier,
    onStationClick: (Point) -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val renderScale = 2.4f
    val scaleLimits = 0.6f..1.8f // User updated scale limits
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Calculate stationRadius in Px here to make it accessible to pointerInput
    val stationRadiusPx = with(LocalDensity.current) { 5.dp.toPx() }
    // For easier tapping, you might want a slightly larger tap area than visual radius
    val stationTapRadiusPx = stationRadiusPx * 1.5f


    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(scaleLimits)
        offset += offsetChange
        scale = newScale
    }

    val branchPath = remember { Path() }

    Canvas(modifier = modifier
        .fillMaxSize()
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offset.x,
            translationY = offset.y,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        .transformable(state = transformState)
        .pointerInput(elements, scale, offset, renderScale, stationRadiusPx, stationTapRadiusPx, onStationClick) {
            detectTapGestures(
                onTap = { tapViewOffset ->
                    // 1. Transform tap coordinates from View Space to Canvas Content Space
                    // This reverses the scaling and translation applied by graphicsLayer
                    val tapInCanvasContentX = (tapViewOffset.x - offset.x) / scale
                    val tapInCanvasContentY = (tapViewOffset.y - offset.y) / scale
                    val tapInCanvasContentCoords = Offset(tapInCanvasContentX, tapInCanvasContentY)

                    var stationClicked: Point? = null

                    // 2. Iterate through elements to find a clicked station
                    for (element in elements) {
                        if (element is BranchElement) {
                            for (point in element.points) {
                                if (point.name != null) { // Check only actual stations
                                    val stationDrawCenter = point.pos.toOffset() * renderScale
                                    val dx = tapInCanvasContentCoords.x - stationDrawCenter.x
                                    val dy = tapInCanvasContentCoords.y - stationDrawCenter.y
                                    val distanceSquared = dx * dx + dy * dy

                                    if (distanceSquared <= stationTapRadiusPx * stationTapRadiusPx) {
                                        stationClicked = point
                                        break
                                    }
                                }
                            }
                        }
                        if (stationClicked != null) {
                            break
                        }
                    }

                    // 3. If a station was clicked, invoke the callback
                    stationClicked?.let {
                        onStationClick(it)
                    }
                }
            )
        }
    ) {
        // Drawing logic is inside the DrawScope, stationRadiusPx is already calculated
        val branchStrokeWidth = 3.dp.toPx()
        val transferStrokeWidth = 2.dp.toPx()

        elements.sortedWith(compareBy({
            when (it) {
                is BranchElement -> 0
                is TransElement -> 1
                else -> Int.MAX_VALUE
            }
        })).forEach { element ->
            when (element) {
                is BranchElement -> {
                    branchPath.reset()
                    val scaledPoints = element.points.map { it.pos.toOffset() * renderScale }

                    if (scaledPoints.size >= 2) {
                        branchPath.moveTo(scaledPoints[0].x, scaledPoints[0].y)
                        if (scaledPoints.size == 2) {
                            branchPath.lineTo(scaledPoints[1].x, scaledPoints[1].y)
                        } else {
                            val smoothness = 0.2f
                            for (i in 0 until scaledPoints.size - 1) {
                                val p_i = scaledPoints[i]
                                val p_i_plus_1 = scaledPoints[i + 1]
                                val p_i_minus_1 = if (i > 0) scaledPoints[i - 1] else p_i
                                val p_i_plus_2 = if (i < scaledPoints.size - 2) scaledPoints[i + 2] else p_i_plus_1
                                val cp1X = p_i.x + (p_i_plus_1.x - p_i_minus_1.x) * smoothness
                                val cp1Y = p_i.y + (p_i_plus_1.y - p_i_minus_1.y) * smoothness
                                val cp2X = p_i_plus_1.x - (p_i_plus_2.x - p_i.x) * smoothness
                                val cp2Y = p_i_plus_1.y - (p_i_plus_2.y - p_i.y) * smoothness
                                branchPath.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p_i_plus_1.x, p_i_plus_1.y)
                            }
                        }
                        drawPath(
                            path = branchPath,
                            color = element.color,
                            style = Stroke(width = branchStrokeWidth, cap = StrokeCap.Round)
                        )
                    } else if (scaledPoints.size == 1) {
                        drawCircle(
                            color = element.color,
                            radius = branchStrokeWidth / 2,
                            center = scaledPoints[0]
                        )
                    }

                    element.points.forEach { point ->
                        val stationCenter = point.pos.toOffset() * renderScale
                        point.name?.let { name ->
                            // Use stationRadiusPx (defined outside DrawScope) for drawing circles
                            drawCircle(
                                color = primaryColor,
                                radius = stationRadiusPx, // Use the pre-calculated Px value
                                center = stationCenter
                            )
                            drawCircle(
                                color = primaryContainerColor,
                                radius = stationRadiusPx, // Use the pre-calculated Px value
                                center = stationCenter,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            val measuredText = textMeasurer.measure(
                                text = name.resolve(),
                                style = TextStyle(fontSize = 10.sp, color = onBackgroundColor) // Used onBackgroundColor
                            )
                            drawText(
                                textLayoutResult = measuredText,
                                topLeft = Offset(
                                    x = stationCenter.x + stationRadiusPx + 2.dp.toPx(),
                                    y = stationCenter.y - measuredText.size.height / 2
                                ),
                                color = onBackgroundColor
                            )
                        }
                    }
                }
                is TransElement -> {
                    val startOffset = element.from.toOffset() * renderScale
                    val endOffset = element.to.toOffset() * renderScale
                    drawLine(
                        color = primaryColor,
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = transferStrokeWidth,
                        cap = StrokeCap.Butt,
                    )
                    drawCircle(
                        color = onPrimaryColor,
                        radius = 2.dp.toPx(),
                        center = startOffset
                    )
                    drawCircle(
                        color = onPrimaryColor,
                        radius = 2.dp.toPx(),
                        center = endOffset
                    )
                }
            }
        }
    }
}