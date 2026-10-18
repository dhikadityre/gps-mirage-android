package com.dhikadityre.gpsmirage.data.repository

import com.dhikadityre.gpsmirage.core.database.dao.DefaultProfileDao
import com.dhikadityre.gpsmirage.core.database.entity.DefaultProfileEntity
import com.dhikadityre.gpsmirage.core.model.DefaultValueProfile
import com.dhikadityre.gpsmirage.domain.preset.DefaultProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultProfileRepositoryImpl(
    private val dao: DefaultProfileDao,
    private val ioDispatcher: CoroutineDispatcher
) : DefaultProfileRepository {

    @Inject
    constructor(dao: DefaultProfileDao) : this(dao, Dispatchers.IO)

    override val allProfiles: Flow<List<DefaultValueProfile>> = dao.getAll()
        .onStart { ensureDefaultSeeded() }
        .map { entities -> entities.map { it.toDomain() } }

    override val activeProfile: Flow<DefaultValueProfile?> = dao.getActive()
        .onStart { ensureDefaultSeeded() }
        .map { it?.toDomain() }

    override suspend fun getById(id: Long): DefaultValueProfile? = withContext(ioDispatcher) {
        dao.getById(id)?.toDomain()
    }

    override suspend fun saveProfile(profile: DefaultValueProfile): Long = withContext(ioDispatcher) {
        val entity = DefaultProfileEntity.fromDomain(profile)
        if (entity.id == 0L) {
            val newId = dao.insert(entity)
            if (profile.isActive) {
                dao.activateProfile(newId)
            }
            newId
        } else {
            dao.update(entity)
            if (profile.isActive) {
                dao.activateProfile(entity.id)
            }
            entity.id
        }
    }

    override suspend fun deleteProfile(id: Long): Unit = withContext(ioDispatcher) {
        val target = dao.getById(id) ?: return@withContext
        if (target.isDefault) return@withContext // Do not delete system default preset

        val wasActive = target.isActive
        dao.delete(id)

        if (wasActive) {
            val fallback = dao.getDefaultProfile() ?: dao.getFirstProfile()
            if (fallback != null) {
                dao.activateProfile(fallback.id)
            }
        }
    }

    override suspend fun setActiveProfile(id: Long): Unit = withContext(ioDispatcher) {
        dao.activateProfile(id)
    }

    override suspend fun resetOriginalPreset(): Unit = withContext(ioDispatcher) {
        val defaultProfile = dao.getDefaultProfile()
        val resetEntity = DefaultProfileEntity.fromDomain(
            DefaultValueProfile.ORIGINAL.copy(
                id = defaultProfile?.id ?: 1L,
                isActive = defaultProfile?.isActive ?: true
            )
        )
        dao.insert(resetEntity)
    }

    override suspend fun ensureDefaultSeeded(): Unit = withContext(ioDispatcher) {
        if (dao.count() == 0) {
            dao.insert(DefaultProfileEntity.fromDomain(DefaultValueProfile.ORIGINAL))
        }
    }
}
