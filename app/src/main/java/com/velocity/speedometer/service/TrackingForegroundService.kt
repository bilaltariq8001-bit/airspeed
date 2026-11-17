package com.velocity.speedometer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.velocity.speedometer.MainActivity
import com.velocity.speedometer.R
import com.velocity.speedometer.domain.model.LiveTelemetry
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.TripState
import com.velocity.speedometer.domain.repository.SettingsRepository
import com.velocity.speedometer.domain.repository.TrackingController
import com.velocity.speedometer.domain.util.SpeedConverter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TrackingForegroundService : LifecycleService() {

    @Inject
    lateinit var trackingController: TrackingController

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var notificationObserverStarted = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_PAUSE -> trackingController.pauseTrip()
            ACTION_RESUME -> trackingController.resumeTrip()
            ACTION_STOP -> {
                trackingController.stopTrip()
                stopSelfSafely()
                return START_NOT_STICKY
            }
            ACTION_STOP_SERVICE -> {
                stopSelfSafely()
                return START_NOT_STICKY
            }
            else -> {
                promoteToForeground(
                    telemetry = trackingController.telemetry.value,
                    unit = SpeedUnit.KMH
                )
                observeTelemetryForNotification()
            }
        }
        return START_STICKY
    }

    private fun observeTelemetryForNotification() {
        if (notificationObserverStarted) return
        notificationObserverStarted = true

        lifecycleScope.launch {
            combine(
                trackingController.telemetry,
                settingsRepository.settings
            ) { telemetry, settings ->
                telemetry to settings.unit
            }.collect { (telemetry, unit) ->
                if (telemetry.tripState == TripState.IDLE) {
                    stopSelfSafely()
                } else {
                    val manager = getSystemService(NotificationManager::class.java)
                    manager.notify(NOTIFICATION_ID, buildNotification(telemetry, unit))
                }
            }
        }
    }

    private fun promoteToForeground(telemetry: LiveTelemetry, unit: SpeedUnit) {
        val notification = buildNotification(telemetry, unit)
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
    }

    private fun buildNotification(telemetry: LiveTelemetry, unit: SpeedUnit): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPaused = telemetry.tripState == TripState.PAUSED ||
            telemetry.tripState == TripState.AUTO_PAUSED

        val pauseOrResumeAction = if (isPaused) {
            val resumeIntent = Intent(this, TrackingForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getService(
                this,
                1,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                0,
                getString(R.string.notif_action_resume),
                resumePendingIntent
            ).build()
        } else {
            val pauseIntent = Intent(this, TrackingForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                2,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                0,
                getString(R.string.notif_action_pause),
                pausePendingIntent
            ).build()
        }

        val stopIntent = Intent(this, TrackingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = NotificationCompat.Action.Builder(
            0,
            getString(R.string.notif_action_stop),
            stopPendingIntent
        ).build()

        val unitLabel = when (unit) {
            SpeedUnit.KMH -> getString(R.string.unit_kmh)
            SpeedUnit.MPH -> getString(R.string.unit_mph)
            SpeedUnit.KNOTS -> getString(R.string.unit_knots)
        }
        val speedValue = SpeedConverter.fromMpsRounded(telemetry.smoothedSpeedMps, unit)
        val distanceText = SpeedConverter.formatDistance(telemetry.distanceMeters, unit)

        val title = if (isPaused) {
            getString(R.string.notif_title_paused)
        } else {
            getString(R.string.notif_title_recording)
        }
        val body = getString(R.string.notif_body_format, speedValue, unitLabel, distanceText)

        return NotificationCompat.Builder(this, getString(R.string.notif_channel_id))
            .setSmallIcon(R.drawable.ic_notification_speed)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(pauseOrResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            getString(R.string.notif_channel_id),
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun stopSelfSafely() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_START_FOREGROUND = "com.velocity.speedometer.action.START_FOREGROUND"
        const val ACTION_PAUSE = "com.velocity.speedometer.action.PAUSE"
        const val ACTION_RESUME = "com.velocity.speedometer.action.RESUME"
        const val ACTION_STOP = "com.velocity.speedometer.action.STOP"
        const val ACTION_STOP_SERVICE = "com.velocity.speedometer.action.STOP_SERVICE"
        private const val NOTIFICATION_ID = 1001
    }
}
