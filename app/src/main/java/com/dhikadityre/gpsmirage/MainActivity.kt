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
import com.dhikadityre.gpsmirage.ui.navigation.GpsMirageBottomNav
import com.dhikadityre.gpsmirage.ui.navigation.GpsMirageNavHost
import com.dhikadityre.gpsmirage.ui.navigation.Screen
import com.dhikadityre.gpsmirage.ui.navigation.bottomNavItems
import com.dhikadityre.gpsmirage.ui.theme.GpsMirageTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GpsMirageTheme {
                GpsMirageMainScreen()
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
