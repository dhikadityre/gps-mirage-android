package com.dhikadityre.gpsmirage.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dhikadityre.gpsmirage.core.database.dao.DefaultProfileDao
import com.dhikadityre.gpsmirage.core.database.dao.FolderDao
import com.dhikadityre.gpsmirage.core.database.dao.HistoryDao
import com.dhikadityre.gpsmirage.core.database.dao.LocationDao
import com.dhikadityre.gpsmirage.core.database.dao.ReminderAlarmDao
import com.dhikadityre.gpsmirage.core.database.entity.DefaultProfileEntity
import com.dhikadityre.gpsmirage.core.database.entity.FolderEntity
import com.dhikadityre.gpsmirage.core.database.entity.HistoryEntity
import com.dhikadityre.gpsmirage.core.database.entity.LocationEntity
import com.dhikadityre.gpsmirage.core.database.entity.ReminderAlarmEntity

@Database(
    entities = [
        LocationEntity::class,
        FolderEntity::class,
        HistoryEntity::class,
        DefaultProfileEntity::class,
        ReminderAlarmEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class GpsMirageDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun folderDao(): FolderDao
    abstract fun historyDao(): HistoryDao
    abstract fun defaultProfileDao(): DefaultProfileDao
    abstract fun reminderAlarmDao(): ReminderAlarmDao

    companion object {
        const val DATABASE_NAME = "gps_mirage.db"
    }
}
