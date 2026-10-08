package com.dhikadityre.gpsmirage.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dhikadityre.gpsmirage.core.database.dao.FolderDao
import com.dhikadityre.gpsmirage.core.database.dao.HistoryDao
import com.dhikadityre.gpsmirage.core.database.dao.LocationDao
import com.dhikadityre.gpsmirage.core.database.entity.FolderEntity
import com.dhikadityre.gpsmirage.core.database.entity.HistoryEntity
import com.dhikadityre.gpsmirage.core.database.entity.LocationEntity

@Database(
    entities = [
        LocationEntity::class,
        FolderEntity::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GpsMirageDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun folderDao(): FolderDao
    abstract fun historyDao(): HistoryDao

    companion object {
        const val DATABASE_NAME = "gps_mirage.db"
    }
}
