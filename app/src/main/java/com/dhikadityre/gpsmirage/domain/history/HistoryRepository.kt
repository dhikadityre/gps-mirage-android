package com.dhikadityre.gpsmirage.domain.history

import com.dhikadityre.gpsmirage.core.model.History
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getAllFlow(): Flow<List<History>>
    suspend fun getAll(): List<History>
    suspend fun getActiveSession(): History?
    suspend fun startSession(history: History): Long
    suspend fun stopSession(id: Long, stoppedAt: Long, durationMs: Long)
    suspend fun delete(id: Long)
    suspend fun clearAll()
}
