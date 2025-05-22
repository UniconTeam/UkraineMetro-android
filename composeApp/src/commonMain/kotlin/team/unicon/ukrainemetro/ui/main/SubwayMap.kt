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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary // Used for transfer endpoint circles

    val renderScale = 2.4f
    val scaleLimits = 1f..2f // Original scaleLimits from your code
    var scale by remember { mutableStateOf(1f) }
    scale = scale.coerceIn(scaleLimits) // Coerce within defined limits
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        // Coerce scale after zoomChange before applying to graphicsLayer
        scale = scale.coerceIn(scaleLimits)
        offset += offsetChange
    }

    // It's good practice to remember the Path object if the element itself is stable
    // However, if BranchElement objects are recreated often, direct creation is fine.
    // For simplicity here, creating path inside the loop if not remembered with a key.
    val branchPath = remember { Path() }

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

        elements.sortedWith(compareBy({
            when (it) {
                is BranchElement -> 0
                is TransElement -> 1
                else -> Int.MAX_VALUE
            }
        })).forEach { element ->
            when (element) {
                is BranchElement -> {
                    branchPath.reset() // Reset the path for the current branch
                    val scaledPoints = element.points.map { it.pos.toOffset() * renderScale }

                    if (scaledPoints.size >= 2) {
                        branchPath.moveTo(scaledPoints[0].x, scaledPoints[0].y)

                        if (scaledPoints.size == 2) {
                            // For only two points, draw a straight line.
                            branchPath.lineTo(scaledPoints[1].x, scaledPoints[1].y)
                        } else {
                            // More than two points, use cubic Bezier curves for a smooth line.
                            // Adjust this factor to control the "curviness".
                            // Values typically range from 0.15 to 0.3. 1/6f (~0.167f) is common for Catmull-Rom like splines.
                            val smoothness = 0.2f

                            for (i in 0 until scaledPoints.size - 1) {
                                val p_i = scaledPoints[i]            // Current point (P_i)
                                val p_i_plus_1 = scaledPoints[i+1]   // Next point (P_{i+1})

                                // Determine P_{i-1} (point before P_i)
                                // For the first segment, P_{i-1} is P_i itself.
                                val p_i_minus_1 = if (i > 0) scaledPoints[i - 1] else p_i

                                // Determine P_{i+2} (point after P_{i+1})
                                // For the last segment, P_{i+2} is P_{i+1} itself.
                                val p_i_plus_2 = if (i < scaledPoints.size - 2) scaledPoints[i + 2] else p_i_plus_1

                                // Calculate control point 1 (influences curve leaving P_i)
                                // C1 = P_i + smoothness * (P_{i+1} - P_{i-1})
                                val cp1X = p_i.x + (p_i_plus_1.x - p_i_minus_1.x) * smoothness
                                val cp1Y = p_i.y + (p_i_plus_1.y - p_i_minus_1.y) * smoothness

                                // Calculate control point 2 (influences curve approaching P_{i+1})
                                // C2 = P_{i+1} - smoothness * (P_{i+2} - P_i)
                                val cp2X = p_i_plus_1.x - (p_i_plus_2.x - p_i.x) * smoothness
                                val cp2Y = p_i_plus_1.y - (p_i_plus_2.y - p_i.y) * smoothness

                                branchPath.cubicTo(
                                    cp1X, cp1Y,
                                    cp2X, cp2Y,
                                    p_i_plus_1.x, p_i_plus_1.y
                                )
                            }
                        }
                        // Draw the constructed path for the branch
                        drawPath(
                            path = branchPath,
                            color = element.color,
                            style = Stroke(width = branchStrokeWidth, cap = StrokeCap.Round)
                        )
                    } else if (scaledPoints.size == 1) {
                        // Optional: Draw a single point if a branch has only one station
                        drawCircle(
                            color = element.color,
                            radius = branchStrokeWidth / 2, // Or stationRadius if it should look like a station
                            center = scaledPoints[0]
                        )
                    }


                    // Draw the stations and their names (existing logic)
                    element.points.forEach { point ->
                        val stationCenter = point.pos.toOffset() * renderScale
                        point.name?.let { name ->
                            drawCircle(
                                color = primaryColor,
                                radius = stationRadius,
                                center = stationCenter
                            )
                            drawCircle(
                                color = primaryContainerColor,
                                radius = stationRadius,
                                center = stationCenter,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            val measuredText = textMeasurer.measure(
                                text = name.resolve(),
                                style = TextStyle(fontSize = 10.sp, color = Color.Black) // Consider using onBackgroundColor
                            )
                            drawText(
                                textLayoutResult = measuredText,
                                topLeft = Offset(
                                    x = stationCenter.x + stationRadius + 2.dp.toPx(),
                                    y = stationCenter.y - measuredText.size.height / 2
                                ),
                                color = onBackgroundColor // Use theme color
                            )
                        }
                    }
                }

                is TransElement -> {
                    // Transfer lines remain straight
                    val startOffset = element.from.toOffset() * renderScale
                    val endOffset = element.to.toOffset() * renderScale
                    drawLine(
                        color = primaryColor, // Or a specific transfer color
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = transferStrokeWidth,
                        cap = StrokeCap.Butt,
                    )
                    // Circles at the ends of transfer lines
                    drawCircle(
                        color = onPrimaryColor, // Or a different color to distinguish transfer points
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
