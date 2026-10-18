package com.dhikadityre.gpsmirage.data.repository

import com.dhikadityre.gpsmirage.core.database.dao.LocationDao
import com.dhikadityre.gpsmirage.core.database.entity.LocationEntity
import com.dhikadityre.gpsmirage.core.model.AnchorWithMocks
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.LocationType
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao
) : LocationRepository {

    override fun getAllFlow(): Flow<List<Location>> =
        locationDao.getAllFlow().map { entities -> entities.map { it.toDomain() } }

    override fun getFavoritesFlow(): Flow<List<Location>> =
        locationDao.getFavoritesFlow().map { entities -> entities.map { it.toDomain() } }

    override fun getByFolderFlow(folderId: Long): Flow<List<Location>> =
        locationDao.getByFolderFlow(folderId).map { entities -> entities.map { it.toDomain() } }

    override fun getByTypeFlow(type: LocationType): Flow<List<Location>> =
        locationDao.getByTypeFlow(type.name).map { entities -> entities.map { it.toDomain() } }

    override fun getChildrenOfAnchorFlow(anchorId: Long): Flow<List<Location>> =
        locationDao.getChildrenOfAnchorFlow(anchorId).map { entities -> entities.map { it.toDomain() } }

    override fun getAnchorsWithMocksFlow(): Flow<List<AnchorWithMocks>> =
        combine(
            getByTypeFlow(LocationType.CENTER_ANCHOR),
            getByTypeFlow(LocationType.MOCK_TARGET)
        ) { anchors, mocks ->
            anchors.map { anchor ->
                val children = mocks.filter { it.parentAnchorId == anchor.id }
                AnchorWithMocks(anchor = anchor, mocks = children)
            }
        }

    override suspend fun getAll(): List<Location> =
        locationDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: Long): Location? =
        locationDao.getById(id)?.toDomain()

    override suspend fun getByType(type: LocationType): List<Location> =
        locationDao.getByType(type.name).map { it.toDomain() }

    override suspend fun getChildrenOfAnchor(anchorId: Long): List<Location> =
        locationDao.getChildrenOfAnchor(anchorId).map { it.toDomain() }

    override suspend fun getFavorites(): List<Location> =
        locationDao.getFavorites().map { it.toDomain() }

    override suspend fun getByFolder(folderId: Long): List<Location> =
        locationDao.getByFolder(folderId).map { it.toDomain() }

    override suspend fun getRecent(limit: Int): List<Location> =
        locationDao.getRecent(limit).map { it.toDomain() }

    override suspend fun search(query: String): List<Location> =
        locationDao.search(query).map { it.toDomain() }

    override suspend fun save(location: Location): Long =
        locationDao.insert(LocationEntity.fromDomain(location))

    override suspend fun saveAll(locations: List<Location>): List<Long> =
        locationDao.insertAll(locations.map { LocationEntity.fromDomain(it) })

    override suspend fun update(location: Location) =
        locationDao.update(LocationEntity.fromDomain(location))

    override suspend fun updateFavorite(id: Long, isFavorite: Boolean) =
        locationDao.updateFavorite(id, isFavorite)

    override suspend fun updateFolder(id: Long, folderId: Long?) =
        locationDao.updateFolder(id, folderId)

    override suspend fun updateParentAnchor(id: Long, parentAnchorId: Long?) =
        locationDao.updateParentAnchor(id, parentAnchorId)

    override suspend fun delete(id: Long) =
        locationDao.deleteById(id)

    override suspend fun isDuplicate(name: String, latitude: Double, longitude: Double): Boolean =
        locationDao.countDuplicates(latitude, longitude, name) > 0
}

