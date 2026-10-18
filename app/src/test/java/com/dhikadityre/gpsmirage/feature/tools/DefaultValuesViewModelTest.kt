package com.dhikadityre.gpsmirage.feature.tools

import com.dhikadityre.gpsmirage.core.database.dao.DefaultProfileDao
import com.dhikadityre.gpsmirage.core.geo.UnitConverter
import com.dhikadityre.gpsmirage.core.model.DefaultValueProfile
import com.dhikadityre.gpsmirage.core.sensor.CompassSensorManager
import com.dhikadityre.gpsmirage.data.repository.DefaultProfileRepositoryImpl
import com.dhikadityre.gpsmirage.data.repository.FakeDefaultProfileDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultValuesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeDefaultProfileDao
    private lateinit var repository: DefaultProfileRepositoryImpl
    private lateinit var compassSensorManager: FakeCompassSensorManager
    private lateinit var viewModel: DefaultValuesViewModel

    class FakeCompassSensorManager(
        var available: Boolean = true,
        var bearing: Float = 45.0f
    ) : CompassSensorManager() {
        override fun isCompassAvailable(): Boolean = available
        override fun observeBearing(): Flow<Float> = flowOf(bearing)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeDefaultProfileDao()
        repository = DefaultProfileRepositoryImpl(fakeDao, testDispatcher)

        compassSensorManager = FakeCompassSensorManager(available = true, bearing = 45.0f)

        viewModel = DefaultValuesViewModel(
            repository = repository,
            compassSensorManager = compassSensorManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads seeded original profile as active`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.profiles.size)
        assertEquals("original", state.activeProfile?.name)
        assertTrue(state.activeProfile?.isActive == true)
        assertEquals(45.0f, state.currentSensorBearing)
        assertTrue(state.isCompassAvailable)
    }

    @Test
    fun `form bidirectional unit conversion syncs meters and feet correctly`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.openCreateDialog()
        var form = viewModel.uiState.value.formState
        assertNotNull(form)

        // Change Accuracy meters -> feet updates
        viewModel.onAccuracyMetersChanged("10.0")
        form = viewModel.uiState.value.formState
        assertEquals("10.0", form?.accuracyMeters)
        assertEquals("32.81", form?.accuracyFeet)

        // Change Accuracy feet -> meters updates (78 ft -> 23.77 m)
        viewModel.onAccuracyFeetChanged("78.0")
        form = viewModel.uiState.value.formState
        assertEquals("78.0", form?.accuracyFeet)
        assertEquals("23.77", form?.accuracyMeters)

        // Change Altitude meters -> feet updates
        viewModel.onAltitudeMetersChanged("100.0")
        form = viewModel.uiState.value.formState
        assertEquals("100.0", form?.altitudeMeters)
        assertEquals("328.08", form?.altitudeFeet)

        // Change Altitude feet -> meters updates (89 ft -> 27.13 m)
        viewModel.onAltitudeFeetChanged("89.0")
        form = viewModel.uiState.value.formState
        assertEquals("89.0", form?.altitudeFeet)
        assertEquals("27.13", form?.altitudeMeters)
    }

    @Test
    fun `saveForm creates new profile and displays user message`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.openCreateDialog()
        viewModel.onNameChanged("Work Profile")
        viewModel.onLatitudeChanged("-6.200000")
        viewModel.onLongitudeChanged("106.800000")
        viewModel.onRadiusChanged("150")
        viewModel.onAccuracyFeetChanged("50")
        viewModel.onAltitudeFeetChanged("30")
        viewModel.onSpeedChanged("10")
        viewModel.onBearingChanged("180")
        viewModel.onUseDeviceSensorToggled(false)

        viewModel.saveForm()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.formState)
        assertEquals(2, state.profiles.size)
        assertNotNull(state.userMessage)
        assertTrue(state.userMessage!!.contains("Work Profile"))
    }

    @Test
    fun `setActiveProfile updates active profile in state`() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Create custom profile
        val custom = DefaultValueProfile(
            id = 0L,
            name = "Park",
            latitude = -6.1,
            longitude = 106.1,
            radiusMeters = 20.0,
            accuracyMeters = 5.0,
            altitudeMeters = 0.0,
            speedKmh = 0.0,
            bearingDegrees = 0.0,
            isActive = false
        )
        val newId = repository.saveProfile(custom)
        advanceUntilIdle()

        viewModel.setActiveProfile(newId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Park", state.activeProfile?.name)
        assertEquals(newId, state.activeProfile?.id)
    }

    @Test
    fun `deleteProfile removes user profile`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val custom = DefaultValueProfile(
            id = 0L,
            name = "Temp",
            latitude = -6.1,
            longitude = 106.1,
            radiusMeters = 20.0,
            accuracyMeters = 5.0,
            altitudeMeters = 0.0,
            speedKmh = 0.0,
            bearingDegrees = 0.0,
            isActive = false
        )
        val id = repository.saveProfile(custom)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.profiles.size)

        viewModel.deleteProfile(id)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.profiles.size)
    }
}
