package com.velocity.speedometer.domain.repository

import com.velocity.speedometer.domain.model.LiveTelemetry
import com.velocity.speedometer.domain.model.TrackPoint
import com.velocity.speedometer.domain.model.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TripRepository {
    fun observeAllTrips(): Flow<List<Trip>>
    fun observeTripById(tripId: Long): Flow<Trip?>
    fun observeTrackPoints(tripId: Long): Flow<List<TrackPoint>>
    suspend fun getTripById(tripId: Long): Trip?
    suspend fun getTrackPoints(tripId: Long): List<TrackPoint>
    suspend fun insertTrip(trip: Trip): Long
    suspend fun updateTrip(trip: Trip)
    suspend fun insertTrackPoint(point: TrackPoint)
    suspend fun deleteTrip(tripId: Long)
    suspend fun deleteAllTrips()
}

interface TrackingController {
    val telemetry: StateFlow<LiveTelemetry>

    fun startForegroundPreview()
    fun stopForegroundPreview()
    fun startTrip()
    fun pauseTrip()
    fun resumeTrip()
    fun stopTrip()
}
