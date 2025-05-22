package team.unicon.ukrainemetro.ui.main

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.unicon.ukrainemetro.entities.elements.BranchElement
import team.unicon.ukrainemetro.entities.elements.Element
import team.unicon.ukrainemetro.entities.elements.TransElement
import team.unicon.ukrainemetro.entities.toOffset

@Composable
fun SubwayMap(elements: List<Element>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val renderScale = 2.4f
    val scaleLimits = 1f..2f
    var scale by remember { mutableStateOf(1f) }
    scale = scale.coerceIn(scaleLimits)
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }

    Canvas(modifier = modifier
        .fillMaxSize()
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offset.x,
            translationY = offset.y
        )
        .transformable(state)
    ) {
        val stationRadius = 5.dp.toPx()
        val branchStrokeWidth = 3.dp.toPx()
        val transferStrokeWidth = 2.dp.toPx()

        // Loop through all elements and draw them
        // Sort by type
        elements.sortedWith(compareBy({
            when(it) {
                is BranchElement -> 0
                is TransElement -> 1
                else -> Int.MAX_VALUE
            }
        })).forEach { element ->
            when (element) {
                is BranchElement -> {
                    // Draw the branch lines
                    for (i in 0 until element.points.size - 1) {
                        val startPoint = element.points[i].pos.toOffset() * renderScale
                        val endPoint = element.points[i + 1].pos.toOffset() * renderScale
                        drawLine(
                            color = element.color,
                            start = startPoint,
                            end = endPoint,
                            strokeWidth = branchStrokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }

                    // Draw the stations and their names
                    element.points.forEach { point ->
                        val stationCenter = point.pos.toOffset() * renderScale

                        // Draw station circle
                        drawCircle(
                            color = primaryColor,
                            radius = stationRadius,
                            center = stationCenter
                        )
                        // Add a border to stations for better visibility
                        drawCircle(
                            color = primaryContainerColor,
                            radius = stationRadius,
                            center = stationCenter,
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Draw station name
                        point.name?.let { name ->
                            val measuredText = textMeasurer.measure(
                                text = name.resolve(), // Assuming 'default' property for the string
                                style = TextStyle(fontSize = 10.sp, color = Color.Black)
                            )
                            // Position text slightly below and to the right of the station
                            drawText(
                                textLayoutResult = measuredText,
                                topLeft = Offset(
                                    x = stationCenter.x + stationRadius + 2.dp.toPx(),
                                    y = stationCenter.y - measuredText.size.height / 2
                                ),
                                color = onBackgroundColor
                            )
                        }
                    }
                }

                is TransElement -> {
                    drawLine(
                        color = primaryColor,
                        start = element.from.toOffset() * renderScale,
                        end = element.to.toOffset() * renderScale,
                        strokeWidth = transferStrokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    )
                    drawCircle(
                        color = onPrimaryColor,
                        radius = 2.dp.toPx(),
                        center = element.from.toOffset() * renderScale
                    )
                    drawCircle(
                        color = onPrimaryColor,
                        radius = 2.dp.toPx(),
                        center = element.to.toOffset() * renderScale
                    )
                }
            }
        }
    }
}