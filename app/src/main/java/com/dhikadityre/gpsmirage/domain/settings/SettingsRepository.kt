package com.dhikadityre.gpsmirage.domain.settings

import com.dhikadityre.gpsmirage.core.model.RadiusConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user preferences and geofence settings.
 */
interface SettingsRepository {
    /**
     * Observable stream of current [RadiusConfig].
     */
    val radiusConfig: Flow<RadiusConfig>

    /**
     * Persist updated [RadiusConfig] preferences.
     */
    suspend fun updateRadiusConfig(config: RadiusConfig)
}
