package com.dhikadityre.gpsmirage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dhikadityre.dev.tools.BuildConfig
import com.dhikadityre.gpsmirage.core.model.AppearanceConfig
import com.dhikadityre.gpsmirage.domain.settings.SettingsRepository
import com.dhikadityre.gpsmirage.ui.navigation.GpsMirageBottomNav
import com.dhikadityre.gpsmirage.ui.navigation.GpsMirageNavHost
import com.dhikadityre.gpsmirage.ui.navigation.Screen
import com.dhikadityre.gpsmirage.ui.navigation.bottomNavItems
import com.dhikadityre.gpsmirage.ui.theme.GpsMirageTheme
import com.dhikadityre.gpsmirage.ui.theme.ProvideAppLanguage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.dhikadityre.dev.tools.R
import com.dhikadityre.gpsmirage.ui.theme.DesertNight
import com.dhikadityre.gpsmirage.ui.theme.GoldenAmber
import com.dhikadityre.gpsmirage.ui.theme.GoldenWhite
import com.dhikadityre.gpsmirage.ui.theme.MoonGold
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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

            var isSplashFinished by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(1500)
                isSplashFinished = true
            }

            ProvideAppLanguage(language = appearanceConfig.language) {
                GpsMirageTheme(themeMode = appearanceConfig.themeMode) {
                    Crossfade(
                        targetState = isSplashFinished,
                        animationSpec = tween(400),
                        label = "splash_transition"
                    ) { finished ->
                        if (finished) {
                            GpsMirageMainScreen()
                        } else {
                            SplashScreenView(versionName = BuildConfig.VERSION_NAME)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreenView(versionName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesertNight),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_gps_mirage_logo),
                contentDescription = "GPS Mirage Logo",
                modifier = Modifier.size(112.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "GPS Mirage",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = GoldenAmber,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Precision Mock Geolocation",
                style = MaterialTheme.typography.bodyMedium,
                color = GoldenWhite.copy(alpha = 0.85f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "v$versionName",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MoonGold.copy(alpha = 0.75f),
                letterSpacing = 0.5.sp
            )
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
