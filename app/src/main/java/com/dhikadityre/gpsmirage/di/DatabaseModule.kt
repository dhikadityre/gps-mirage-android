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

    private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `default_profiles` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `latitude` REAL NOT NULL,
                    `longitude` REAL NOT NULL,
                    `radius_meters` REAL NOT NULL,
                    `accuracy_meters` REAL NOT NULL,
                    `altitude_meters` REAL NOT NULL,
                    `speed_kmh` REAL NOT NULL,
                    `bearing_degrees` REAL NOT NULL,
                    `use_device_sensor` INTEGER NOT NULL,
                    `is_active` INTEGER NOT NULL,
                    `is_default` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reminder_alarms` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `label` TEXT NOT NULL,
                    `hour` INTEGER NOT NULL,
                    `minute` INTEGER NOT NULL,
                    `days_of_week` INTEGER NOT NULL,
                    `is_enabled` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `locations` ADD COLUMN `is_generated` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `reminder_alarms` ADD COLUMN `title` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `reminder_alarms` ADD COLUMN `is_delete_after_ringing` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `reminder_alarms` ADD COLUMN `sound_uri` TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE `reminder_alarms` ADD COLUMN `snooze_minutes` INTEGER NOT NULL DEFAULT 5")
            db.execSQL("ALTER TABLE `reminder_alarms` ADD COLUMN `is_vibrate` INTEGER NOT NULL DEFAULT 1")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GpsMirageDatabase {
        return Room.databaseBuilder(
            context,
            GpsMirageDatabase::class.java,
            GpsMirageDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideLocationDao(db: GpsMirageDatabase): LocationDao = db.locationDao()

    @Provides
    fun provideFolderDao(db: GpsMirageDatabase): FolderDao = db.folderDao()

    @Provides
    fun provideHistoryDao(db: GpsMirageDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideDefaultProfileDao(db: GpsMirageDatabase): com.dhikadityre.gpsmirage.core.database.dao.DefaultProfileDao =
        db.defaultProfileDao()

    @Provides
    fun provideReminderAlarmDao(db: GpsMirageDatabase): com.dhikadityre.gpsmirage.core.database.dao.ReminderAlarmDao =
        db.reminderAlarmDao()
}
