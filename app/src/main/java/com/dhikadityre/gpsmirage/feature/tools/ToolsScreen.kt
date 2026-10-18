package com.dhikadityre.gpsmirage.feature.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateToEligibility: () -> Unit,
    onNavigateToNativeGps: () -> Unit,
    onNavigateToSetupGuide: () -> Unit,
    onNavigateToGeofenceRadius: () -> Unit,
    onNavigateToDefaultValues: () -> Unit,
    onNavigateToGenerator: () -> Unit,
    onNavigateToAlarmScheduler: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToExport: () -> Unit,
    viewModel: ToolsViewModel = hiltViewModel(),
    onSoftReset: () -> Unit = { viewModel.onSoftReset() },
    onHardReset: () -> Unit = { viewModel.onHardReset() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tools_dialog_reset_title),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.tools_dialog_reset_subtitle),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.tools_dialog_soft_reset_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.tools_dialog_soft_reset_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.tools_dialog_hard_reset_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.tools_dialog_hard_reset_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onHardReset()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.tools_dialog_hard_reset_btn))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            showResetDialog = false
                            onSoftReset()
                        }
                    ) {
                        Text(stringResource(R.string.tools_dialog_soft_reset_btn))
                    }
                }
            }
        )
    }

    if (uiState.showMockRunningWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMockRunningWarning() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.tools_dialog_mock_running_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.tools_dialog_mock_running_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmHardResetFromNativeGps() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.tools_dialog_hard_reset_gps_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissMockRunningWarning() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (uiState.showGoogleMapsVerification) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissGoogleMapsVerification() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.tools_dialog_verify_location_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.tools_dialog_verify_location_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.tools_dialog_verify_countdown, uiState.verificationCountdown),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissGoogleMapsVerification()
                        launchGoogleMaps(context)
                    }
                ) {
                    Text(stringResource(R.string.tools_dialog_open_gmaps_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissGoogleMapsVerification() }) {
                    Text(stringResource(R.string.tools_dialog_no_btn))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tools_title),
                        fontWeight = FontWeight.SemiBold
                    )
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
            // SECTION 1: BASELINE
            SectionHeader(
                title = stringResource(R.string.tools_section_baseline_title),
                description = stringResource(R.string.tools_section_baseline_desc)
            )

            ToolItemCard(
                title = stringResource(R.string.tools_item_eligibility_title),
                description = stringResource(R.string.tools_item_eligibility_desc),
                badge = stringResource(R.string.tools_item_eligibility_badge),
                icon = Icons.Default.VerifiedUser,
                onClick = onNavigateToEligibility
            )

            ToolItemCard(
                title = stringResource(R.string.tools_item_native_gps_title),
                description = stringResource(R.string.tools_item_native_gps_desc),
                badge = stringResource(R.string.tools_item_native_gps_badge),
                icon = Icons.Default.GpsFixed,
                onClick = {
                    viewModel.onNativeGpsClicked(
                        onNavigate = onNavigateToNativeGps
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // SECTION 2: INTERCEPT GPS
            SectionHeader(
                title = stringResource(R.string.tools_section_intercept_title),
                description = stringResource(R.string.tools_section_intercept_desc)
            )

            ToolItemCard(
                title = stringResource(R.string.tools_item_reset_title),
                description = stringResource(R.string.tools_item_reset_desc),
                badge = stringResource(R.string.tools_item_reset_badge),
                icon = Icons.Default.RestartAlt,
                onClick = { showResetDialog = true }
            )

            ToolItemCard(
                title = stringResource(R.string.tools_item_setup_title),
                description = stringResource(R.string.tools_item_setup_desc),
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                onClick = onNavigateToSetupGuide
            )

            ToolItemCard(
                title = stringResource(R.string.tools_item_geofence_title),
                description = stringResource(R.string.tools_item_geofence_desc),
                icon = Icons.Default.Adjust,
                onClick = onNavigateToGeofenceRadius
            )

            ToolItemCard(
                title = stringResource(R.string.tools_item_default_values_title),
                description = stringResource(R.string.tools_item_default_values_desc),
                icon = Icons.Default.Tune,
                onClick = onNavigateToDefaultValues
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // SECTION 3: AUTOMATION
            SectionHeader(
                title = stringResource(R.string.tools_section_automation_title),
                description = stringResource(R.string.tools_section_automation_desc)
            )

            ToolItemCard(
                title = stringResource(R.string.tools_item_generator_title),
                description = stringResource(R.string.tools_item_generator_desc),
                icon = Icons.Default.Refresh,
                onClick = onNavigateToGenerator
            )

            ToolItemCard(
                title = stringResource(R.string.tools_item_alarm_title),
                description = stringResource(R.string.tools_item_alarm_desc),
                icon = Icons.Default.Alarm,
                onClick = onNavigateToAlarmScheduler
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // SECTION 4: DATA MANAGEMENT
            SectionHeader(
                title = stringResource(R.string.tools_section_data_title),
                description = stringResource(R.string.tools_section_data_desc)
            )

            ToolItemCard(
                title = stringResource(R.string.tools_item_import_title),
                description = stringResource(R.string.tools_item_import_desc),
                icon = Icons.Default.FileDownload,
                onClick = onNavigateToImport
            )

            ToolItemCard(
                title = stringResource(R.string.tools_item_export_title),
                description = stringResource(R.string.tools_item_export_desc),
                icon = Icons.Default.FileUpload,
                onClick = onNavigateToExport
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    description: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToolItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    badge: String? = null,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    badge?.let {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(bottom = 2.dp)
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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun launchGoogleMaps(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=my+location")).apply {
            setPackage("com.google.android.apps.maps")
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {}
    } catch (_: Exception) {}
}
