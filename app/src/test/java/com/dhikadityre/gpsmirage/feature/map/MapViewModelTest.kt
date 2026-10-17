package com.dhikadityre.gpsmirage.feature.map

import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.History
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.RadiusConfig
import com.dhikadityre.gpsmirage.domain.history.HistoryRepository
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import com.dhikadityre.gpsmirage.domain.location.MockLocationManager
import com.dhikadityre.gpsmirage.domain.location.MockStartResult
import com.dhikadityre.gpsmirage.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeLocationRepository = object : LocationRepository {
        override fun getAllFlow(): Flow<List<Location>> = emptyFlow()
        override fun getFavoritesFlow(): Flow<List<Location>> = emptyFlow()
        override fun getByFolderFlow(folderId: Long): Flow<List<Location>> = emptyFlow()
        override suspend fun getAll(): List<Location> = emptyList()
        override suspend fun getById(id: Long): Location? = null
        override suspend fun getFavorites(): List<Location> = emptyList()
        override suspend fun getByFolder(folderId: Long): List<Location> = emptyList()
        override suspend fun getRecent(limit: Int): List<Location> = emptyList()
        override suspend fun search(query: String): List<Location> = emptyList()
        override suspend fun save(location: Location): Long = 1L
        override suspend fun saveAll(locations: List<Location>): List<Long> = emptyList()
        override suspend fun update(location: Location) {}
        override suspend fun updateFavorite(id: Long, isFavorite: Boolean) {}
        override suspend fun updateFolder(id: Long, folderId: Long?) {}
        override suspend fun delete(id: Long) {}
        override suspend fun isDuplicate(name: String, latitude: Double, longitude: Double): Boolean = false
    }

    private val fakeMockLocationManager = object : MockLocationManager {
        private val _isRunning = MutableStateFlow(false)
        override val isRunning: StateFlow<Boolean> = _isRunning
        private val _activeLocation = MutableStateFlow<Location?>(null)
        override val activeLocation: StateFlow<Location?> = _activeLocation
        override fun start(location: Location): MockStartResult = MockStartResult.Success
        override fun stop() {}
        override fun isAppSetAsMockProvider(): Boolean = true
    }

    private val fakeHistoryRepository = object : HistoryRepository {
        override fun getAllFlow(): Flow<List<History>> = emptyFlow()
        override suspend fun getAll(): List<History> = emptyList()
        override suspend fun getActiveSession(): History? = null
        override suspend fun startSession(history: History): Long = 1L
        override suspend fun stopSession(id: Long, stoppedAt: Long, durationMs: Long) {}
        override suspend fun delete(id: Long) {}
        override suspend fun clearAll() {}
    }

    private class FakeSettingsRepository : SettingsRepository {
        val configFlow = MutableStateFlow(RadiusConfig())
        override val radiusConfig: Flow<RadiusConfig> = configFlow

        override suspend fun updateRadiusConfig(config: RadiusConfig) {
            configFlow.value = config
        }
    }

    private lateinit var fakeSettingsRepository: FakeSettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSettingsRepository = FakeSettingsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `mapUiState observes radiusConfig from SettingsRepository`() = runTest(testDispatcher) {
        val viewModel = MapViewModel(
            locationRepository = fakeLocationRepository,
            mockLocationManager = fakeMockLocationManager,
            historyRepository = fakeHistoryRepository,
            settingsRepository = fakeSettingsRepository
        )

        advanceUntilIdle()
        assertEquals(RadiusConfig(), viewModel.uiState.value.radiusConfig)

        val updated = RadiusConfig(
            isEnabled = true,
            anchor = Coordinate(latitude = -6.2088, longitude = 106.8456),
            radiusMeters = 1200.0
        )
        fakeSettingsRepository.updateRadiusConfig(updated)
        advanceUntilIdle()

        assertEquals(updated, viewModel.uiState.value.radiusConfig)
        assertTrue(viewModel.uiState.value.radiusConfig.isEnabled)
        assertEquals(1200.0, viewModel.uiState.value.radiusConfig.radiusMeters, 0.001)
    }
}
