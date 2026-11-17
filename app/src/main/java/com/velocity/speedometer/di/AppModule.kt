package com.velocity.speedometer.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.velocity.speedometer.data.preferences.SettingsDataStoreRepository
import com.velocity.speedometer.data.repository.RoomTripRepository
import com.velocity.speedometer.domain.repository.SettingsRepository
import com.velocity.speedometer.domain.repository.TrackingController
import com.velocity.speedometer.domain.repository.TripRepository
import com.velocity.speedometer.service.TrackingManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsDataStoreRepository
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindTripRepository(
        impl: RoomTripRepository
    ): TripRepository

    @Binds
    @Singleton
    abstract fun bindTrackingController(
        impl: TrackingManager
    ): TrackingController
}

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
}
