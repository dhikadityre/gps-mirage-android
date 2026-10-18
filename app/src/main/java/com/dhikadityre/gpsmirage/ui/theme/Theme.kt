package com.dhikadityre.gpsmirage.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.dhikadityre.gpsmirage.core.model.AppThemeMode

// Desert Day (Light Mode) & Desert Night (Dark Mode) Color System
val DesertSand = Color(0xFFEAD0A8)
val WarmOchre = Color(0xFFC58B3E)
val DeepBrown = Color(0xFF4E3629)
val DarkBrownText = Color(0xFF3E2723)
val SandCream = Color(0xFFFAF5ED)

val DesertNight = Color(0xFF0F141D)
val DuneNight = Color(0xFF1F2A38)
val DuneSurface = Color(0xFF151D2A)
val GoldenAmber = Color(0xFFF5B041)
val MoonGold = Color(0xFFF9D38D)
val GoldenWhite = Color(0xFFFAE5C7)

// Functional indicators
val MockActiveGreen = Color(0xFF22C55E)
val MockInactiveGray = Color(0xFF8D7C68)
val ErrorRed = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF5B041)

// Backward compatibility tokens
val PrimaryBlue = GoldenAmber
val PrimaryBlueDark = WarmOchre
val PrimaryBlueLight = MoonGold
val SurfaceDark = DuneSurface
val SurfaceDarker = DesertNight
val SurfaceLight = SandCream

private val DarkColorScheme = darkColorScheme(
    primary = GoldenAmber,
    onPrimary = Color(0xFF221600),
    primaryContainer = Color(0xFF452F0C),
    onPrimaryContainer = MoonGold,
    secondary = Color(0xFF8A99AD),
    onSecondary = DesertNight,
    secondaryContainer = Color(0xFF1E2836),
    onSecondaryContainer = GoldenWhite,
    tertiary = MockActiveGreen,
    background = DesertNight,
    onBackground = GoldenWhite,
    surface = DuneSurface,
    onSurface = GoldenWhite,
    surfaceVariant = DuneNight,
    onSurfaceVariant = Color(0xFFD4DEEB),
    error = Color(0xFFFFB4AB),
    outline = Color(0xFF2D3C4F),
    outlineVariant = Color(0xFF1F2A38)
)

private val LightColorScheme = lightColorScheme(
    primary = WarmOchre,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7E2C3),
    onPrimaryContainer = DarkBrownText,
    secondary = Color(0xFF8D6E63),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE4DA),
    onSecondaryContainer = DarkBrownText,
    tertiary = MockActiveGreen,
    background = Color(0xFFF8F3EB),
    onBackground = DarkBrownText,
    surface = SandCream,
    onSurface = DarkBrownText,
    surfaceVariant = DesertSand.copy(alpha = 0.5f),
    onSurfaceVariant = Color(0xFF5D4037),
    error = Color(0xFFBA1A1A),
    outline = Color(0xFFD3BA9E),
    outlineVariant = DesertSand
)

@Composable
fun GpsMirageTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GpsMirageTypography,
        content = content
    )
}
