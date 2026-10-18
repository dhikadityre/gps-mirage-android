package com.dhikadityre.gpsmirage.domain.location

import com.dhikadityre.gpsmirage.core.model.AnchorWithMocks
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.LocationType
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getAllFlow(): Flow<List<Location>>
    fun getFavoritesFlow(): Flow<List<Location>>
    fun getByFolderFlow(folderId: Long): Flow<List<Location>>
    fun getByTypeFlow(type: LocationType): Flow<List<Location>>
    fun getChildrenOfAnchorFlow(anchorId: Long): Flow<List<Location>>
    fun getAnchorsWithMocksFlow(): Flow<List<AnchorWithMocks>>
    suspend fun getAll(): List<Location>
    suspend fun getById(id: Long): Location?
    suspend fun getByType(type: LocationType): List<Location>
    suspend fun getChildrenOfAnchor(anchorId: Long): List<Location>
    suspend fun getFavorites(): List<Location>
    suspend fun getByFolder(folderId: Long): List<Location>
    suspend fun getRecent(limit: Int = 5): List<Location>
    suspend fun search(query: String): List<Location>
    suspend fun save(location: Location): Long
    suspend fun saveAll(locations: List<Location>): List<Long>
    suspend fun update(location: Location)
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
    suspend fun updateFolder(id: Long, folderId: Long?)
    suspend fun updateParentAnchor(id: Long, parentAnchorId: Long?)
    suspend fun delete(id: Long)
    suspend fun isDuplicate(name: String, latitude: Double, longitude: Double): Boolean
}

