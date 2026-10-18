package com.dhikadityre.gpsmirage.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.dhikadityre.gpsmirage.core.model.Coordinate
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
}
