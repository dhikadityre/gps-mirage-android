package com.dhikadityre.gpsmirage.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.RadiusConfig
import com.dhikadityre.gpsmirage.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var dataStoreScope: CoroutineScope

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(testDispatcher + Job())
        testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { tempFolder.newFile("test_settings.preferences_pb") }
        )
        repository = SettingsRepositoryImpl(testDataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun defaultRadiusConfig_returnsDefaultValues() = testScope.runTest {
        val config = repository.radiusConfig.first()

        assertTrue(config.isEnabled)
        assertEquals(RadiusConfig.MAYAPADA_TOWER, config.anchor)
        assertEquals(500.0, config.radiusMeters, 0.001)
    }

    @Test
    fun updateRadiusConfig_savesAndEmitsNewValues() = testScope.runTest {
        val targetConfig = RadiusConfig(
            isEnabled = true,
            anchor = Coordinate(latitude = -6.2088, longitude = 106.8456),
            radiusMeters = 1500.0
        )

        repository.updateRadiusConfig(targetConfig)

        val updatedConfig = repository.radiusConfig.first()
        assertTrue(updatedConfig.isEnabled)
        assertEquals(-6.2088, updatedConfig.anchor?.latitude ?: 0.0, 0.0001)
        assertEquals(106.8456, updatedConfig.anchor?.longitude ?: 0.0, 0.0001)
        assertEquals(1500.0, updatedConfig.radiusMeters, 0.001)
    }

    @Test
    fun updateRadiusConfig_clearingAnchor_persistsNullAnchor() = testScope.runTest {
        val initialConfig = RadiusConfig(
            isEnabled = true,
            anchor = Coordinate(latitude = -6.2088, longitude = 106.8456),
            radiusMeters = 1000.0
        )
        repository.updateRadiusConfig(initialConfig)

        val clearedAnchorConfig = RadiusConfig(
            isEnabled = false,
            anchor = null,
            radiusMeters = 800.0
        )
        repository.updateRadiusConfig(clearedAnchorConfig)

        val resultConfig = repository.radiusConfig.first()
        assertFalse(resultConfig.isEnabled)
        assertNull(resultConfig.anchor)
        assertEquals(800.0, resultConfig.radiusMeters, 0.001)
    }

    @Test
    fun defaultAppearanceConfig_returnsDefaultValues() = testScope.runTest {
        val config = repository.appearanceConfig.first()

        assertEquals(com.dhikadityre.gpsmirage.core.model.MapLayerStyle.GOOGLE_ROADMAP, config.mapStyle)
        assertTrue(config.showStatusBarNotification)
        assertEquals(com.dhikadityre.gpsmirage.core.model.AppLanguage.INDONESIAN, config.language)
        assertEquals(com.dhikadityre.gpsmirage.core.model.AppThemeMode.SYSTEM, config.themeMode)
    }

    @Test
    fun updateMapStyle_persistsAndEmitsNewStyle() = testScope.runTest {
        repository.updateMapStyle(com.dhikadityre.gpsmirage.core.model.MapLayerStyle.GOOGLE_HYBRID)

        val config = repository.appearanceConfig.first()
        assertEquals(com.dhikadityre.gpsmirage.core.model.MapLayerStyle.GOOGLE_HYBRID, config.mapStyle)
    }

    @Test
    fun updateShowStatusBarNotification_persistsAndEmitsNewValue() = testScope.runTest {
        repository.updateShowStatusBarNotification(false)

        val config = repository.appearanceConfig.first()
        assertFalse(config.showStatusBarNotification)
    }

    @Test
    fun updateLanguage_persistsAndEmitsNewLanguage() = testScope.runTest {
        repository.updateLanguage(com.dhikadityre.gpsmirage.core.model.AppLanguage.ENGLISH)

        val config = repository.appearanceConfig.first()
        assertEquals(com.dhikadityre.gpsmirage.core.model.AppLanguage.ENGLISH, config.language)
    }

    @Test
    fun updateThemeMode_persistsAndEmitsNewThemeMode() = testScope.runTest {
        repository.updateThemeMode(com.dhikadityre.gpsmirage.core.model.AppThemeMode.DARK)

        val config = repository.appearanceConfig.first()
        assertEquals(com.dhikadityre.gpsmirage.core.model.AppThemeMode.DARK, config.themeMode)
    }

    @Test
    fun updateAppearanceConfig_persistsAllValues() = testScope.runTest {
        val updated = com.dhikadityre.gpsmirage.core.model.AppearanceConfig(
            mapStyle = com.dhikadityre.gpsmirage.core.model.MapLayerStyle.OSM_STANDARD,
            showStatusBarNotification = false,
            language = com.dhikadityre.gpsmirage.core.model.AppLanguage.ENGLISH,
            themeMode = com.dhikadityre.gpsmirage.core.model.AppThemeMode.LIGHT
        )
        repository.updateAppearanceConfig(updated)

        val result = repository.appearanceConfig.first()
        assertEquals(com.dhikadityre.gpsmirage.core.model.MapLayerStyle.OSM_STANDARD, result.mapStyle)
        assertFalse(result.showStatusBarNotification)
        assertEquals(com.dhikadityre.gpsmirage.core.model.AppLanguage.ENGLISH, result.language)
        assertEquals(com.dhikadityre.gpsmirage.core.model.AppThemeMode.LIGHT, result.themeMode)
    }
}

