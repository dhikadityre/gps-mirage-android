package com.dhikadityre.gpsmirage.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dhikadityre.gpsmirage.core.database.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations ORDER BY updated_at DESC")
    fun getAllFlow(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations ORDER BY updated_at DESC")
    suspend fun getAll(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LocationEntity?

    @Query("SELECT * FROM locations WHERE is_favorite = 1 ORDER BY updated_at DESC")
    fun getFavoritesFlow(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE is_favorite = 1 ORDER BY updated_at DESC")
    suspend fun getFavorites(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE folder_id = :folderId ORDER BY updated_at DESC")
    fun getByFolderFlow(folderId: Long): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE folder_id = :folderId ORDER BY updated_at DESC")
    suspend fun getByFolder(folderId: Long): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE folder_id IS NULL ORDER BY updated_at DESC")
    fun getUnfolderedFlow(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE name LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    suspend fun search(query: String): List<LocationEntity>

    @Query("SELECT * FROM locations ORDER BY updated_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 5): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: LocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(locations: List<LocationEntity>): List<Long>

    @Update
    suspend fun update(location: LocationEntity)

    @Query("UPDATE locations SET is_favorite = :isFavorite, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE locations SET folder_id = :folderId, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateFolder(id: Long, folderId: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(location: LocationEntity)

    @Query("SELECT * FROM locations WHERE location_type = :type ORDER BY updated_at DESC")
    fun getByTypeFlow(type: String): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE location_type = :type ORDER BY updated_at DESC")
    suspend fun getByType(type: String): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE parent_anchor_id = :anchorId ORDER BY updated_at DESC")
    fun getChildrenOfAnchorFlow(anchorId: Long): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE parent_anchor_id = :anchorId ORDER BY updated_at DESC")
    suspend fun getChildrenOfAnchor(anchorId: Long): List<LocationEntity>

    @Query("UPDATE locations SET parent_anchor_id = :parentAnchorId, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateParentAnchor(id: Long, parentAnchorId: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM locations WHERE latitude = :lat AND longitude = :lng AND name = :name")
    suspend fun countDuplicates(lat: Double, lng: Double, name: String): Int

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun count(): Int
}

