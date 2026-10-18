package com.dhikadityre.gpsmirage.domain.settings

import com.dhikadityre.gpsmirage.core.model.AppearanceConfig
import com.dhikadityre.gpsmirage.core.model.AppLanguage
import com.dhikadityre.gpsmirage.core.model.AppThemeMode
import com.dhikadityre.gpsmirage.core.model.MapLayerStyle
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

    /**
     * Observable stream of current [AppearanceConfig].
     */
    val appearanceConfig: Flow<AppearanceConfig>

    /**
     * Persist updated [AppearanceConfig] preferences.
     */
    suspend fun updateAppearanceConfig(config: AppearanceConfig)

    /**
     * Persist updated map tile style.
     */
    suspend fun updateMapStyle(mapStyle: MapLayerStyle)

    /**
     * Persist status bar notification icon visibility preference.
     */
    suspend fun updateShowStatusBarNotification(show: Boolean)

    /**
     * Persist in-app language preference.
     */
    suspend fun updateLanguage(language: AppLanguage)

    /**
     * Persist display theme mode preference.
     */
    suspend fun updateThemeMode(themeMode: AppThemeMode)
}

