package com.velocity.speedometer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.velocity.speedometer.R
import com.velocity.speedometer.domain.model.TripState
import com.velocity.speedometer.ui.theme.LocalVelocityColors
import com.velocity.speedometer.ui.theme.VelocityDanger

@Composable
fun TripControlsBar(
    tripState: TripState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val velocityColors = LocalVelocityColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, velocityColors.divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val historyDesc = stringResource(R.string.cd_history_button)
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onHistoryClick()
                },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = historyDesc }
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = historyDesc,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            AnimatedContent(
                targetState = tripState,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.94f)) togetherWith
                        (fadeOut() + scaleOut(targetScale = 0.94f))
                },
                modifier = Modifier.weight(1f),
                label = "trip_control_morph"
            ) { state ->
                if (state == TripState.IDLE) {
                    val startCd = stringResource(R.string.cd_start_trip)
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStart()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .semantics { contentDescription = startCd },
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_start),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    val isPaused = state == TripState.PAUSED || state == TripState.AUTO_PAUSED
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val pauseResumeCd = if (isPaused) {
                            stringResource(R.string.cd_resume_trip)
                        } else {
                            stringResource(R.string.cd_pause_trip)
                        }
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isPaused) onResume() else onPause()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .semantics { contentDescription = pauseResumeCd },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(
                                    if (isPaused) R.string.action_resume else R.string.action_pause
                                ),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        val stopCd = stringResource(R.string.cd_stop_trip)
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStop()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .semantics { contentDescription = stopCd },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VelocityDanger,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_stop),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            val settingsDesc = stringResource(R.string.cd_settings_button)
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSettingsClick()
                },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = settingsDesc }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = settingsDesc,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
