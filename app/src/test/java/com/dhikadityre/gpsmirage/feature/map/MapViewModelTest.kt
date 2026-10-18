package com.dhikadityre.gpsmirage.feature.map

import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.History
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.RadiusConfig
import com.dhikadityre.gpsmirage.domain.history.HistoryRepository
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import com.dhikadityre.gpsmirage.core.geo.ReverseGeocoder
import com.dhikadityre.gpsmirage.core.model.GeocodeResult
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        var lastSavedLocation: Location? = null
        override suspend fun save(location: Location): Long {
            lastSavedLocation = location
            return 1L
        }
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

    private class FakeReverseGeocoder : ReverseGeocoder {
        var resultToReturn: GeocodeResult? = GeocodeResult(shortAddress = "Jl. Sudirman No. 10")
        var delayMs: Long = 0L
        var callsCount = 0
        var lastCoordinate: Coordinate? = null

        override suspend fun getShortAddress(coordinate: Coordinate): GeocodeResult? {
            callsCount++
            lastCoordinate = coordinate
            if (delayMs > 0) {
                kotlinx.coroutines.delay(delayMs)
            }
            return resultToReturn
        }
    }

    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var mockLocationManager: RecordingMockLocationManager
    private lateinit var fakeReverseGeocoder: FakeReverseGeocoder

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSettingsRepository = FakeSettingsRepository()
        mockLocationManager = RecordingMockLocationManager()
        fakeReverseGeocoder = FakeReverseGeocoder()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        reverseGeocoder: ReverseGeocoder = fakeReverseGeocoder
    ): MapViewModel {
        return MapViewModel(
            locationRepository = fakeLocationRepository,
            mockLocationManager = mockLocationManager,
            historyRepository = fakeHistoryRepository,
            settingsRepository = fakeSettingsRepository,
            geoEngine = geoEngine,
            reverseGeocoder = reverseGeocoder
        )
    }

    @Test
    fun `initial state has null selectedCoordinate and observes Mayapada Tower anchor`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("Selected coordinate must be null on initial state", state.selectedCoordinate)
        assertNull("Distance to anchor must be null on initial state", state.distanceToAnchorMeters)
        assertEquals(RadiusConfig.MAYAPADA_TOWER, state.radiusConfig.anchor)
        assertTrue(state.radiusConfig.isEnabled)
    }

    @Test
    fun `initial centering at Mayapada Tower does not prematurely select anchor coordinate`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Simulate initial camera centering scroll event to Mayapada Tower
        val anchor = RadiusConfig.MAYAPADA_TOWER
        viewModel.onMapCenterMoved(anchor.latitude, anchor.longitude)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("Initial centering event at anchor must be guarded and remain null", state.selectedCoordinate)
        assertNull(state.distanceToAnchorMeters)
    }

    @Test
    fun `intentional pan away from anchor triggers coordinate selection and radius validation`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Move to a point 100m away from Mayapada Tower
        val testPoint = Coordinate(latitude = -6.214000, longitude = 106.820500)
        viewModel.onMapCenterMoved(testPoint.latitude, testPoint.longitude)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(testPoint, state.selectedCoordinate)
        assertTrue(state.isWithinRadius)
        assertNotNull(state.distanceToAnchorMeters)
    }

    @Test
    fun `clearSelectedCoordinate resets selection back to null`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMapCenterMoved(-6.214000, 106.820500)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.selectedCoordinate)

        viewModel.clearSelectedCoordinate()
        val state = viewModel.uiState.value
        assertNull(state.selectedCoordinate)
        assertNull(state.distanceToAnchorMeters)
        assertNull(state.resolvedShortAddress)
    }

    @Test
    fun `initialState_radiusConfigDefaultsToMayapadaTowerAndEnabled`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val config = viewModel.uiState.value.radiusConfig
        assertTrue(config.isEnabled)
        assertEquals(RadiusConfig.MAYAPADA_TOWER, config.anchor)
        assertEquals(-6.213726, config.anchor?.latitude ?: 0.0, 0.000001)
        assertEquals(106.819897, config.anchor?.longitude ?: 0.0, 0.000001)
        assertEquals(500.0, config.radiusMeters, 0.001)
    }

    @Test
    fun `initialState_selectedCoordinateRemainsNullUntilUserInteraction`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.selectedCoordinate)
        assertFalse(state.isMockActive)
        assertEquals(0, mockLocationManager.startCallCount)

        // Attempting to start mock on unselected state does nothing
        viewModel.onStartMock()
        advanceUntilIdle()
        assertEquals(0, mockLocationManager.startCallCount)
        assertFalse(viewModel.uiState.value.isMockActive)
    }

    @Test
    fun `selectingPointWithinMayapadaRadius_marksAsValidAndComputesDistance`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Test coordinate inside 500m radius of Mayapada Tower (~130m away)
        val validPoint = Coordinate(latitude = -6.2145, longitude = 106.8190)
        viewModel.onMapCenterMoved(validPoint.latitude, validPoint.longitude)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(validPoint, state.selectedCoordinate)
        assertTrue(state.isWithinRadius)
        assertNotNull(state.distanceToAnchorMeters)
        assertTrue("Distance should be under 500m", state.distanceToAnchorMeters!! <= 500.0)

        // Starting mock succeeds
        viewModel.onStartMock()
        advanceUntilIdle()
        assertEquals(1, mockLocationManager.startCallCount)
        assertTrue(viewModel.uiState.value.isMockActive)
    }

    @Test
    fun `selectingPointOutsideMayapadaRadius_marksAsOutOfRadiusAndPreventsMock`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Point outside 500m radius of Mayapada Tower (~1.4km away)
        val outsidePoint = Coordinate(latitude = -6.2270, longitude = 106.819897)
        viewModel.onMapCenterMoved(outsidePoint.latitude, outsidePoint.longitude)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(outsidePoint, state.selectedCoordinate)
        assertFalse(state.isWithinRadius)
        assertNotNull(state.distanceToAnchorMeters)
        assertTrue("Distance should exceed 500m", state.distanceToAnchorMeters!! > 500.0)

        // Starting mock must be blocked
        viewModel.onStartMock()
        advanceUntilIdle()
        assertEquals(0, mockLocationManager.startCallCount)
        assertFalse(viewModel.uiState.value.isMockActive)
        val event = viewModel.uiState.value.event
        assertTrue("Expected ShowSnackbar event", event is MapEvent.ShowSnackbar)
        assertEquals("Lokasi berada di luar batas radius", (event as MapEvent.ShowSnackbar).message)
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
        fakeSettingsRepository.updateRadiusConfig(RadiusConfig(isEnabled = false))
        viewModel.setSelectionMode(MapSelectionMode.TAP_TO_SELECT)
        advanceUntilIdle()

        // Radius explicitly disabled
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
        viewModel.setSelectionMode(MapSelectionMode.TAP_TO_SELECT)
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
        viewModel.setSelectionMode(MapSelectionMode.TAP_TO_SELECT)
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
        viewModel.setSelectionMode(MapSelectionMode.TAP_TO_SELECT)
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
        viewModel.setSelectionMode(MapSelectionMode.TAP_TO_SELECT)
        val anchor = Coordinate(latitude = -6.2088, longitude = 106.8456)
        fakeSettingsRepository.updateRadiusConfig(RadiusConfig(isEnabled = false))
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

    @Test
    fun `onMapTap sets isResolvingAddress and resolves address into state`() = runTest(testDispatcher) {
        fakeReverseGeocoder.resultToReturn = GeocodeResult(shortAddress = "Jl. Jenderal Sudirman No. 10")
        fakeReverseGeocoder.delayMs = 100L
        val viewModel = createViewModel()
        viewModel.setSelectionMode(MapSelectionMode.TAP_TO_SELECT)

        viewModel.onMapTap(-6.2088, 106.8456)

        assertEquals(true, viewModel.uiState.value.isResolvingAddress)
        assertEquals(null, viewModel.uiState.value.resolvedShortAddress)
        assertEquals("", viewModel.uiState.value.selectedLocationName)

        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isResolvingAddress)
        assertEquals("Jl. Jenderal Sudirman No. 10", viewModel.uiState.value.resolvedShortAddress)
        assertEquals("Jl. Jenderal Sudirman No. 10", viewModel.uiState.value.selectedLocationName)
    }

    @Test
    fun `onMapTap handles geocoder null result safely`() = runTest(testDispatcher) {
        fakeReverseGeocoder.resultToReturn = null
        val viewModel = createViewModel()
        viewModel.setSelectionMode(MapSelectionMode.TAP_TO_SELECT)

        viewModel.onMapTap(-6.2088, 106.8456)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isResolvingAddress)
        assertNull(viewModel.uiState.value.resolvedShortAddress)
        assertEquals("", viewModel.uiState.value.selectedLocationName)
    }

    @Test
    fun `rapid consecutive onMapTap calls cancel in-flight resolution and only apply latest`() = runTest(testDispatcher) {
        val customGeocoder = object : ReverseGeocoder {
            override suspend fun getShortAddress(coordinate: Coordinate): GeocodeResult? {
                if (coordinate.latitude == -6.1) {
                    kotlinx.coroutines.delay(500L)
                    return GeocodeResult(shortAddress = "Old Street")
                } else {
                    kotlinx.coroutines.delay(100L)
                    return GeocodeResult(shortAddress = "Latest Street")
                }
            }
        }
        val viewModel = createViewModel(reverseGeocoder = customGeocoder)
        viewModel.setSelectionMode(MapSelectionMode.TAP_TO_SELECT)

        viewModel.onMapTap(-6.1, 106.1)
        viewModel.onMapTap(-6.2, 106.2)

        advanceUntilIdle()

        assertEquals(Coordinate(-6.2, 106.2), viewModel.uiState.value.selectedCoordinate)
        assertEquals("Latest Street", viewModel.uiState.value.resolvedShortAddress)
        assertEquals("Latest Street", viewModel.uiState.value.selectedLocationName)
        assertEquals(false, viewModel.uiState.value.isResolvingAddress)
    }

    @Test
    fun `initial state defaults to CENTER_PIN selectionMode and unlocked position`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(MapSelectionMode.CENTER_PIN, viewModel.uiState.value.selectionMode)
        assertFalse(viewModel.uiState.value.isPositionLocked)
    }

    @Test
    fun `togglePositionLock flips isPositionLocked state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPositionLocked)
        viewModel.togglePositionLock()
        assertTrue(viewModel.uiState.value.isPositionLocked)
        viewModel.togglePositionLock()
        assertFalse(viewModel.uiState.value.isPositionLocked)
    }

    @Test
    fun `setSelectionMode switches selectionMode state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(MapSelectionMode.CENTER_PIN, viewModel.uiState.value.selectionMode)
        viewModel.setSelectionMode(MapSelectionMode.TAP_TO_SELECT)
        assertEquals(MapSelectionMode.TAP_TO_SELECT, viewModel.uiState.value.selectionMode)
        viewModel.setSelectionMode(MapSelectionMode.CENTER_PIN)
        assertEquals(MapSelectionMode.CENTER_PIN, viewModel.uiState.value.selectionMode)
    }

    @Test
    fun `in CENTER_PIN mode, onMapCenterMoved updates selectedCoordinate and calculates radius`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val anchor = Coordinate(latitude = -6.2088, longitude = 106.8456)
        fakeSettingsRepository.updateRadiusConfig(
            RadiusConfig(isEnabled = true, anchor = anchor, radiusMeters = 500.0)
        )
        advanceUntilIdle()

        val pointInside = Coordinate(latitude = -6.2078, longitude = 106.8456)
        viewModel.onMapCenterMoved(pointInside.latitude, pointInside.longitude)

        // Coordinates and radius are updated immediately
        assertEquals(pointInside, viewModel.uiState.value.selectedCoordinate)
        assertTrue(viewModel.uiState.value.isWithinRadius)
        assertTrue(viewModel.uiState.value.isResolvingAddress)

        // Address resolution completes after debounce + geocoding
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isResolvingAddress)
        assertEquals("Jl. Sudirman No. 10", viewModel.uiState.value.resolvedShortAddress)
    }

    @Test
    fun `in CENTER_PIN mode, rapid onMapCenterMoved debounces reverse geocoder lookup`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMapCenterMoved(-6.1001, 106.8001)
        viewModel.onMapCenterMoved(-6.1002, 106.8002)
        viewModel.onMapCenterMoved(-6.1003, 106.8003)

        // Before debounce timer expires, geocoder callsCount should be 0
        assertEquals(0, fakeReverseGeocoder.callsCount)

        advanceUntilIdle()

        // After settling, only 1 geocoder call should have been made for the final coordinate
        assertEquals(1, fakeReverseGeocoder.callsCount)
        assertEquals(Coordinate(-6.1003, 106.8003), fakeReverseGeocoder.lastCoordinate)
        assertEquals(Coordinate(-6.1003, 106.8003), viewModel.uiState.value.selectedCoordinate)
    }

    @Test
    fun `when position is locked, onMapCenterMoved is ignored`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val initialCoord = Coordinate(-6.2088, 106.8456)
        viewModel.onMapCenterMoved(initialCoord.latitude, initialCoord.longitude)
        advanceUntilIdle()
        assertEquals(initialCoord, viewModel.uiState.value.selectedCoordinate)

        viewModel.togglePositionLock()
        assertTrue(viewModel.uiState.value.isPositionLocked)

        // Attempt to move center while locked
        viewModel.onMapCenterMoved(-6.3000, 106.9000)
        advanceUntilIdle()

        // Coordinate must remain initialCoord
        assertEquals(initialCoord, viewModel.uiState.value.selectedCoordinate)
    }

    @Test
    fun `when in TAP_TO_SELECT mode, onMapCenterMoved is ignored and onMapTap updates coordinate`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.setSelectionMode(MapSelectionMode.TAP_TO_SELECT)
        advanceUntilIdle()

        // onMapCenterMoved should be ignored
        viewModel.onMapCenterMoved(-6.1111, 106.1111)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedCoordinate)

        // onMapTap should work
        val tapCoord = Coordinate(-6.2222, 106.2222)
        viewModel.onMapTap(tapCoord.latitude, tapCoord.longitude)
        advanceUntilIdle()
        assertEquals(tapCoord, viewModel.uiState.value.selectedCoordinate)
    }

    @Test
    fun `when in TAP_TO_SELECT mode, tapping directly on anchor coordinate is ignored`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.setSelectionMode(MapSelectionMode.TAP_TO_SELECT)
        advanceUntilIdle()

        // Attempt to tap directly on Mayapada Tower anchor
        val anchor = RadiusConfig.MAYAPADA_TOWER
        viewModel.onMapTap(anchor.latitude, anchor.longitude)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("Tapping on the center anchor must not set it as a pinpoint", state.selectedCoordinate)
    }

    @Test
    fun `when in CENTER_PIN mode, onMapTap is ignored`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(MapSelectionMode.CENTER_PIN, viewModel.uiState.value.selectionMode)

        viewModel.onMapTap(-6.5555, 106.5555)
        advanceUntilIdle()

        // Coordinate must remain null because taps are ignored in CENTER_PIN mode
        assertNull(viewModel.uiState.value.selectedCoordinate)
    }

    @Test
    fun `when position is locked in TAP_TO_SELECT mode, onMapTap is ignored`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.setSelectionMode(MapSelectionMode.TAP_TO_SELECT)
        val initialCoord = Coordinate(-6.2088, 106.8456)
        viewModel.onMapTap(initialCoord.latitude, initialCoord.longitude)
        advanceUntilIdle()
        assertEquals(initialCoord, viewModel.uiState.value.selectedCoordinate)

        viewModel.togglePositionLock()
        assertTrue(viewModel.uiState.value.isPositionLocked)

        viewModel.onMapTap(-6.9999, 106.9999)
        advanceUntilIdle()

        assertEquals(initialCoord, viewModel.uiState.value.selectedCoordinate)
    }

    @Test
    fun `onSaveLocation with custom name saves location and triggers snackbar`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val coord = Coordinate(-6.2140, 106.8200)
        viewModel.onMapCenterMoved(coord.latitude, coord.longitude)
        advanceUntilIdle()

        viewModel.onSaveLocation("Kantor Sudirman")
        advanceUntilIdle()

        assertEquals("Kantor Sudirman", fakeLocationRepository.lastSavedLocation?.name)
        assertEquals(coord.latitude, fakeLocationRepository.lastSavedLocation?.latitude ?: 0.0, 0.000001)
        assertEquals(coord.longitude, fakeLocationRepository.lastSavedLocation?.longitude ?: 0.0, 0.000001)
        assertTrue(viewModel.uiState.value.event is MapEvent.ShowSnackbar)
        assertEquals("Location saved: Kantor Sudirman", (viewModel.uiState.value.event as MapEvent.ShowSnackbar).message)
    }

    @Test
    fun `onSaveLocation with blank name falls back to Unnamed Location`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val coord = Coordinate(-6.2140, 106.8200)
        viewModel.onMapCenterMoved(coord.latitude, coord.longitude)
        advanceUntilIdle()

        viewModel.onSaveLocation("   ")
        advanceUntilIdle()

        assertEquals("Unnamed Location", fakeLocationRepository.lastSavedLocation?.name)
        assertTrue(viewModel.uiState.value.event is MapEvent.ShowSnackbar)
        assertEquals("Location saved: Unnamed Location", (viewModel.uiState.value.event as MapEvent.ShowSnackbar).message)
    }
}

