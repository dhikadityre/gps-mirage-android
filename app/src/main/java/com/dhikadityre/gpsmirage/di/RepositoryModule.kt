package com.dhikadityre.gpsmirage.di

import com.dhikadityre.gpsmirage.data.repository.FolderRepositoryImpl
import com.dhikadityre.gpsmirage.data.repository.HistoryRepositoryImpl
import com.dhikadityre.gpsmirage.data.repository.LocationRepositoryImpl
import com.dhikadityre.gpsmirage.domain.folder.FolderRepository
import com.dhikadityre.gpsmirage.domain.history.HistoryRepository
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import com.dhikadityre.gpsmirage.data.repository.SettingsRepositoryImpl
import com.dhikadityre.gpsmirage.domain.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(
        impl: FolderRepositoryImpl
    ): FolderRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        impl: HistoryRepositoryImpl
    ): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindDefaultProfileRepository(
        impl: com.dhikadityre.gpsmirage.data.repository.DefaultProfileRepositoryImpl
    ): com.dhikadityre.gpsmirage.domain.preset.DefaultProfileRepository
}
