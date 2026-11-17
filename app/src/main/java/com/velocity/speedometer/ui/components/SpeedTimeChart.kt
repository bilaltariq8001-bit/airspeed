package com.velocity.speedometer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.speedometer.R
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.TrackPoint
import com.velocity.speedometer.domain.util.SpeedConverter
import com.velocity.speedometer.ui.theme.InterFontFamily
import com.velocity.speedometer.ui.theme.LocalVelocityColors
import com.velocity.speedometer.ui.theme.VelocityCyan
import kotlin.math.roundToInt

@Composable
fun SpeedTimeChart(
    points: List<TrackPoint>,
    unit: SpeedUnit,
    unitLabel: String,
    modifier: Modifier = Modifier
) {
    val velocityColors = LocalVelocityColors.current
    val textMeasurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    val convertedPoints = remember(points, unit) {
        if (points.isEmpty()) emptyList()
        else {
            val startTs = points.first().timestamp
            points.map { pt ->
                (pt.timestamp - startTs).coerceAtLeast(0L) to
                    SpeedConverter.fromMps(pt.speedMps, unit)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, velocityColors.divider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_speed_chart),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (convertedPoints.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.detail_no_points),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxSpeed = (convertedPoints.maxOf { it.second }).coerceAtLeast(10f)
                val maxTimeMs = (convertedPoints.last().first).coerceAtLeast(1L)
                val labelStyle = TextStyle(
                    fontFamily = InterFontFamily,
                    fontSize = 10.sp,
                    color = axisColor
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(top = 12.dp)
                ) {
                    val leftPadding = 38.dp.toPx()
                    val bottomPadding = 22.dp.toPx()
                    val topPadding = 8.dp.toPx()
                    val rightPadding = 8.dp.toPx()

                    val plotWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(10f)
                    val plotHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(10f)

                    // Horizontal grid lines (0%, 50%, 100%)
                    for (step in 0..2) {
                        val fraction = step / 2f
                        val y = topPadding + plotHeight * (1f - fraction)
                        drawLine(
                            color = velocityColors.divider.copy(alpha = 0.7f),
                            start = Offset(leftPadding, y),
                            end = Offset(leftPadding + plotWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        val speedLabel = (maxSpeed * fraction).roundToInt().toString()
                        val measured = textMeasurer.measure(speedLabel, labelStyle)
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                x = (leftPadding - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                                y = y - measured.size.height / 2f
                            )
                        )
                    }

                    // Build line and gradient fill paths
                    val linePath = Path()
                    val fillPath = Path()

                    convertedPoints.forEachIndexed { index, (elapsedMs, speed) ->
                        val x = leftPadding + (elapsedMs.toFloat() / maxTimeMs.toFloat()) * plotWidth
                        val y = topPadding + plotHeight * (1f - (speed / maxSpeed).coerceIn(0f, 1f))
                        if (index == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, topPadding + plotHeight)
                            fillPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                        if (index == convertedPoints.lastIndex) {
                            fillPath.lineTo(x, topPadding + plotHeight)
                            fillPath.close()
                        }
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                VelocityCyan.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            startY = topPadding,
                            endY = topPadding + plotHeight
                        )
                    )

                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Start and end time labels
                    val startMeasured = textMeasurer.measure("0:00", labelStyle)
                    drawText(
                        textLayoutResult = startMeasured,
                        topLeft = Offset(leftPadding, topPadding + plotHeight + 6.dp.toPx())
                    )
                    val endText = "${SpeedConverter.formatDuration(maxTimeMs)} ($unitLabel)"
                    val endMeasured = textMeasurer.measure(endText, labelStyle)
                    drawText(
                        textLayoutResult = endMeasured,
                        topLeft = Offset(
                            x = (leftPadding + plotWidth - endMeasured.size.width).coerceAtLeast(leftPadding),
                            y = topPadding + plotHeight + 6.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}
