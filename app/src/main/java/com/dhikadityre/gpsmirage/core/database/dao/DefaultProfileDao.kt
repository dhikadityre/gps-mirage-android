package com.dhikadityre.gpsmirage.core.database.dao

import androidx.room.*
import com.dhikadityre.gpsmirage.core.database.entity.DefaultProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DefaultProfileDao {

    @Query("SELECT * FROM default_profiles ORDER BY is_default DESC, created_at ASC")
    fun getAll(): Flow<List<DefaultProfileEntity>>

    @Query("SELECT * FROM default_profiles WHERE is_active = 1 LIMIT 1")
    fun getActive(): Flow<DefaultProfileEntity?>

    @Query("SELECT * FROM default_profiles WHERE id = :id")
    suspend fun getById(id: Long): DefaultProfileEntity?

    @Query("SELECT * FROM default_profiles WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultProfile(): DefaultProfileEntity?

    @Query("SELECT * FROM default_profiles LIMIT 1")
    suspend fun getFirstProfile(): DefaultProfileEntity?

    @Query("SELECT COUNT(*) FROM default_profiles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DefaultProfileEntity): Long

    @Update
    suspend fun update(entity: DefaultProfileEntity)

    @Query("DELETE FROM default_profiles WHERE id = :id AND is_default = 0")
    suspend fun delete(id: Long)

    @Query("UPDATE default_profiles SET is_active = 0")
    suspend fun clearActive()

    @Query("UPDATE default_profiles SET is_active = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Transaction
    suspend fun activateProfile(id: Long) {
        clearActive()
        setActive(id)
    }
}
