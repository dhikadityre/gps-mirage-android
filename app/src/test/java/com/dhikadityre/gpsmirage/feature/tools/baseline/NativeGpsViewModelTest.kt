package com.dhikadityre.gpsmirage.feature.tools.baseline

import com.dhikadityre.gpsmirage.core.model.AnchorWithMocks
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.LocationType
import com.dhikadityre.gpsmirage.domain.baseline.NativeLocationData
import com.dhikadityre.gpsmirage.domain.baseline.NativeLocationProvider
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NativeGpsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val locationFlow = MutableSharedFlow<NativeLocationData>()
    private var lastKnownLocation: NativeLocationData? = null
    val savedLocations = mutableListOf<Location>()

    private val fakeNativeLocationProvider = object : NativeLocationProvider {
        override val locationFlow: Flow<NativeLocationData> = this@NativeGpsViewModelTest.locationFlow
        override suspend fun getLastKnownLocation(): NativeLocationData? = lastKnownLocation
    }

    private val fakeLocationRepository = object : LocationRepository {
        override fun getAllFlow(): Flow<List<Location>> = emptyFlow()
        override fun getFavoritesFlow(): Flow<List<Location>> = emptyFlow()
        override fun getByFolderFlow(folderId: Long): Flow<List<Location>> = emptyFlow()
        override fun getByTypeFlow(type: LocationType): Flow<List<Location>> = emptyFlow()
        override fun getChildrenOfAnchorFlow(anchorId: Long): Flow<List<Location>> = emptyFlow()
        override fun getAnchorsWithMocksFlow(): Flow<List<AnchorWithMocks>> = emptyFlow()
        override suspend fun getAll(): List<Location> = emptyList()
        override suspend fun getById(id: Long): Location? = null
        override suspend fun getByType(type: LocationType): List<Location> = emptyList()
        override suspend fun getChildrenOfAnchor(anchorId: Long): List<Location> = emptyList()
        override suspend fun getFavorites(): List<Location> = emptyList()
        override suspend fun getByFolder(folderId: Long): List<Location> = emptyList()
        override suspend fun getRecent(limit: Int): List<Location> = emptyList()
        override suspend fun search(query: String): List<Location> = emptyList()
        override suspend fun save(location: Location): Long {
            savedLocations.add(location)
            return savedLocations.size.toLong()
        }
        override suspend fun saveAll(locations: List<Location>): List<Long> = emptyList()
        override suspend fun update(location: Location) {}
        override suspend fun updateFavorite(id: Long, isFavorite: Boolean) {}
        override suspend fun updateFolder(id: Long, folderId: Long?) {}
        override suspend fun updateParentAnchor(id: Long, parentAnchorId: Long?) {}
        override suspend fun delete(id: Long) {}
        override suspend fun isDuplicate(name: String, latitude: Double, longitude: Double): Boolean = false
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        lastKnownLocation = null
        savedLocations.clear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): NativeGpsViewModel {
        return NativeGpsViewModel(
            nativeLocationProvider = fakeNativeLocationProvider,
            locationRepository = fakeLocationRepository
        )
    }

    @Test
    fun `initial state has isAcquiring true and null coordinates when no last known location`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAcquiring)
        assertNull(state.latitude)
        assertNull(state.longitude)
        assertEquals(500.0, state.radiusMeters, 0.001)
        assertFalse(state.showSaveDialog)
    }

    @Test
    fun `initial state loads last known location if available`() = runTest(testDispatcher) {
        lastKnownLocation = NativeLocationData(
            latitude = -6.214620,
            longitude = 106.845130,
            accuracy = 8.5f,
            altitude = 25.0,
            speed = 12.0f,
            bearing = 180.0f
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAcquiring)
        assertEquals(-6.214620, state.latitude!!, 0.000001)
        assertEquals(106.845130, state.longitude!!, 0.000001)
        assertEquals(8.5f, state.accuracy, 0.01f)
        assertEquals(25.0, state.altitude, 0.01)
        assertEquals(12.0f, state.speed, 0.01f)
        assertEquals(180.0f, state.bearing, 0.01f)
    }

    @Test
    fun `when location flow emits update, state updates with accurate telemetry values`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        locationFlow.emit(
            NativeLocationData(
                latitude = -6.175392,
                longitude = 106.827153,
                accuracy = 3.5f,
                altitude = 15.2,
                speed = 24.5f,
                bearing = 95.0f
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAcquiring)
        assertEquals(-6.175392, state.latitude!!, 0.000001)
        assertEquals(106.827153, state.longitude!!, 0.000001)
        assertEquals(3.5f, state.accuracy, 0.01f)
        assertEquals(15.2, state.altitude, 0.01)
        assertEquals(24.5f, state.speed, 0.01f)
        assertEquals(95.0f, state.bearing, 0.01f)
    }

    @Test
    fun `onRadiusChanged updates radiusMeters in state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onRadiusChanged(1200.0)
        assertEquals(1200.0, viewModel.uiState.value.radiusMeters, 0.001)
    }

    @Test
    fun `onOpenSaveDialog sets error if coordinates are null`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onOpenSaveDialog()
        assertFalse(viewModel.uiState.value.showSaveDialog)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onOpenSaveDialog opens dialog with default name when coordinates exist`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        locationFlow.emit(
            NativeLocationData(
                latitude = -6.175392,
                longitude = 106.827153,
                accuracy = 5.0f,
                altitude = 10.0,
                speed = 0f,
                bearing = 0f
            )
        )
        advanceUntilIdle()

        viewModel.onOpenSaveDialog()
        assertTrue(viewModel.uiState.value.showSaveDialog)
        assertTrue(viewModel.uiState.value.saveLocationName.startsWith("Native GPS"))
    }

    @Test
    fun `onSaveLocationNameChanged and onDismissSaveDialog work properly`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        locationFlow.emit(
            NativeLocationData(
                latitude = -6.175392,
                longitude = 106.827153,
                accuracy = 5.0f,
                altitude = 10.0,
                speed = 0f,
                bearing = 0f
            )
        )
        advanceUntilIdle()

        viewModel.onOpenSaveDialog()
        viewModel.onSaveLocationNameChanged("Monas Hardware Real Point")
        assertEquals("Monas Hardware Real Point", viewModel.uiState.value.saveLocationName)

        viewModel.onDismissSaveDialog()
        assertFalse(viewModel.uiState.value.showSaveDialog)
    }

    @Test
    fun `onConfirmSaveLocation saves location with LocationType NATIVE and notifies user`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        locationFlow.emit(
            NativeLocationData(
                latitude = -6.175392,
                longitude = 106.827153,
                accuracy = 4.0f,
                altitude = 18.0,
                speed = 5.0f,
                bearing = 45.0f
            )
        )
        advanceUntilIdle()

        viewModel.onRadiusChanged(800.0)
        viewModel.onOpenSaveDialog()
        viewModel.onSaveLocationNameChanged("Monas Native Landmark")
        viewModel.onConfirmSaveLocation()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showSaveDialog)
        assertTrue(viewModel.uiState.value.isSaved)
        assertNotNull(viewModel.uiState.value.message)

        assertEquals(1, savedLocations.size)
        val saved = savedLocations.first()
        assertEquals("Monas Native Landmark", saved.name)
        assertEquals(-6.175392, saved.latitude, 0.000001)
        assertEquals(106.827153, saved.longitude, 0.000001)
        assertEquals(LocationType.NATIVE, saved.type)
        assertEquals(800.0, saved.radiusMeters!!, 0.001)
        assertEquals(4.0f, saved.accuracy, 0.01f)
        assertEquals(18.0, saved.altitude, 0.01)
        assertEquals(5.0f, saved.speed, 0.01f)
        assertEquals(45.0f, saved.bearing, 0.01f)
    }
}
