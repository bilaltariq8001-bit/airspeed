package com.velocity.speedometer.data.local

import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.TrackPoint
import com.velocity.speedometer.domain.model.Trip

fun TripEntity.toDomain(): Trip = Trip(
    id = id,
    startTime = startTime,
    endTime = endTime,
    distanceMeters = distanceMeters,
    maxSpeedMps = maxSpeedMps,
    avgSpeedMps = avgSpeedMps,
    durationMs = durationMs,
    unitAtRecording = runCatching { SpeedUnit.valueOf(unitAtRecording) }.getOrDefault(SpeedUnit.KMH)
)

fun Trip.toEntity(): TripEntity = TripEntity(
    id = id,
    startTime = startTime,
    endTime = endTime,
    distanceMeters = distanceMeters,
    maxSpeedMps = maxSpeedMps,
    avgSpeedMps = avgSpeedMps,
    durationMs = durationMs,
    unitAtRecording = unitAtRecording.name
)

fun TrackPointEntity.toDomain(): TrackPoint = TrackPoint(
    id = id,
    tripId = tripId,
    timestamp = timestamp,
    lat = lat,
    lng = lng,
    speedMps = speedMps,
    altitude = altitude,
    accuracy = accuracy,
    bearing = bearing
)

fun TrackPoint.toEntity(): TrackPointEntity = TrackPointEntity(
    id = id,
    tripId = tripId,
    timestamp = timestamp,
    lat = lat,
    lng = lng,
    speedMps = speedMps,
    altitude = altitude,
    accuracy = accuracy,
    bearing = bearing
)
