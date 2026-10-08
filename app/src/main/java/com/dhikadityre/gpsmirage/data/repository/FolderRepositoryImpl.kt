package com.dhikadityre.gpsmirage.data.repository

import com.dhikadityre.gpsmirage.core.database.dao.FolderDao
import com.dhikadityre.gpsmirage.core.database.entity.FolderEntity
import com.dhikadityre.gpsmirage.core.model.Folder
import com.dhikadityre.gpsmirage.domain.folder.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao
) : FolderRepository {

    override fun getAllFlow(): Flow<List<Folder>> =
        folderDao.getAllFlow().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAll(): List<Folder> =
        folderDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: Long): Folder? =
        folderDao.getById(id)?.toDomain()

    override suspend fun create(folder: Folder): Long =
        folderDao.insert(FolderEntity.fromDomain(folder))

    override suspend fun rename(id: Long, name: String) =
        folderDao.rename(id, name)

    override suspend fun delete(id: Long) =
        folderDao.deleteById(id)

    override suspend fun getLocationCount(folderId: Long): Int =
        folderDao.countLocations(folderId)
}
