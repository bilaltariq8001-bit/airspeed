package com.velocity.speedometer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.velocity.speedometer.R
import com.velocity.speedometer.domain.model.GpsSignalQuality
import com.velocity.speedometer.ui.theme.LocalVelocityColors
import kotlin.math.roundToInt

@Composable
fun GpsStatusPill(
    accuracyMeters: Float?,
    gpsQuality: GpsSignalQuality,
    modifier: Modifier = Modifier
) {
    val velocityColors = LocalVelocityColors.current
    val dotColor = when (gpsQuality) {
        GpsSignalQuality.GOOD -> velocityColors.success
        GpsSignalQuality.MODERATE -> velocityColors.warning
        GpsSignalQuality.POOR_OR_NONE -> velocityColors.danger
    }

    val label = if (accuracyMeters != null && accuracyMeters < 1000f) {
        stringResource(R.string.gps_accuracy_meters, accuracyMeters.roundToInt())
    } else {
        stringResource(R.string.gps_no_fix)
    }

    val cdText = stringResource(R.string.cd_gps_status, label)

    Surface(
        modifier = modifier.semantics { contentDescription = cdText },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, velocityColors.divider)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
