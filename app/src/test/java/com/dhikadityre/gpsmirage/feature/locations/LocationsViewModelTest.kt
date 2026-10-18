package com.dhikadityre.gpsmirage.feature.locations

import com.dhikadityre.gpsmirage.core.geo.GeoEngineImpl
import com.dhikadityre.gpsmirage.core.model.AnchorWithMocks
import com.dhikadityre.gpsmirage.core.model.Folder
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.LocationType
import com.dhikadityre.gpsmirage.domain.folder.FolderRepository
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import com.dhikadityre.gpsmirage.domain.location.MapPreviewCoordinator
import com.dhikadityre.gpsmirage.domain.location.MockLocationManager
import com.dhikadityre.gpsmirage.domain.location.MockStartResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class LocationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val geoEngine = GeoEngineImpl()

    private val anchorsFlow = MutableStateFlow<List<Location>>(emptyList())
    private val mockTargetsFlow = MutableStateFlow<List<Location>>(emptyList())
    private val groupedFlow = MutableStateFlow<List<AnchorWithMocks>>(emptyList())
    private val foldersFlow = MutableStateFlow<List<Folder>>(emptyList())

    var lastUpdatedParentAnchorId: Pair<Long, Long?>? = null

    private val fakeLocationRepository = object : LocationRepository {
        override fun getAllFlow(): Flow<List<Location>> = MutableStateFlow(emptyList())
        override fun getFavoritesFlow(): Flow<List<Location>> = MutableStateFlow(emptyList())
        override fun getByFolderFlow(folderId: Long): Flow<List<Location>> = MutableStateFlow(emptyList())
        override fun getByTypeFlow(type: LocationType): Flow<List<Location>> =
            if (type == LocationType.CENTER_ANCHOR) anchorsFlow else mockTargetsFlow

        override fun getChildrenOfAnchorFlow(anchorId: Long): Flow<List<Location>> = MutableStateFlow(emptyList())
        override fun getAnchorsWithMocksFlow(): Flow<List<AnchorWithMocks>> = groupedFlow
        override suspend fun getAll(): List<Location> = emptyList()
        override suspend fun getById(id: Long): Location? = null
        override suspend fun getByType(type: LocationType): List<Location> =
            if (type == LocationType.CENTER_ANCHOR) anchorsFlow.value else mockTargetsFlow.value

        override suspend fun getChildrenOfAnchor(anchorId: Long): List<Location> = emptyList()
        override suspend fun getFavorites(): List<Location> = emptyList()
        override suspend fun getByFolder(folderId: Long): List<Location> = emptyList()
        override suspend fun getRecent(limit: Int): List<Location> = emptyList()
        override suspend fun search(query: String): List<Location> = emptyList()
        override suspend fun save(location: Location): Long = 1L
        override suspend fun saveAll(locations: List<Location>): List<Long> = emptyList()
        override suspend fun update(location: Location) {}
        override suspend fun updateFavorite(id: Long, isFavorite: Boolean) {}
        override suspend fun updateFolder(id: Long, folderId: Long?) {}
        override suspend fun updateParentAnchor(id: Long, parentAnchorId: Long?) {
            lastUpdatedParentAnchorId = id to parentAnchorId
        }
        override suspend fun delete(id: Long) {}
        override suspend fun isDuplicate(name: String, latitude: Double, longitude: Double): Boolean = false
    }

    private val fakeFolderRepository = object : FolderRepository {
        override fun getAllFlow(): Flow<List<Folder>> = foldersFlow
        override suspend fun getAll(): List<Folder> = foldersFlow.value
        override suspend fun getById(id: Long): Folder? = null
        override suspend fun create(folder: Folder): Long = 1L
        override suspend fun rename(id: Long, name: String) {}
        override suspend fun delete(id: Long) {}
        override suspend fun getLocationCount(folderId: Long): Int = 0
    }


    private val fakeMockLocationManager = object : MockLocationManager {
        override val isRunning: StateFlow<Boolean> = MutableStateFlow(false)
        override val activeLocation: StateFlow<Location?> = MutableStateFlow(null)
        override fun start(location: Location): MockStartResult = MockStartResult.Success
        override fun stop() {}
        override fun isAppSetAsMockProvider(): Boolean = true
    }

    private lateinit var mapPreviewCoordinator: MapPreviewCoordinator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mapPreviewCoordinator = MapPreviewCoordinator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): LocationsViewModel {
        return LocationsViewModel(
            locationRepository = fakeLocationRepository,
            folderRepository = fakeFolderRepository,
            mockLocationManager = fakeMockLocationManager,
            mapPreviewCoordinator = mapPreviewCoordinator,
            geoEngine = geoEngine
        )
    }

    @Test
    fun `tab selection changes state correctly`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(LocationsTab.ANCHORS, viewModel.uiState.value.selectedTab)

        viewModel.onTabSelected(LocationsTab.MOCK_TARGETS)
        assertEquals(LocationsTab.MOCK_TARGETS, viewModel.uiState.value.selectedTab)

        viewModel.onTabSelected(LocationsTab.GROUPED)
        assertEquals(LocationsTab.GROUPED, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `detects duplicate coordinates between Mock and Anchor and populates duplicateAnchorMap`() = runTest(testDispatcher) {
        val anchor = Location(
            id = 1L,
            name = "Sudirman Anchor",
            latitude = -6.213726,
            longitude = 106.819897,
            type = LocationType.CENTER_ANCHOR,
            radiusMeters = 500.0
        )
        val duplicateMock = Location(
            id = 2L,
            name = "Office Sudirman Mock",
            latitude = -6.213726,
            longitude = 106.819897,
            type = LocationType.MOCK_TARGET
        )
        val nonDuplicateMock = Location(
            id = 3L,
            name = "Bandung Mock",
            latitude = -6.917464,
            longitude = 107.619123,
            type = LocationType.MOCK_TARGET
        )

        anchorsFlow.value = listOf(anchor)
        mockTargetsFlow.value = listOf(duplicateMock, nonDuplicateMock)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val duplicates = viewModel.uiState.value.duplicateAnchorMap
        assertTrue("Duplicate mock must be flagged", duplicates.containsKey(2L))
        assertEquals("Sudirman Anchor", duplicates[2L]?.name)
        assertTrue("Non-duplicate mock must not be in map", !duplicates.containsKey(3L))
    }

    @Test
    fun `computeDuplicates ignores mock if it is already linked to that matching anchor`() = runTest(testDispatcher) {
        val anchor = Location(
            id = 1L,
            name = "Sudirman Anchor",
            latitude = -6.208763,
            longitude = 106.845599,
            type = LocationType.CENTER_ANCHOR,
            radiusMeters = 500.0
        )
        val linkedMock = Location(
            id = 2L,
            name = "Sudirman Mock Linked",
            latitude = -6.208763,
            longitude = 106.845599,
            type = LocationType.MOCK_TARGET,
            parentAnchorId = 1L // already linked!
        )

        anchorsFlow.value = listOf(anchor)
        mockTargetsFlow.value = listOf(linkedMock)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val duplicates = viewModel.uiState.value.duplicateAnchorMap
        assertTrue("Already linked mock must not be flagged as duplicate", duplicates.isEmpty())
    }

    @Test
    fun `onLinkToAnchor delegates to repository updateParentAnchor and sets userMessage`() = runTest(testDispatcher) {
        val anchor = Location(
            id = 99L,
            name = "Target Anchor",
            latitude = -6.1,
            longitude = 106.8,
            type = LocationType.CENTER_ANCHOR
        )
        anchorsFlow.value = listOf(anchor)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onLinkToAnchor(mockId = 42L, anchorId = 99L)
        advanceUntilIdle()

        assertNotNull(lastUpdatedParentAnchorId)
        assertEquals(42L to 99L, lastUpdatedParentAnchorId)
        assertEquals("Mock Target berhasil dikaitkan ke Target Anchor", viewModel.uiState.value.userMessage)

        viewModel.clearUserMessage()
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onPreviewLocation requests preview through MapPreviewCoordinator and triggers navigation`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val mock = Location(
            id = 50L,
            name = "Target Mock",
            latitude = -6.123456,
            longitude = 106.654321,
            type = LocationType.MOCK_TARGET
        )

        var navigated = false
        viewModel.onPreviewLocation(target = mock, parentAnchor = null) {
            navigated = true
        }

        assertTrue(navigated)
        val request = mapPreviewCoordinator.consumePreview()
        assertNotNull(request)
        assertEquals(mock.id, request?.target?.id)
    }
}
