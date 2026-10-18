package com.dhikadityre.gpsmirage.feature.tools

import com.dhikadityre.gpsmirage.core.model.AppearanceConfig
import com.dhikadityre.gpsmirage.core.model.AppLanguage
import com.dhikadityre.gpsmirage.core.model.AppThemeMode
import com.dhikadityre.gpsmirage.core.model.MapLayerStyle
import com.dhikadityre.gpsmirage.core.model.RadiusConfig
import com.dhikadityre.gpsmirage.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSettingsRepository : SettingsRepository {
        private val _radiusConfig = MutableStateFlow(RadiusConfig())
        override val radiusConfig: Flow<RadiusConfig> = _radiusConfig.asStateFlow()

        private val _appearanceConfig = MutableStateFlow(AppearanceConfig())
        override val appearanceConfig: Flow<AppearanceConfig> = _appearanceConfig.asStateFlow()

        override suspend fun updateRadiusConfig(config: RadiusConfig) {
            _radiusConfig.value = config
        }

        override suspend fun updateAppearanceConfig(config: AppearanceConfig) {
            _appearanceConfig.value = config
        }

        override suspend fun updateMapStyle(mapStyle: MapLayerStyle) {
            _appearanceConfig.value = _appearanceConfig.value.copy(mapStyle = mapStyle)
        }

        override suspend fun updateShowStatusBarNotification(show: Boolean) {
            _appearanceConfig.value = _appearanceConfig.value.copy(showStatusBarNotification = show)
        }

        override suspend fun updateLanguage(language: AppLanguage) {
            _appearanceConfig.value = _appearanceConfig.value.copy(language = language)
        }

        override suspend fun updateThemeMode(themeMode: AppThemeMode) {
            _appearanceConfig.value = _appearanceConfig.value.copy(themeMode = themeMode)
        }
    }

    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var viewModel: AppearanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = FakeSettingsRepository()
        viewModel = AppearanceViewModel(settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state emits default appearance config`() = runTest(testDispatcher) {
        advanceUntilIdle()
        val config = viewModel.uiState.value.config
        assertEquals(MapLayerStyle.GOOGLE_ROADMAP, config.mapStyle)
        assertTrue(config.showStatusBarNotification)
        assertEquals(AppLanguage.ENGLISH, config.language)
        assertEquals(AppThemeMode.SYSTEM, config.themeMode)
    }

    @Test
    fun `setMapStyle updates map style in repository and state`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.setMapStyle(MapLayerStyle.GOOGLE_SATELLITE)
        advanceUntilIdle()

        assertEquals(MapLayerStyle.GOOGLE_SATELLITE, viewModel.uiState.value.config.mapStyle)
    }

    @Test
    fun `setShowStatusBarNotification updates notification icon status`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.setShowStatusBarNotification(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.config.showStatusBarNotification)
    }

    @Test
    fun `setLanguage updates language in repository and state`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.setLanguage(AppLanguage.ENGLISH)
        advanceUntilIdle()

        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.config.language)
    }

    @Test
    fun `setThemeMode updates theme in repository and state`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.setThemeMode(AppThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(AppThemeMode.DARK, viewModel.uiState.value.config.themeMode)
    }

    @Test
    fun `AppLanguage resolves Indonesian for id or in system locale`() {
        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale("id", "ID"))
            assertEquals(AppLanguage.INDONESIAN, AppLanguage.fromCode(null))

            java.util.Locale.setDefault(java.util.Locale("in", "ID"))
            assertEquals(AppLanguage.INDONESIAN, AppLanguage.fromCode(null))
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `AppLanguage defaults to English for English or any non-Indonesian language`() {
        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.ENGLISH)
            assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null))

            java.util.Locale.setDefault(java.util.Locale.FRENCH)
            assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null))

            java.util.Locale.setDefault(java.util.Locale.JAPANESE)
            assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null))
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `AppLanguage fromCode respects persisted code regardless of system locale`() {
        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.FRENCH)
            assertEquals(AppLanguage.INDONESIAN, AppLanguage.fromCode("id"))
            assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }
}

