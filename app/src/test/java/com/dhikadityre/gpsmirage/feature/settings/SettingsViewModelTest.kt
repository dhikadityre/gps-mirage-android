package com.dhikadityre.gpsmirage.feature.settings

import com.dhikadityre.gpsmirage.core.location.DeviceLocationProvider
import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.RadiusConfig
import com.dhikadityre.gpsmirage.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSettingsRepository : SettingsRepository {
        val configFlow = MutableStateFlow(RadiusConfig())
        override val radiusConfig: Flow<RadiusConfig> = configFlow

        val appearanceFlow = MutableStateFlow(com.dhikadityre.gpsmirage.core.model.AppearanceConfig())
        override val appearanceConfig: Flow<com.dhikadityre.gpsmirage.core.model.AppearanceConfig> = appearanceFlow

        override suspend fun updateRadiusConfig(config: RadiusConfig) {
            configFlow.value = config
        }

        override suspend fun updateAppearanceConfig(config: com.dhikadityre.gpsmirage.core.model.AppearanceConfig) {
            appearanceFlow.value = config
        }

        override suspend fun updateMapStyle(mapStyle: com.dhikadityre.gpsmirage.core.model.MapLayerStyle) {
            appearanceFlow.value = appearanceFlow.value.copy(mapStyle = mapStyle)
        }

        override suspend fun updateShowStatusBarNotification(show: Boolean) {
            appearanceFlow.value = appearanceFlow.value.copy(showStatusBarNotification = show)
        }

        override suspend fun updateLanguage(language: com.dhikadityre.gpsmirage.core.model.AppLanguage) {
            appearanceFlow.value = appearanceFlow.value.copy(language = language)
        }

        override suspend fun updateThemeMode(themeMode: com.dhikadityre.gpsmirage.core.model.AppThemeMode) {
            appearanceFlow.value = appearanceFlow.value.copy(themeMode = themeMode)
        }
    }

    private class FakeDeviceLocationProvider : DeviceLocationProvider {
        var mockLocation: Coordinate? = Coordinate(-6.1754, 106.8272)
        override suspend fun getCurrentLocation(): Coordinate? = mockLocation
    }

    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var deviceLocationProvider: FakeDeviceLocationProvider
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = FakeSettingsRepository()
        deviceLocationProvider = FakeDeviceLocationProvider()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads config from repository`() = runTest(testDispatcher) {
        viewModel = SettingsViewModel(settingsRepository, deviceLocationProvider)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.radiusConfig.isEnabled)
        assertEquals("-6.213726", state.latitudeInput)
        assertEquals("106.819897", state.longitudeInput)
        assertEquals("500", state.radiusInput)
        assertEquals(500f, state.radiusSliderValue)
    }

    @Test
    fun `onRadiusToggleChanged updates isEnabled and persists`() = runTest(testDispatcher) {
        viewModel = SettingsViewModel(settingsRepository, deviceLocationProvider)
        advanceUntilIdle()

        viewModel.onRadiusToggleChanged(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.radiusConfig.isEnabled)
        assertFalse(settingsRepository.configFlow.value.isEnabled)
    }

    @Test
    fun `onSaveAnchor with valid coordinates updates anchor and persists`() = runTest(testDispatcher) {
        viewModel = SettingsViewModel(settingsRepository, deviceLocationProvider)
        advanceUntilIdle()

        viewModel.onLatitudeChanged("-6.9175")
        viewModel.onLongitudeChanged("107.6191")
        viewModel.onSaveAnchor()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.coordinateError)
        assertEquals(-6.9175, state.radiusConfig.anchor?.latitude ?: 0.0, 0.0001)
        assertEquals(107.6191, state.radiusConfig.anchor?.longitude ?: 0.0, 0.0001)
        assertEquals(-6.9175, settingsRepository.configFlow.value.anchor?.latitude ?: 0.0, 0.0001)
    }

    @Test
    fun `onSaveAnchor with invalid coordinates shows error and does not persist`() = runTest(testDispatcher) {
        viewModel = SettingsViewModel(settingsRepository, deviceLocationProvider)
        advanceUntilIdle()

        viewModel.onLatitudeChanged("120.0") // Invalid latitude
        viewModel.onLongitudeChanged("107.6191")
        viewModel.onSaveAnchor()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.coordinateError)
        // Anchor in repo should remain unchanged
        assertEquals(-6.213726, settingsRepository.configFlow.value.anchor?.latitude ?: 0.0, 0.0001)
    }

    @Test
    fun `onRadiusSliderChanged updates radius and persists`() = runTest(testDispatcher) {
        viewModel = SettingsViewModel(settingsRepository, deviceLocationProvider)
        advanceUntilIdle()

        viewModel.onRadiusSliderChanged(1500f)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("1500", state.radiusInput)
        assertEquals(1500f, state.radiusSliderValue)
        assertEquals(1500.0, state.radiusConfig.radiusMeters, 0.01)
        assertEquals(1500.0, settingsRepository.configFlow.value.radiusMeters, 0.01)
    }

    @Test
    fun `onUseCurrentLocation success auto-fills coordinates and persists`() = runTest(testDispatcher) {
        viewModel = SettingsViewModel(settingsRepository, deviceLocationProvider)
        advanceUntilIdle()

        viewModel.onUseCurrentLocation()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDetectingLocation)
        assertNull(state.coordinateError)
        assertEquals("-6.175400", state.latitudeInput)
        assertEquals("106.827200", state.longitudeInput)
        assertEquals(-6.1754, state.radiusConfig.anchor?.latitude ?: 0.0, 0.0001)
        assertEquals(106.8272, settingsRepository.configFlow.value.anchor?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun `onUseCurrentLocation failure shows error message`() = runTest(testDispatcher) {
        deviceLocationProvider.mockLocation = null
        viewModel = SettingsViewModel(settingsRepository, deviceLocationProvider)
        advanceUntilIdle()

        viewModel.onUseCurrentLocation()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDetectingLocation)
        assertNotNull(state.coordinateError)
    }
}
