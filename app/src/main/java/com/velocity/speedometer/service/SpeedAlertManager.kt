package com.velocity.speedometer.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeedAlertManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var lastAlertTimestampMs: Long = 0L
    private var toneGenerator: ToneGenerator? = null

    fun triggerOverLimitAlertIfDue(
        enableVibration: Boolean,
        enableSound: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ) {
        if (!enableVibration && !enableSound) return
        if (nowMs - lastAlertTimestampMs < ALERT_COOLDOWN_MS) return
        lastAlertTimestampMs = nowMs

        if (enableVibration) {
            vibrateAlert()
        }
        if (enableSound) {
            playBeep()
        }
    }

    fun resetCooldown() {
        lastAlertTimestampMs = 0L
    }

    private fun vibrateAlert() {
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.let {
                if (it.hasVibrator()) {
                    it.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0L, 140L, 80L, 140L),
                            -1
                        )
                    )
                }
            }
        }
    }

    private fun playBeep() {
        runCatching {
            val generator = toneGenerator ?: ToneGenerator(
                AudioManager.STREAM_NOTIFICATION,
                85
            ).also { toneGenerator = it }
            generator.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
        }
    }

    fun release() {
        runCatching {
            toneGenerator?.release()
            toneGenerator = null
        }
    }

    private companion object {
        const val ALERT_COOLDOWN_MS = 2500L
    }
}
