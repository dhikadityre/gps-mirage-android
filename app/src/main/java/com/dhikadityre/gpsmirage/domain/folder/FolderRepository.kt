package com.dhikadityre.gpsmirage.domain.folder

import com.dhikadityre.gpsmirage.core.model.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun getAllFlow(): Flow<List<Folder>>
    suspend fun getAll(): List<Folder>
    suspend fun getById(id: Long): Folder?
    suspend fun create(folder: Folder): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)
    suspend fun getLocationCount(folderId: Long): Int
}
