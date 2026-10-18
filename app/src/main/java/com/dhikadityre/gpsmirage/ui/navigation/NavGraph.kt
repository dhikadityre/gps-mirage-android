package com.dhikadityre.gpsmirage.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dhikadityre.gpsmirage.feature.generator.GeneratorScreen
import com.dhikadityre.gpsmirage.feature.history.HistoryScreen
import com.dhikadityre.gpsmirage.feature.importexport.ExportScreen
import com.dhikadityre.gpsmirage.feature.importexport.ImportScreen
import com.dhikadityre.gpsmirage.feature.locations.LocationDetailScreen
import com.dhikadityre.gpsmirage.feature.locations.LocationEditScreen
import com.dhikadityre.gpsmirage.feature.locations.LocationsScreen
import com.dhikadityre.gpsmirage.feature.map.MapScreen
import com.dhikadityre.gpsmirage.feature.more.MoreScreen
import com.dhikadityre.gpsmirage.feature.settings.MockSetupGuideScreen
import com.dhikadityre.gpsmirage.feature.settings.SettingsScreen
import com.dhikadityre.gpsmirage.feature.tools.AlarmSchedulerScreen
import com.dhikadityre.gpsmirage.feature.tools.AppearanceScreen
import com.dhikadityre.gpsmirage.feature.tools.DefaultValuesScreen
import com.dhikadityre.gpsmirage.feature.tools.GeofenceRadiusScreen
import com.dhikadityre.gpsmirage.feature.tools.ToolsScreen

@Composable
fun GpsMirageNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        modifier = modifier
    ) {
        composable(Screen.Map.route) {
            MapScreen(
                onNavigateToLocations = { navController.navigate(Screen.Locations.route) },
                onSaveLocation = { lat, lng ->
                    navController.navigate(Screen.LocationSave.route(lat, lng))
                }
            )
        }

        composable(Screen.Locations.route) {
            LocationsScreen(
                onLocationClick = { id ->
                    navController.navigate(Screen.LocationDetail.route(id))
                },
                onAddLocation = {
                    navController.navigate(Screen.LocationEdit.route(0L))
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Map.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.LocationDetail.route,
            arguments = listOf(navArgument("locationId") { type = NavType.LongType })
        ) { backStack ->
            val locationId = backStack.arguments?.getLong("locationId") ?: return@composable
            LocationDetailScreen(
                locationId = locationId,
                onEdit = { navController.navigate(Screen.LocationEdit.route(locationId)) },
                onBack = { navController.popBackStack() },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Map.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.LocationEdit.route,
            arguments = listOf(navArgument("locationId") { type = NavType.LongType })
        ) { backStack ->
            val locationId = backStack.arguments?.getLong("locationId") ?: return@composable
            LocationEditScreen(
                locationId = locationId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.LocationSave.route,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lng") { type = NavType.StringType }
            )
        ) { backStack ->
            val lat = backStack.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lng = backStack.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0
            LocationEditScreen(
                locationId = 0L,
                initialLat = lat,
                initialLng = lng,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Tools.route) {
            ToolsScreen(
                onNavigateToSetupGuide = { navController.navigate(Screen.MockSetupGuide.route) },
                onNavigateToGeofenceRadius = { navController.navigate(Screen.GeofenceRadius.route) },
                onNavigateToDefaultValues = { navController.navigate(Screen.DefaultValues.route) },
                onNavigateToGenerator = { navController.navigate(Screen.Generator.route) },
                onNavigateToAlarmScheduler = { navController.navigate(Screen.AlarmScheduler.route) },
                onNavigateToAppearance = { navController.navigate(Screen.AppearanceSettings.route) },
                onNavigateToImport = { navController.navigate(Screen.Import.route) },
                onNavigateToExport = { navController.navigate(Screen.Export.route) }
            )
        }

        composable(Screen.GeofenceRadius.route) {
            GeofenceRadiusScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSetupGuide = { navController.navigate(Screen.MockSetupGuide.route) }
            )
        }

        composable(Screen.DefaultValues.route) {
            DefaultValuesScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AlarmScheduler.route) {
            AlarmSchedulerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AppearanceSettings.route) {
            AppearanceScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Generator.route) {
            GeneratorScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Import.route) {
            ImportScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Export.route) {
            ExportScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.More.route) {
            MoreScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSetupGuide = { navController.navigate(Screen.MockSetupGuide.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToGeofenceRadius = { navController.navigate(Screen.GeofenceRadius.route) },
                onNavigateToSetupGuide = { navController.navigate(Screen.MockSetupGuide.route) }
            )
        }

        composable(Screen.MockSetupGuide.route) {
            MockSetupGuideScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.About.route) {
            MoreScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSetupGuide = { navController.navigate(Screen.MockSetupGuide.route) },
                onNavigateToAbout = {}
            )
        }
    }
}
