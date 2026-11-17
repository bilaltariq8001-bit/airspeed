package com.velocity.speedometer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.speedometer.R
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.ui.theme.AnalogSpeedTextStyle
import com.velocity.speedometer.ui.theme.InterFontFamily
import com.velocity.speedometer.ui.theme.LocalVelocityColors
import com.velocity.speedometer.ui.theme.VelocityCyan
import com.velocity.speedometer.ui.theme.VelocityDanger
import com.velocity.speedometer.ui.theme.VelocitySuccess
import com.velocity.speedometer.ui.theme.VelocityViolet
import com.velocity.speedometer.ui.theme.VelocityWarning
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val GAUGE_START_ANGLE = 135f
private const val GAUGE_SWEEP_ANGLE = 270f

@Composable
fun AnalogGauge(
    speedValue: Float,
    maxTripSpeedValue: Float,
    speedLimitValue: Float?,
    unit: SpeedUnit,
    unitLabel: String,
    compassLabel: String,
    isOverLimit: Boolean,
    isWarningZone: Boolean,
    onUnitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val velocityColors = LocalVelocityColors.current
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val scaleMax = unit.scaleMax.toFloat()
    val clampedFraction = (speedValue / scaleMax).coerceIn(0f, 1f)
    val maxTripFraction = (maxTripSpeedValue / scaleMax).coerceIn(0f, 1f)
    val limitFraction = speedLimitValue?.let { (it / scaleMax).coerceIn(0f, 1f) }

    val animatedFraction by animateFloatAsState(
        targetValue = clampedFraction,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "gauge_needle_fraction"
    )

    val displayedSpeed by animateIntAsState(
        targetValue = speedValue.roundToInt().coerceAtLeast(0),
        animationSpec = tween(durationMillis = 300),
        label = "analog_speed_digits"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "over_limit_pulse")
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val activePulse = if (isOverLimit) pulseFactor else 1f

    val speedTextColor by animateColorAsState(
        targetValue = when {
            isOverLimit -> VelocityDanger.copy(alpha = activePulse.coerceIn(0.6f, 1f))
            isWarningZone -> VelocityWarning
            else -> MaterialTheme.colorScheme.onBackground
        },
        animationSpec = tween(250),
        label = "speed_text_color"
    )

    val needleColor = if (isOverLimit) VelocityDanger else MaterialTheme.colorScheme.primary
    val trackColor = velocityColors.gaugeTrack
    val hubColor = velocityColors.surfaceVariantRaised
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    val tickLabelStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = secondaryTextColor
    )

    val gaugeContentDesc = stringResource(R.string.cd_analog_gauge, displayedSpeed, unitLabel)
    val tapUnitDesc = stringResource(R.string.cd_tap_unit, unitLabel)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .semantics {
                contentDescription = gaugeContentDesc
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 14.dp.toPx()
            val glowStrokeWidthPx = 24.dp.toPx()
            val paddingPx = with(density) { 28.dp.toPx() }
            val arcDiameter = (size.minDimension - paddingPx * 2f).coerceAtLeast(10f)
            val topLeft = Offset(
                x = (size.width - arcDiameter) / 2f,
                y = (size.height - arcDiameter) / 2f
            )
            val arcSize = Size(arcDiameter, arcDiameter)
            val radius = arcDiameter / 2f
            val centerOffset = Offset(size.width / 2f, size.height / 2f)

            // 1. Background 270-degree track arc
            drawArc(
                color = trackColor,
                startAngle = GAUGE_START_ANGLE,
                sweepAngle = GAUGE_SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // 2. Speed-limit red tick band from limit to end of scale
            if (limitFraction != null && limitFraction < 1f) {
                val limitStartAngle = GAUGE_START_ANGLE + limitFraction * GAUGE_SWEEP_ANGLE
                val limitSweep = (1f - limitFraction) * GAUGE_SWEEP_ANGLE
                drawArc(
                    color = VelocityDanger.copy(alpha = 0.55f),
                    startAngle = limitStartAngle,
                    sweepAngle = limitSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Butt)
                )
            }

            // 3. Progress arc with soft outer glow and gradient (rotated so 0 deg aligns with 135 deg)
            val progressSweep = (animatedFraction * GAUGE_SWEEP_ANGLE).coerceIn(0f, GAUGE_SWEEP_ANGLE)
            if (progressSweep > 0.5f) {
                val arcBrush = if (isOverLimit) {
                    Brush.sweepGradient(
                        0f to VelocityDanger,
                        0.75f to VelocityDanger,
                        1f to VelocityDanger,
                        center = centerOffset
                    )
                } else {
                    Brush.sweepGradient(
                        0.00f to VelocitySuccess,
                        0.28f to VelocityCyan,
                        0.54f to VelocityWarning,
                        0.75f to VelocityDanger,
                        1.00f to VelocityDanger,
                        center = centerOffset
                    )
                }

                withTransform({
                    rotate(degrees = GAUGE_START_ANGLE, pivot = centerOffset)
                }) {
                    // Outer soft glow (40% alpha)
                    drawArc(
                        brush = arcBrush,
                        startAngle = 0f,
                        sweepAngle = progressSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        alpha = 0.40f * activePulse,
                        style = Stroke(width = glowStrokeWidthPx, cap = StrokeCap.Round)
                    )
                    // Main progress arc
                    drawArc(
                        brush = arcBrush,
                        startAngle = 0f,
                        sweepAngle = progressSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        alpha = activePulse,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }

            // 4. Major ticks (every 20) with numbers & Minor ticks (every 10)
            val maxInt = unit.scaleMax
            val minorStep = unit.minorTickStep
            val majorStep = unit.majorTickStep
            for (tick in 0..maxInt step minorStep) {
                val isMajor = tick % majorStep == 0
                val fraction = tick / scaleMax
                val angleDeg = GAUGE_START_ANGLE + fraction * GAUGE_SWEEP_ANGLE
                val angleRad = angleDeg * (PI / 180.0)

                val tickOuterR = radius - strokeWidthPx * 0.72f
                val tickInnerR = if (isMajor) tickOuterR - 11.dp.toPx() else tickOuterR - 6.dp.toPx()

                val start = Offset(
                    x = centerOffset.x + (tickInnerR * cos(angleRad)).toFloat(),
                    y = centerOffset.y + (tickInnerR * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    x = centerOffset.x + (tickOuterR * cos(angleRad)).toFloat(),
                    y = centerOffset.y + (tickOuterR * sin(angleRad)).toFloat()
                )

                val tickColor = if (limitFraction != null && fraction >= limitFraction) {
                    VelocityDanger.copy(alpha = 0.85f)
                } else if (isMajor) {
                    secondaryTextColor
                } else {
                    trackColor
                }

                drawLine(
                    color = tickColor,
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                if (isMajor) {
                    val labelRadius = tickInnerR - 13.dp.toPx()
                    val measured = textMeasurer.measure(
                        text = tick.toString(),
                        style = tickLabelStyle
                    )
                    val labelCenterX = centerOffset.x + (labelRadius * cos(angleRad)).toFloat()
                    val labelCenterY = centerOffset.y + (labelRadius * sin(angleRad)).toFloat()
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = labelCenterX - measured.size.width / 2f,
                            y = labelCenterY - measured.size.height / 2f
                        )
                    )
                }
            }

            // 5. Violet triangle marker at trip's max speed
            if (maxTripFraction > 0.01f) {
                val maxAngleDeg = GAUGE_START_ANGLE + maxTripFraction * GAUGE_SWEEP_ANGLE
                val maxAngleRad = maxAngleDeg * (PI / 180.0)
                val markerTipR = radius + strokeWidthPx * 0.62f
                val markerBaseR = markerTipR + 9.dp.toPx()
                val spreadRad = 0.065

                val tip = Offset(
                    x = centerOffset.x + (markerTipR * cos(maxAngleRad)).toFloat(),
                    y = centerOffset.y + (markerTipR * sin(maxAngleRad)).toFloat()
                )
                val baseLeft = Offset(
                    x = centerOffset.x + (markerBaseR * cos(maxAngleRad - spreadRad)).toFloat(),
                    y = centerOffset.y + (markerBaseR * sin(maxAngleRad - spreadRad)).toFloat()
                )
                val baseRight = Offset(
                    x = centerOffset.x + (markerBaseR * cos(maxAngleRad + spreadRad)).toFloat(),
                    y = centerOffset.y + (markerBaseR * sin(maxAngleRad + spreadRad)).toFloat()
                )

                val markerPath = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(baseLeft.x, baseLeft.y)
                    lineTo(baseRight.x, baseRight.y)
                    close()
                }
                drawPath(path = markerPath, color = VelocityViolet)
            }

            // 6. Tapered needle pointing to current animated speed
            val needleAngleDeg = GAUGE_START_ANGLE + animatedFraction * GAUGE_SWEEP_ANGLE
            val needleAngleRad = needleAngleDeg * (PI / 180.0)
            val perpRad = needleAngleRad + PI / 2.0
            val needleLength = radius - strokeWidthPx * 0.65f
            val baseHalfWidth = 4.5.dp.toPx()
            val tipHalfWidth = 1.dp.toPx()
            val tailLength = 14.dp.toPx()

            val needleTip = Offset(
                x = centerOffset.x + (needleLength * cos(needleAngleRad)).toFloat(),
                y = centerOffset.y + (needleLength * sin(needleAngleRad)).toFloat()
            )
            val needleTail = Offset(
                x = centerOffset.x - (tailLength * cos(needleAngleRad)).toFloat(),
                y = centerOffset.y - (tailLength * sin(needleAngleRad)).toFloat()
            )
            val baseLeft = Offset(
                x = needleTail.x + (baseHalfWidth * cos(perpRad)).toFloat(),
                y = needleTail.y + (baseHalfWidth * sin(perpRad)).toFloat()
            )
            val baseRight = Offset(
                x = needleTail.x - (baseHalfWidth * cos(perpRad)).toFloat(),
                y = needleTail.y - (baseHalfWidth * sin(perpRad)).toFloat()
            )
            val tipLeft = Offset(
                x = needleTip.x + (tipHalfWidth * cos(perpRad)).toFloat(),
                y = needleTip.y + (tipHalfWidth * sin(perpRad)).toFloat()
            )
            val tipRight = Offset(
                x = needleTip.x - (tipHalfWidth * cos(perpRad)).toFloat(),
                y = needleTip.y - (tipHalfWidth * sin(perpRad)).toFloat()
            )

            val needlePath = Path().apply {
                moveTo(baseLeft.x, baseLeft.y)
                lineTo(tipLeft.x, tipLeft.y)
                lineTo(tipRight.x, tipRight.y)
                lineTo(baseRight.x, baseRight.y)
                close()
            }
            drawPath(path = needlePath, color = needleColor.copy(alpha = 0.92f))

            // 7. Center hub circle (16 dp diameter -> 8 dp radius) with 2 dp cyan ring
            val hubRadius = 8.dp.toPx()
            drawCircle(
                color = hubColor,
                radius = hubRadius,
                center = centerOffset
            )
            drawCircle(
                color = needleColor,
                radius = hubRadius,
                center = centerOffset,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center digital readout positioned cleanly in the lower-center opening of the 270° arc
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = displayedSpeed.toString(),
                style = AnalogSpeedTextStyle,
                color = speedTextColor
            )
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = tapUnitDesc,
                        onClick = onUnitClick
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = unitLabel.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = compassLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
