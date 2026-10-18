package com.dhikadityre.gpsmirage.ui.navigation

/**
 * All destinations in GPS Mirage.
 */
sealed class Screen(val route: String) {
    // Bottom nav roots
    object Map : Screen("map")
    object Locations : Screen("locations")
    object Tools : Screen("tools")
    object More : Screen("more")

    // Locations sub-screens
    object LocationDetail : Screen("location_detail/{locationId}") {
        fun route(locationId: Long) = "location_detail/$locationId"
    }
    object LocationEdit : Screen("location_edit/{locationId}") {
        fun route(locationId: Long) = "location_edit/$locationId"
    }
    object LocationSave : Screen("location_save/{lat}/{lng}") {
        fun route(lat: Double, lng: Double) = "location_save/$lat/$lng"
    }

    // Folder sub-screens
    object FolderDetail : Screen("folder_detail/{folderId}") {
        fun route(folderId: Long) = "folder_detail/$folderId"
    }

    // Tools sub-screens
    object Generator : Screen("generator")
    object Import : Screen("import")
    object Export : Screen("export")
    object GeofenceRadius : Screen("geofence_radius")
    object DefaultValues : Screen("default_values")
    object AlarmScheduler : Screen("alarm_scheduler")
    object AppearanceSettings : Screen("appearance_settings")
    object Eligibility : Screen("eligibility")
    object NativeGps : Screen("native_gps")

    // Shared & More sub-screens
    object MockSetupGuide : Screen("mock_setup_guide")
    object About : Screen("about")
}

/**
 * Bottom navigation destinations.
 */
enum class BottomNavDestination(
    val screen: Screen,
    val label: String,
    val iconRes: String
) {
    MAP(Screen.Map, "Map", "map"),
    LOCATIONS(Screen.Locations, "Locations", "location_on"),
    TOOLS(Screen.Tools, "Tools", "build"),
    MORE(Screen.More, "More", "more_horiz")
}
