package com.dhikadityre.gpsmirage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikadityre.gpsmirage.core.model.AppearanceConfig
import com.dhikadityre.gpsmirage.domain.settings.SettingsRepository
import com.dhikadityre.gpsmirage.ui.navigation.GpsMirageBottomNav
import com.dhikadityre.gpsmirage.ui.navigation.GpsMirageNavHost
import com.dhikadityre.gpsmirage.ui.navigation.Screen
import com.dhikadityre.gpsmirage.ui.navigation.bottomNavItems
import com.dhikadityre.gpsmirage.ui.theme.GpsMirageTheme
import com.dhikadityre.gpsmirage.ui.theme.ProvideAppLanguage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearanceConfig by settingsRepository.appearanceConfig
                .collectAsStateWithLifecycle(initialValue = AppearanceConfig())

            ProvideAppLanguage(language = appearanceConfig.language) {
                GpsMirageTheme(themeMode = appearanceConfig.themeMode) {
                    GpsMirageMainScreen()
                }
            }
        }
    }
}

@Composable
fun GpsMirageMainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomNav = bottomNavItems.any { it.screen.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                GpsMirageBottomNav(navController = navController)
            }
        }
    ) { innerPadding ->
        GpsMirageNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
