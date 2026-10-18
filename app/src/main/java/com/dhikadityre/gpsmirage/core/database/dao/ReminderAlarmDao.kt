package com.dhikadityre.gpsmirage.core.database.dao

import androidx.room.*
import com.dhikadityre.gpsmirage.core.database.entity.ReminderAlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderAlarmDao {

    @Query("SELECT * FROM reminder_alarms ORDER BY hour ASC, minute ASC")
    fun getAll(): Flow<List<ReminderAlarmEntity>>

    @Query("SELECT * FROM reminder_alarms ORDER BY hour ASC, minute ASC")
    suspend fun getAllList(): List<ReminderAlarmEntity>

    @Query("SELECT * FROM reminder_alarms WHERE is_enabled = 1 ORDER BY hour ASC, minute ASC")
    suspend fun getEnabledAlarms(): List<ReminderAlarmEntity>

    @Query("SELECT * FROM reminder_alarms WHERE id = :id")
    suspend fun getById(id: Long): ReminderAlarmEntity?

    @Query("SELECT COUNT(*) FROM reminder_alarms")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReminderAlarmEntity): Long

    @Update
    suspend fun update(entity: ReminderAlarmEntity)

    @Query("DELETE FROM reminder_alarms WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE reminder_alarms SET is_enabled = :isEnabled, updated_at = :now")
    suspend fun setAllEnabled(isEnabled: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE reminder_alarms SET is_enabled = :isEnabled, updated_at = :now WHERE id = :id")
    suspend fun updateEnabled(id: Long, isEnabled: Boolean, now: Long = System.currentTimeMillis())
}
