package com.dhikadityre.gpsmirage.di

import android.content.Context
import androidx.room.Room
import com.dhikadityre.gpsmirage.core.database.GpsMirageDatabase
import com.dhikadityre.gpsmirage.core.database.dao.FolderDao
import com.dhikadityre.gpsmirage.core.database.dao.HistoryDao
import com.dhikadityre.gpsmirage.core.database.dao.LocationDao
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
    fun provideDatabase(@ApplicationContext context: Context): GpsMirageDatabase {
        return Room.databaseBuilder(
            context,
            GpsMirageDatabase::class.java,
            GpsMirageDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideLocationDao(db: GpsMirageDatabase): LocationDao = db.locationDao()

    @Provides
    fun provideFolderDao(db: GpsMirageDatabase): FolderDao = db.folderDao()

    @Provides
    fun provideHistoryDao(db: GpsMirageDatabase): HistoryDao = db.historyDao()
}
