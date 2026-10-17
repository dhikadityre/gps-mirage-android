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

import com.dhikadityre.gpsmirage.core.geo.GeoEngineImpl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val geoEngine = GeoEngineImpl()

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

    private class RecordingMockLocationManager : MockLocationManager {
        private val _isRunning = MutableStateFlow(false)
        override val isRunning: StateFlow<Boolean> = _isRunning
        private val _activeLocation = MutableStateFlow<Location?>(null)
        override val activeLocation: StateFlow<Location?> = _activeLocation
        var startCallCount = 0
            private set

        override fun start(location: Location): MockStartResult {
            startCallCount++
            _isRunning.value = true
            _activeLocation.value = location
            return MockStartResult.Success
        }

        override fun stop() {
            _isRunning.value = false
            _activeLocation.value = null
        }

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
    private lateinit var mockLocationManager: RecordingMockLocationManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSettingsRepository = FakeSettingsRepository()
        mockLocationManager = RecordingMockLocationManager()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MapViewModel {
        return MapViewModel(
            locationRepository = fakeLocationRepository,
            mockLocationManager = mockLocationManager,
            historyRepository = fakeHistoryRepository,
            settingsRepository = fakeSettingsRepository,
            geoEngine = geoEngine
        )
    }

    @Test
    fun `mapUiState observes radiusConfig from SettingsRepository`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

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

    @Test
    fun `when radius is disabled, selection anywhere is valid and within radius`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Radius disabled by default
        assertFalse(viewModel.uiState.value.radiusConfig.isEnabled)

        viewModel.onMapTap(lat = 37.7749, lng = -122.4194)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(Coordinate(37.7749, -122.4194), state.selectedCoordinate)
        assertTrue(state.isWithinRadius)
        assertNull(state.distanceToAnchorMeters)
    }

    @Test
    fun `when radius is enabled, tap inside radius marks isWithinRadius true and computes distance`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val anchor = Coordinate(latitude = -6.2088, longitude = 106.8456)
        fakeSettingsRepository.updateRadiusConfig(
            RadiusConfig(
                isEnabled = true,
                anchor = anchor,
                radiusMeters = 500.0
            )
        )
        advanceUntilIdle()

        // Point approx ~111m north of anchor
        val pointInside = Coordinate(latitude = -6.2078, longitude = 106.8456)
        viewModel.onMapTap(pointInside.latitude, pointInside.longitude)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(pointInside, state.selectedCoordinate)
        assertTrue(state.isWithinRadius)
        assertTrue(state.distanceToAnchorMeters != null && state.distanceToAnchorMeters!! <= 500.0)
    }

    @Test
    fun `when radius is enabled, tap outside radius marks isWithinRadius false and computes distance`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val anchor = Coordinate(latitude = -6.2088, longitude = 106.8456)
        fakeSettingsRepository.updateRadiusConfig(
            RadiusConfig(
                isEnabled = true,
                anchor = anchor,
                radiusMeters = 500.0
            )
        )
        advanceUntilIdle()

        // Point approx ~2.2km away
        val pointOutside = Coordinate(latitude = -6.1888, longitude = 106.8456)
        viewModel.onMapTap(pointOutside.latitude, pointOutside.longitude)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(pointOutside, state.selectedCoordinate)
        assertFalse(state.isWithinRadius)
        assertTrue(state.distanceToAnchorMeters != null && state.distanceToAnchorMeters!! > 500.0)
    }

    @Test
    fun `when outside radius, onStartMock is rejected and emits warning event`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val anchor = Coordinate(latitude = -6.2088, longitude = 106.8456)
        fakeSettingsRepository.updateRadiusConfig(
            RadiusConfig(
                isEnabled = true,
                anchor = anchor,
                radiusMeters = 500.0
            )
        )
        advanceUntilIdle()

        // Select point outside radius
        val pointOutside = Coordinate(latitude = -6.1888, longitude = 106.8456)
        viewModel.onMapTap(pointOutside.latitude, pointOutside.longitude)
        advanceUntilIdle()

        viewModel.onStartMock()
        advanceUntilIdle()

        // Mock should NOT start
        assertEquals(0, mockLocationManager.startCallCount)
        assertFalse(viewModel.uiState.value.isMockActive)
        val event = viewModel.uiState.value.event
        assertTrue("Expected ShowSnackbar event", event is MapEvent.ShowSnackbar)
    }

    @Test
    fun `when radiusConfig updates dynamically, existing selected coordinate re-evaluates boundary`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val anchor = Coordinate(latitude = -6.2088, longitude = 106.8456)
        advanceUntilIdle()

        // Select point while radius is disabled
        val testPoint = Coordinate(latitude = -6.2000, longitude = 106.8456)
        viewModel.onMapTap(testPoint.latitude, testPoint.longitude)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isWithinRadius)

        // Now enable radius with 500m (distance is ~970m, so it becomes out of radius)
        fakeSettingsRepository.updateRadiusConfig(
            RadiusConfig(
                isEnabled = true,
                anchor = anchor,
                radiusMeters = 500.0
            )
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isWithinRadius)
        assertTrue(viewModel.uiState.value.distanceToAnchorMeters != null)

        // Expand radius to 2000m -> should dynamically become within radius
        fakeSettingsRepository.updateRadiusConfig(
            RadiusConfig(
                isEnabled = true,
                anchor = anchor,
                radiusMeters = 2000.0
            )
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isWithinRadius)
    }
}

