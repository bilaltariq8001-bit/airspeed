package com.velocity.speedometer.di

import android.content.Context
import androidx.room.Room
import com.velocity.speedometer.data.local.TripDao
import com.velocity.speedometer.data.local.VelocityDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVelocityDatabase(
        @ApplicationContext context: Context
    ): VelocityDatabase = Room.databaseBuilder(
        context,
        VelocityDatabase::class.java,
        "velocity.db"
    ).build()

    @Provides
    fun provideTripDao(database: VelocityDatabase): TripDao = database.tripDao()
}
