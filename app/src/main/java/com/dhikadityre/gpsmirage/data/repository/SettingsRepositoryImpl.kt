package com.dhikadityre.gpsmirage.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dhikadityre.gpsmirage.core.model.AppearanceConfig
import com.dhikadityre.gpsmirage.core.model.AppLanguage
import com.dhikadityre.gpsmirage.core.model.AppThemeMode
import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.MapLayerStyle
import com.dhikadityre.gpsmirage.core.model.RadiusConfig
import com.dhikadityre.gpsmirage.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val RADIUS_ENABLED = booleanPreferencesKey("radius_enabled")
        val ANCHOR_LAT = doublePreferencesKey("anchor_latitude")
        val ANCHOR_LNG = doublePreferencesKey("anchor_longitude")
        val ANCHOR_EXPLICITLY_NULL = booleanPreferencesKey("anchor_explicitly_null")
        val RADIUS_METERS = doublePreferencesKey("radius_meters")

        val MAP_LAYER_STYLE = stringPreferencesKey("map_layer_style")
        val SHOW_STATUS_BAR_NOTIFICATION = booleanPreferencesKey("show_status_bar_notification")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
    }

    override val radiusConfig: Flow<RadiusConfig> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isEnabled = preferences[PreferencesKeys.RADIUS_ENABLED] ?: true
            val lat = preferences[PreferencesKeys.ANCHOR_LAT]
            val lng = preferences[PreferencesKeys.ANCHOR_LNG]
            val isExplicitlyNull = preferences[PreferencesKeys.ANCHOR_EXPLICITLY_NULL] ?: false
            val radiusMeters = preferences[PreferencesKeys.RADIUS_METERS] ?: 500.0

            val anchor = if (lat != null && lng != null) {
                Coordinate(latitude = lat, longitude = lng)
            } else if (isExplicitlyNull) {
                null
            } else {
                RadiusConfig.MAYAPADA_TOWER
            }

            RadiusConfig(
                isEnabled = isEnabled,
                anchor = anchor,
                radiusMeters = radiusMeters
            )
        }

    override suspend fun updateRadiusConfig(config: RadiusConfig) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.RADIUS_ENABLED] = config.isEnabled
            if (config.anchor != null) {
                preferences[PreferencesKeys.ANCHOR_LAT] = config.anchor.latitude
                preferences[PreferencesKeys.ANCHOR_LNG] = config.anchor.longitude
                preferences[PreferencesKeys.ANCHOR_EXPLICITLY_NULL] = false
            } else {
                preferences.remove(PreferencesKeys.ANCHOR_LAT)
                preferences.remove(PreferencesKeys.ANCHOR_LNG)
                preferences[PreferencesKeys.ANCHOR_EXPLICITLY_NULL] = true
            }
            preferences[PreferencesKeys.RADIUS_METERS] = config.radiusMeters
        }
    }

    override val appearanceConfig: Flow<AppearanceConfig> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppearanceConfig(
                mapStyle = MapLayerStyle.fromId(preferences[PreferencesKeys.MAP_LAYER_STYLE]),
                showStatusBarNotification = preferences[PreferencesKeys.SHOW_STATUS_BAR_NOTIFICATION] ?: true,
                language = AppLanguage.fromCode(preferences[PreferencesKeys.APP_LANGUAGE]),
                themeMode = AppThemeMode.fromId(preferences[PreferencesKeys.APP_THEME_MODE])
            )
        }

    override suspend fun updateAppearanceConfig(config: AppearanceConfig) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAP_LAYER_STYLE] = config.mapStyle.id
            preferences[PreferencesKeys.SHOW_STATUS_BAR_NOTIFICATION] = config.showStatusBarNotification
            preferences[PreferencesKeys.APP_LANGUAGE] = config.language.code
            preferences[PreferencesKeys.APP_THEME_MODE] = config.themeMode.id
        }
    }

    override suspend fun updateMapStyle(mapStyle: MapLayerStyle) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAP_LAYER_STYLE] = mapStyle.id
        }
    }

    override suspend fun updateShowStatusBarNotification(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_STATUS_BAR_NOTIFICATION] = show
        }
    }

    override suspend fun updateLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = language.code
        }
    }

    override suspend fun updateThemeMode(themeMode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME_MODE] = themeMode.id
        }
    }
}
