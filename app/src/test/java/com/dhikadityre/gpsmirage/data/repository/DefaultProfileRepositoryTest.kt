package com.dhikadityre.gpsmirage.data.repository

import com.dhikadityre.gpsmirage.core.database.dao.DefaultProfileDao
import com.dhikadityre.gpsmirage.core.database.entity.DefaultProfileEntity
import com.dhikadityre.gpsmirage.core.geo.UnitConverter
import com.dhikadityre.gpsmirage.core.model.DefaultValueProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultProfileRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeDefaultProfileDao
    private lateinit var repository: DefaultProfileRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeDefaultProfileDao()
        repository = DefaultProfileRepositoryImpl(fakeDao, testDispatcher)
    }

    @Test
    fun `initialization seeds original preset automatically`() = runTest(testDispatcher) {
        val active = repository.activeProfile.first()

        assertNotNull(active)
        assertEquals("original", active?.name)
        assertEquals(-6.213647, active?.latitude ?: 0.0, 0.000001)
        assertEquals(106.819969, active?.longitude ?: 0.0, 0.000001)
        assertEquals(50.0, active?.radiusMeters ?: 0.0, 0.001)
        assertEquals(78.0, active?.accuracyFeet ?: 0.0, 0.01)
        assertEquals(23.77, active?.accuracyMeters ?: 0.0, 0.01)
        assertEquals(89.0, active?.altitudeFeet ?: 0.0, 0.01)
        assertEquals(27.13, active?.altitudeMeters ?: 0.0, 0.01)
        assertEquals(0.0, active?.speedKmh ?: 0.0, 0.001)
        assertEquals(0.0, active?.bearingDegrees ?: 0.0, 0.001)
        assertTrue(active?.useDeviceSensor == true)
        assertTrue(active?.isActive == true)
        assertTrue(active?.isDefault == true)
    }

    @Test
    fun `saveProfile inserts custom profile and setActive switches active profile`() = runTest(testDispatcher) {
        // Ensure seeded
        repository.ensureDefaultSeeded()

        val customProfile = DefaultValueProfile(
            id = 0L,
            name = "Office Preset",
            latitude = -6.200000,
            longitude = 106.800000,
            radiusMeters = 100.0,
            accuracyMeters = 10.0,
            altitudeMeters = 15.0,
            speedKmh = 5.0,
            bearingDegrees = 90.0,
            useDeviceSensor = false,
            isActive = false,
            isDefault = false
        )

        val newId = repository.saveProfile(customProfile)
        val all = repository.allProfiles.first()
        assertEquals(2, all.size)

        repository.setActiveProfile(newId)
        val active = repository.activeProfile.first()
        assertEquals("Office Preset", active?.name)
        assertEquals(newId, active?.id)
        assertFalse(active?.useDeviceSensor ?: true)
    }

    @Test
    fun `deleteProfile removes profile and falls back to default if deleted was active`() = runTest(testDispatcher) {
        repository.ensureDefaultSeeded()

        val custom = DefaultValueProfile(
            id = 0L,
            name = "Temporary",
            latitude = -6.1,
            longitude = 106.1,
            radiusMeters = 10.0,
            accuracyMeters = 5.0,
            altitudeMeters = 0.0,
            speedKmh = 0.0,
            bearingDegrees = 0.0,
            isActive = true
        )
        val id = repository.saveProfile(custom)
        repository.setActiveProfile(id)

        assertEquals("Temporary", repository.activeProfile.first()?.name)

        repository.deleteProfile(id)
        val allAfterDelete = repository.allProfiles.first()
        assertEquals(1, allAfterDelete.size)

        val activeFallback = repository.activeProfile.first()
        assertEquals("original", activeFallback?.name)
    }

    @Test
    fun `deleteProfile cannot delete system default preset`() = runTest(testDispatcher) {
        repository.ensureDefaultSeeded()
        val defaultProfile = repository.allProfiles.first().first { it.isDefault }

        repository.deleteProfile(defaultProfile.id)
        val all = repository.allProfiles.first()
        assertEquals(1, all.size)
        assertEquals("original", all.first().name)
    }

    @Test
    fun `resetOriginalPreset restores original preset parameters`() = runTest(testDispatcher) {
        repository.ensureDefaultSeeded()
        val defaultProfile = repository.allProfiles.first().first { it.isDefault }

        // Modify default profile
        val modified = defaultProfile.copy(radiusMeters = 999.0, name = "Altered")
        repository.saveProfile(modified)

        val checkModified = repository.getById(defaultProfile.id)
        assertEquals(999.0, checkModified?.radiusMeters ?: 0.0, 0.001)

        repository.resetOriginalPreset()
        val reset = repository.getById(defaultProfile.id)
        assertEquals(50.0, reset?.radiusMeters ?: 0.0, 0.001)
        assertEquals("original", reset?.name)
    }
}

class FakeDefaultProfileDao : DefaultProfileDao {
    private val profiles = mutableListOf<DefaultProfileEntity>()
    private val stateFlow = MutableStateFlow<List<DefaultProfileEntity>>(emptyList())
    private var nextId = 1L

    private fun notifyChanges() {
        stateFlow.value = profiles.toList()
    }

    override fun getAll(): Flow<List<DefaultProfileEntity>> = stateFlow

    override fun getActive(): Flow<DefaultProfileEntity?> = stateFlow.map { list ->
        list.firstOrNull { it.isActive }
    }

    override suspend fun getById(id: Long): DefaultProfileEntity? = profiles.firstOrNull { it.id == id }

    override suspend fun getDefaultProfile(): DefaultProfileEntity? = profiles.firstOrNull { it.isDefault }

    override suspend fun getFirstProfile(): DefaultProfileEntity? = profiles.firstOrNull()

    override suspend fun count(): Int = profiles.size

    override suspend fun insert(entity: DefaultProfileEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        if (entity.id >= nextId) nextId = entity.id + 1
        profiles.removeAll { it.id == id }
        profiles.add(entity.copy(id = id))
        notifyChanges()
        return id
    }

    override suspend fun update(entity: DefaultProfileEntity) {
        profiles.removeAll { it.id == entity.id }
        profiles.add(entity)
        notifyChanges()
    }

    override suspend fun delete(id: Long) {
        profiles.removeAll { it.id == id && !it.isDefault }
        notifyChanges()
    }

    override suspend fun clearActive() {
        for (i in profiles.indices) {
            profiles[i] = profiles[i].copy(isActive = false)
        }
        notifyChanges()
    }

    override suspend fun setActive(id: Long) {
        for (i in profiles.indices) {
            if (profiles[i].id == id) {
                profiles[i] = profiles[i].copy(isActive = true)
            }
        }
        notifyChanges()
    }
}
