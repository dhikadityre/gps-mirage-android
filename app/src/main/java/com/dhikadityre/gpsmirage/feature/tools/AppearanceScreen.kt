package com.dhikadityre.gpsmirage.feature.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikadityre.dev.tools.R
import com.dhikadityre.gpsmirage.core.model.AppLanguage
import com.dhikadityre.gpsmirage.core.model.AppThemeMode
import com.dhikadityre.gpsmirage.core.model.MapLayerStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    viewModel: AppearanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.appearance_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_cancel)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // SECTION 1: MAP LAYER STYLE
            AppearanceSectionCard(
                icon = Icons.Default.Layers,
                title = stringResource(R.string.appearance_map_style_title),
                description = stringResource(R.string.appearance_map_style_desc)
            ) {
                MapLayerStyle.entries.forEach { style ->
                    val isSelected = uiState.config.mapStyle == style
                    SelectableOptionRow(
                        title = stringResource(style.titleRes),
                        subtitle = stringResource(style.descriptionRes),
                        isSelected = isSelected,
                        badge = if (style == MapLayerStyle.GOOGLE_ROADMAP) "Default" else null,
                        onClick = { viewModel.setMapStyle(style) }
                    )
                }
            }

            // SECTION 2: STATUS BAR NOTIFICATION
            AppearanceSectionCard(
                icon = if (uiState.config.showStatusBarNotification) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                title = stringResource(R.string.appearance_statusbar_title),
                description = stringResource(R.string.appearance_statusbar_desc)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.appearance_statusbar_toggle),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (uiState.config.showStatusBarNotification) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ) {
                                Text(
                                    text = if (uiState.config.showStatusBarNotification) "Ikon Terlihat" else "Mode Bersih/Silent",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.appearance_statusbar_toggle_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = uiState.config.showStatusBarNotification,
                        onCheckedChange = { viewModel.setShowStatusBarNotification(it) }
                    )
                }
            }

            // SECTION 3: IN-APP LANGUAGE
            AppearanceSectionCard(
                icon = Icons.Default.Language,
                title = stringResource(R.string.appearance_language_title),
                description = stringResource(R.string.appearance_language_desc)
            ) {
                AppLanguage.entries.forEach { lang ->
                    val isSelected = uiState.config.language == lang
                    SelectableOptionRow(
                        title = stringResource(lang.displayNameRes),
                        subtitle = if (lang == AppLanguage.INDONESIAN) {
                            stringResource(R.string.appearance_lang_id_desc)
                        } else {
                            stringResource(R.string.appearance_lang_en_desc)
                        },
                        isSelected = isSelected,
                        badge = if (lang == AppLanguage.ENGLISH) stringResource(R.string.badge_default) else null,
                        onClick = { viewModel.setLanguage(lang) }
                    )
                }
            }

            // SECTION 4: THEME MODE
            AppearanceSectionCard(
                icon = when (uiState.config.themeMode) {
                    AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                    AppThemeMode.LIGHT -> Icons.Default.LightMode
                    AppThemeMode.DARK -> Icons.Default.DarkMode
                },
                title = stringResource(R.string.appearance_theme_title),
                description = stringResource(R.string.appearance_theme_desc)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val isSelected = uiState.config.themeMode == mode
                    val subtitle = when (mode) {
                        AppThemeMode.SYSTEM -> "Menyesuaikan otomatis dengan tema perangkat Anda"
                        AppThemeMode.LIGHT -> "Palet warna terang dengan kontras ikon status bar gelap"
                        AppThemeMode.DARK -> "Palet warna gelap hemat baterai dengan status bar transparan"
                    }
                    SelectableOptionRow(
                        title = stringResource(mode.displayNameRes),
                        subtitle = subtitle,
                        isSelected = isSelected,
                        badge = if (mode == AppThemeMode.SYSTEM) "Default" else null,
                        onClick = { viewModel.setThemeMode(mode) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AppearanceSectionCard(
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            content()
        }
    }
}

@Composable
private fun SelectableOptionRow(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    badge?.let {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                subtitle?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
