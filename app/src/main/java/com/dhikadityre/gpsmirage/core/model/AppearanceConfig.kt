package com.dhikadityre.gpsmirage.core.model

import com.dhikadityre.dev.tools.R
import java.util.Locale

/**
 * Available map tile styles/layers in GPS Mirage.
 */
enum class MapLayerStyle(val id: String, val titleRes: Int, val descriptionRes: Int) {
    GOOGLE_ROADMAP(
        id = "google_roadmap",
        titleRes = R.string.map_layer_google_roadmap,
        descriptionRes = R.string.map_layer_google_roadmap_desc
    ),
    GOOGLE_SATELLITE(
        id = "google_satellite",
        titleRes = R.string.map_layer_google_satellite,
        descriptionRes = R.string.map_layer_google_satellite_desc
    ),
    GOOGLE_HYBRID(
        id = "google_hybrid",
        titleRes = R.string.map_layer_google_hybrid,
        descriptionRes = R.string.map_layer_google_hybrid_desc
    ),
    OSM_STANDARD(
        id = "osm_standard",
        titleRes = R.string.map_layer_osm_standard,
        descriptionRes = R.string.map_layer_osm_standard_desc
    );

    companion object {
        val DEFAULT = GOOGLE_ROADMAP
        fun fromId(id: String?): MapLayerStyle =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * Supported in-app languages.
 */
enum class AppLanguage(val code: String, val displayNameRes: Int) {
    ENGLISH(
        code = "en",
        displayNameRes = R.string.language_english
    ),
    INDONESIAN(
        code = "id",
        displayNameRes = R.string.language_indonesian
    );

    companion object {
        val DEFAULT = ENGLISH

        /**
         * Resolves [AppLanguage] from persisted code, or falls back to detecting
         * system phone language. If system phone is Indonesian ("in" / "id"),
         * resolves to [INDONESIAN]. Otherwise (English or any foreign language),
         * defaults to [ENGLISH].
         */
        fun fromCode(code: String?): AppLanguage {
            if (code != null) {
                return when (code.lowercase()) {
                    "id", "in" -> INDONESIAN
                    "en" -> ENGLISH
                    else -> DEFAULT
                }
            }
            return resolveSystemDefault()
        }

        fun resolveSystemDefault(): AppLanguage {
            val systemLang = Locale.getDefault().language.lowercase()
            return if (systemLang == "in" || systemLang == "id") {
                INDONESIAN
            } else {
                ENGLISH
            }
        }
    }
}

/**
 * Display theme options for the application.
 */
enum class AppThemeMode(val id: String, val displayNameRes: Int) {
    SYSTEM(
        id = "system",
        displayNameRes = R.string.theme_follow_system
    ),
    LIGHT(
        id = "light",
        displayNameRes = R.string.theme_light
    ),
    DARK(
        id = "dark",
        displayNameRes = R.string.theme_dark
    );

    companion object {
        val DEFAULT = SYSTEM
        fun fromId(id: String?): AppThemeMode =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * Appearance settings state holding visual customization preferences.
 */
data class AppearanceConfig(
    val mapStyle: MapLayerStyle = MapLayerStyle.DEFAULT,
    val showStatusBarNotification: Boolean = true,
    val language: AppLanguage = AppLanguage.DEFAULT,
    val themeMode: AppThemeMode = AppThemeMode.DEFAULT
)
