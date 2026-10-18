package com.dhikadityre.gpsmirage.ui.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.dhikadityre.gpsmirage.core.model.AppLanguage
import java.util.Locale

/**
 * CompositionLocal provider that dynamically updates the active [AppLanguage]
 * while preserving the [Activity] context for Hilt ViewModel injection.
 */
@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val locale = when (language) {
        AppLanguage.INDONESIAN -> Locale("in", "ID")
        AppLanguage.ENGLISH -> Locale("en", "US")
    }

    SideEffect {
        Locale.setDefault(locale)
        val activity = context as? Activity
        if (activity != null) {
            val res = activity.resources
            val config = Configuration(res.configuration)
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)
        }
    }

    val configuration = Configuration(LocalConfiguration.current).apply {
        setLocale(locale)
    }

    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        content = content
    )
}
