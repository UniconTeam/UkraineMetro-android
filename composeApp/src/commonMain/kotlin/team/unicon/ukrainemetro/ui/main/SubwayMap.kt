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
    onStationClick: (Point) -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val renderScale = 2.4f
    val scaleLimits = 0.6f..1.8f
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val stationRadiusPx = with(LocalDensity.current) { 5.dp.toPx() }
    val stationTapRadiusPx = stationRadiusPx * 1.5f

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(scaleLimits)
        offset += offsetChange
        scale = newScale
    }

    val branchPath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(elements, scale, offset, renderScale, stationRadiusPx, stationTapRadiusPx, onStationClick) {
                detectTapGestures { tapViewOffset ->
                    val tapInCanvasContentCoords = (tapViewOffset - offset) / (scale * renderScale)
                    var stationClicked: Point? = null
                    for (element in elements) {
                        if (element is BranchElement) {
                            for (point in element.points) {
                                if (point.name != null) {
                                    val stationCenter = point.pos.toOffset()
                                    val dx = tapInCanvasContentCoords.x - stationCenter.x
                                    val dy = tapInCanvasContentCoords.y - stationCenter.y
                                    val distSq = dx * dx + dy * dy
                                    if (distSq <= stationTapRadiusPx * stationTapRadiusPx / (scale * renderScale).pow(2)) {
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
            withTransform({
                scale(scale, scale)
                translate(offset.x / scale, offset.y / scale)
            }) {
                val branchStrokeWidth = 3.dp.toPx()
                val transferStrokeWidth = 2.dp.toPx()
                elements.sortedWith(compareBy {
                    when (it) {
                        is BranchElement -> 0
                        is TransElement -> 1
                        else -> Int.MAX_VALUE
                    }
                }).forEach { element ->
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
                                        val p0 = scaledPoints[i]
                                        val p1 = scaledPoints[i + 1]
                                        val pM1 = if (i > 0) scaledPoints[i - 1] else p0
                                        val p2 = if (i < scaledPoints.size - 2) scaledPoints[i + 2] else p1
                                        val cp1 = Offset(
                                            p0.x + (p1.x - pM1.x) * smoothness,
                                            p0.y + (p1.y - pM1.y) * smoothness
                                        )
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
                            } else if (scaledPoints.size == 1) {
                                drawCircle(
                                    color = element.color,
                                    radius = branchStrokeWidth / 2,
                                    center = scaledPoints[0]
                                )
                            }
                            element.points.forEach { point ->
                                val center = point.pos.toOffset() * renderScale
                                point.name?.let { name ->
                                    drawCircle(
                                        color = primaryColor,
                                        radius = stationRadiusPx,
                                        center = center
                                    )
                                    drawCircle(
                                        color = primaryContainerColor,
                                        radius = stationRadiusPx,
                                        center = center,
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                    val textLayout = textMeasurer.measure(
                                        text = name.resolve(),
                                        style = TextStyle(fontSize = 10.sp, color = onBackgroundColor)
                                    )
                                    drawText(
                                        textLayoutResult = textLayout,
                                        topLeft = Offset(
                                            center.x + stationRadiusPx + 2.dp.toPx(),
                                            center.y - textLayout.size.height / 2
                                        ),
                                        color = onBackgroundColor
                                    )
                                }
                            }
                        }
                        is TransElement -> {
                            val from = element.from.toOffset() * renderScale
                            val to = element.to.toOffset() * renderScale
                            drawLine(
                                color = primaryColor,
                                start = from,
                                end = to,
                                strokeWidth = transferStrokeWidth
                            )
                            drawCircle(color = onPrimaryColor, radius = 2.dp.toPx(), center = from)
                            drawCircle(color = onPrimaryColor, radius = 2.dp.toPx(), center = to)
                        }
                    }
                }
            }
        }
    }
}