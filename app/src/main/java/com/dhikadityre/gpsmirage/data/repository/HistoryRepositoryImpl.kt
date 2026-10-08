package com.dhikadityre.gpsmirage.data.repository

import com.dhikadityre.gpsmirage.core.database.dao.HistoryDao
import com.dhikadityre.gpsmirage.core.database.entity.HistoryEntity
import com.dhikadityre.gpsmirage.core.model.History
import com.dhikadityre.gpsmirage.domain.history.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override fun getAllFlow(): Flow<List<History>> =
        historyDao.getAllFlow().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAll(): List<History> =
        historyDao.getAll().map { it.toDomain() }

    override suspend fun getActiveSession(): History? =
        historyDao.getActiveSession()?.toDomain()

    override suspend fun startSession(history: History): Long =
        historyDao.insert(HistoryEntity.fromDomain(history))

    override suspend fun stopSession(id: Long, stoppedAt: Long, durationMs: Long) =
        historyDao.stopSession(id, stoppedAt, durationMs)

    override suspend fun delete(id: Long) =
        historyDao.deleteById(id)

    override suspend fun clearAll() =
        historyDao.clearAll()
}
