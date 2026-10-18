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

// GPS Mirage Color Palette — Dark-first, developer-friendly
val PrimaryBlue = Color(0xFF3B82F6)
val PrimaryBlueDark = Color(0xFF1D4ED8)
val PrimaryBlueLight = Color(0xFF93C5FD)

val MockActiveGreen = Color(0xFF22C55E)
val MockInactiveGray = Color(0xFF6B7280)
val ErrorRed = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)

val SurfaceDark = Color(0xFF1E293B)
val SurfaceDarker = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFF1F5F9)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = PrimaryBlueLight,
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    tertiary = MockActiveGreen,
    background = SurfaceDarker,
    onBackground = SurfaceLight,
    surface = SurfaceDark,
    onSurface = SurfaceLight,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = ErrorRed,
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    tertiary = MockActiveGreen,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error = ErrorRed,
    outline = Color(0xFFCBD5E1)
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
