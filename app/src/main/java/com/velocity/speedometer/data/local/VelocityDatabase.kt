package com.velocity.speedometer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TripEntity::class, TrackPointEntity::class],
    version = 1,
    exportSchema = true
)
abstract class VelocityDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}
