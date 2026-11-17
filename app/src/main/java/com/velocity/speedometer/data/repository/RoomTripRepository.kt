package com.velocity.speedometer.data.repository

import com.velocity.speedometer.data.local.TripDao
import com.velocity.speedometer.data.local.toDomain
import com.velocity.speedometer.data.local.toEntity
import com.velocity.speedometer.domain.model.TrackPoint
import com.velocity.speedometer.domain.model.Trip
import com.velocity.speedometer.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTripRepository @Inject constructor(
    private val tripDao: TripDao
) : TripRepository {

    override fun observeAllTrips(): Flow<List<Trip>> =
        tripDao.observeCompletedTrips().map { list -> list.map { it.toDomain() } }

    override fun observeTripById(tripId: Long): Flow<Trip?> =
        tripDao.observeTripById(tripId).map { it?.toDomain() }

    override fun observeTrackPoints(tripId: Long): Flow<List<TrackPoint>> =
        tripDao.observeTrackPoints(tripId).map { list -> list.map { it.toDomain() } }

    override suspend fun getTripById(tripId: Long): Trip? =
        tripDao.getTripById(tripId)?.toDomain()

    override suspend fun getTrackPoints(tripId: Long): List<TrackPoint> =
        tripDao.getTrackPoints(tripId).map { it.toDomain() }

    override suspend fun insertTrip(trip: Trip): Long =
        tripDao.insertTrip(trip.toEntity())

    override suspend fun updateTrip(trip: Trip) {
        tripDao.updateTrip(trip.toEntity())
    }

    override suspend fun insertTrackPoint(point: TrackPoint) {
        tripDao.insertTrackPoint(point.toEntity())
    }

    override suspend fun deleteTrip(tripId: Long) {
        tripDao.deleteTrip(tripId)
    }

    override suspend fun deleteAllTrips() {
        tripDao.deleteAllTrips()
    }
}
