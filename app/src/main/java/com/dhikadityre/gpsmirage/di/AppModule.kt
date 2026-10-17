package com.dhikadityre.gpsmirage.di

import com.dhikadityre.gpsmirage.domain.location.MockLocationManager
import com.dhikadityre.gpsmirage.domain.location.MockLocationManagerImpl
import com.dhikadityre.gpsmirage.core.geo.GeoEngine
import com.dhikadityre.gpsmirage.core.geo.GeoEngineImpl
import com.dhikadityre.gpsmirage.core.parser.LocationParser
import com.dhikadityre.gpsmirage.core.parser.LocationParserImpl
import com.dhikadityre.gpsmirage.core.location.DeviceLocationProvider
import com.dhikadityre.gpsmirage.core.location.DeviceLocationProviderImpl
import com.dhikadityre.gpsmirage.core.geo.ReverseGeocoder
import com.dhikadityre.gpsmirage.core.geo.AndroidReverseGeocoderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindMockLocationManager(
        impl: MockLocationManagerImpl
    ): MockLocationManager

    @Binds
    @Singleton
    abstract fun bindGeoEngine(
        impl: GeoEngineImpl
    ): GeoEngine

    @Binds
    @Singleton
    abstract fun bindLocationParser(
        impl: LocationParserImpl
    ): LocationParser

    @Binds
    @Singleton
    abstract fun bindDeviceLocationProvider(
        impl: DeviceLocationProviderImpl
    ): DeviceLocationProvider

    @Binds
    @Singleton
    abstract fun bindReverseGeocoder(
        impl: AndroidReverseGeocoderImpl
    ): ReverseGeocoder
}
