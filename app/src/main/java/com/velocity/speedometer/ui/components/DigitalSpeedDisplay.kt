package com.velocity.speedometer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.velocity.speedometer.R
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.ui.theme.DigitalSpeedTextStyle
import com.velocity.speedometer.ui.theme.LocalVelocityColors
import com.velocity.speedometer.ui.theme.VelocityCyan
import com.velocity.speedometer.ui.theme.VelocityDanger
import com.velocity.speedometer.ui.theme.VelocitySuccess
import com.velocity.speedometer.ui.theme.VelocityWarning
import kotlin.math.roundToInt

@Composable
fun DigitalSpeedDisplay(
    speedValue: Float,
    unit: SpeedUnit,
    unitLabel: String,
    compassLabel: String,
    isOverLimit: Boolean,
    isWarningZone: Boolean,
    onUnitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val velocityColors = LocalVelocityColors.current
    val scaleMax = unit.scaleMax.toFloat()
    val fraction = (speedValue / scaleMax).coerceIn(0f, 1f)

    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 300),
        label = "digital_bar_fraction"
    )

    val displayedSpeed by animateIntAsState(
        targetValue = speedValue.roundToInt().coerceAtLeast(0),
        animationSpec = tween(durationMillis = 300),
        label = "digital_speed_digits"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "digital_over_limit_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "digital_pulse"
    )
    val effectiveAlpha = if (isOverLimit) pulseAlpha else 1f

    val digitColor by animateColorAsState(
        targetValue = when {
            isOverLimit -> VelocityDanger.copy(alpha = effectiveAlpha.coerceIn(0.6f, 1f))
            isWarningZone -> VelocityWarning
            else -> MaterialTheme.colorScheme.onBackground
        },
        animationSpec = tween(250),
        label = "digital_digit_color"
    )

    val speedDesc = stringResource(R.string.cd_speed_readout, displayedSpeed, unitLabel)
    val tapUnitDesc = stringResource(R.string.cd_tap_unit, unitLabel)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = speedDesc
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = displayedSpeed.toString(),
            style = DigitalSpeedTextStyle,
            color = digitColor,
            maxLines = 1
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = unitLabel.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = compassLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Thin horizontal gradient bar showing speed as a fraction of the scale
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(8.dp)
        ) {
            val corner = CornerRadius(size.height / 2f, size.height / 2f)
            drawRoundRect(
                color = velocityColors.gaugeTrack,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = corner
            )
            val fillWidth = (size.width * animatedFraction).coerceAtLeast(0f)
            if (fillWidth > 1f) {
                val gradient = if (isOverLimit) {
                    Brush.horizontalGradient(
                        colors = listOf(VelocityDanger, VelocityDanger),
                        startX = 0f,
                        endX = size.width
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            VelocitySuccess,
                            VelocityCyan,
                            VelocityWarning,
                            VelocityDanger
                        ),
                        startX = 0f,
                        endX = size.width
                    )
                }
                drawRoundRect(
                    brush = gradient,
                    topLeft = Offset.Zero,
                    size = Size(fillWidth, size.height),
                    cornerRadius = corner,
                    alpha = effectiveAlpha
                )
            }
        }
    }
}
