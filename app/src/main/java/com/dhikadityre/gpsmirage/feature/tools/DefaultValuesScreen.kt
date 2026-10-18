package com.dhikadityre.gpsmirage.feature.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.dhikadityre.dev.tools.R
import com.dhikadityre.gpsmirage.core.geo.UnitConverter
import com.dhikadityre.gpsmirage.core.model.DefaultValueProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultValuesScreen(
    onBack: () -> Unit,
    viewModel: DefaultValuesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var profileToDelete by remember { mutableStateOf<DefaultValueProfile?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.default_values_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openCreateDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.default_values_add_preset))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.default_values_banner_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.default_values_banner_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(uiState.profiles, key = { it.id }) { profile ->
                PresetCard(
                    profile = profile,
                    isActive = profile.id == uiState.activeProfile?.id,
                    onSelectActive = { viewModel.setActiveProfile(profile.id) },
                    onEdit = { viewModel.openEditDialog(profile) },
                    onDelete = { profileToDelete = profile },
                    onResetOriginal = { showResetConfirm = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    // Modal Form Edit / Create
    uiState.formState?.let { form ->
        DefaultProfileEditDialog(
            form = form,
            currentSensorBearing = uiState.currentSensorBearing,
            isCompassAvailable = uiState.isCompassAvailable,
            onNameChange = viewModel::onNameChanged,
            onLatChange = viewModel::onLatitudeChanged,
            onLngChange = viewModel::onLongitudeChanged,
            onRadiusChange = viewModel::onRadiusChanged,
            onAccMetersChange = viewModel::onAccuracyMetersChanged,
            onAccFeetChange = viewModel::onAccuracyFeetChanged,
            onAltMetersChange = viewModel::onAltitudeMetersChanged,
            onAltFeetChange = viewModel::onAltitudeFeetChanged,
            onSpeedChange = viewModel::onSpeedChanged,
            onBearingChange = viewModel::onBearingChanged,
            onUseDeviceSensorToggle = viewModel::onUseDeviceSensorToggled,
            onDismiss = viewModel::dismissDialog,
            onSave = viewModel::saveForm
        )
    }

    // Delete confirmation dialog
    profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.default_values_delete_title)) },
            text = { Text(stringResource(R.string.default_values_delete_message, profile.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProfile(profile.id)
                        profileToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.location_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Reset confirmation dialog for system original preset
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
            title = { Text(stringResource(R.string.default_values_reset_original_title)) },
            text = { Text(stringResource(R.string.default_values_reset_original_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetOriginalPreset()
                        showResetConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.action_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun PresetCard(
    profile: DefaultValueProfile,
    isActive: Boolean,
    onSelectActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onResetOriginal: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (profile.isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Bawaan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (isActive) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.default_values_active_badge), fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                } else {
                    OutlinedButton(
                        onClick = onSelectActive,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(stringResource(R.string.default_values_select_active), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DetailRow(label = stringResource(R.string.nav_map), value = "${profile.latitude}, ${profile.longitude}")
                DetailRow(label = stringResource(R.string.generator_radius), value = "${profile.radiusMeters.toInt()} ${stringResource(R.string.generator_radius_unit)}")
                DetailRow(
                    label = stringResource(R.string.location_accuracy),
                    value = "${UnitConverter.formatTwoDecimals(profile.accuracyFeet)} ft (~${UnitConverter.formatTwoDecimals(profile.accuracyMeters)}m)"
                )
                DetailRow(
                    label = stringResource(R.string.location_altitude),
                    value = "${UnitConverter.formatTwoDecimals(profile.altitudeFeet)} ft (~${UnitConverter.formatTwoDecimals(profile.altitudeMeters)}m)"
                )
                DetailRow(label = stringResource(R.string.location_speed), value = "${profile.speedKmh.toInt()} km/h")
                DetailRow(
                    label = stringResource(R.string.location_bearing),
                    value = if (profile.useDeviceSensor) {
                        "${UnitConverter.formatTwoDecimals(profile.bearingDegrees)}° (Compass)"
                    } else {
                        "${UnitConverter.formatTwoDecimals(profile.bearingDegrees)}° (Manual)"
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (profile.isDefault) {
                    TextButton(onClick = onResetOriginal) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.default_values_reset_initial))
                    }
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.location_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_edit))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultProfileEditDialog(
    form: DefaultProfileFormState,
    currentSensorBearing: Float,
    isCompassAvailable: Boolean,
    onNameChange: (String) -> Unit,
    onLatChange: (String) -> Unit,
    onLngChange: (String) -> Unit,
    onRadiusChange: (String) -> Unit,
    onAccMetersChange: (String) -> Unit,
    onAccFeetChange: (String) -> Unit,
    onAltMetersChange: (String) -> Unit,
    onAltFeetChange: (String) -> Unit,
    onSpeedChange: (String) -> Unit,
    onBearingChange: (String) -> Unit,
    onUseDeviceSensorToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                if (form.id == 0L) {
                                    stringResource(R.string.default_values_dialog_add_title)
                                } else {
                                    stringResource(R.string.default_values_dialog_edit_title, form.name)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                            }
                        },
                        actions = {
                            TextButton(onClick = onSave) {
                                Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (form.error != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = form.error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = form.name,
                        onValueChange = onNameChange,
                        label = { Text(stringResource(R.string.default_values_preset_name)) },
                        placeholder = { Text(stringResource(R.string.default_values_preset_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(stringResource(R.string.default_values_section_coord_geofence), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = form.latitude,
                            onValueChange = onLatChange,
                            label = { Text("Latitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = form.longitude,
                            onValueChange = onLngChange,
                            label = { Text("Longitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = form.radiusMeters,
                        onValueChange = onRadiusChange,
                        label = { Text(stringResource(R.string.default_values_radius_meters)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    HorizontalDivider()

                    Text(stringResource(R.string.default_values_accuracy_section), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = stringResource(R.string.default_values_accuracy_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = form.accuracyMeters,
                            onValueChange = onAccMetersChange,
                            label = { Text("Meter (m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = form.accuracyFeet,
                            onValueChange = onAccFeetChange,
                            label = { Text("Feet (ft)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    HorizontalDivider()

                    Text(stringResource(R.string.default_values_altitude_section), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = stringResource(R.string.default_values_altitude_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = form.altitudeMeters,
                            onValueChange = onAltMetersChange,
                            label = { Text("Meter (m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = form.altitudeFeet,
                            onValueChange = onAltFeetChange,
                            label = { Text("Feet (ft)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    HorizontalDivider()

                    Text(stringResource(R.string.default_values_movement_section), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = form.speedKmh,
                        onValueChange = onSpeedChange,
                        label = { Text(stringResource(R.string.default_values_speed)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.default_values_compass_follow),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isCompassAvailable) {
                                        stringResource(
                                            R.string.default_values_sensor_active,
                                            UnitConverter.formatTwoDecimals(currentSensorBearing.toDouble())
                                        )
                                    } else {
                                        stringResource(R.string.default_values_compass_unavailable)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = form.useDeviceSensor,
                                onCheckedChange = onUseDeviceSensorToggle,
                                enabled = isCompassAvailable
                            )
                        }
                    }

                    OutlinedTextField(
                        value = form.bearingDegrees,
                        onValueChange = onBearingChange,
                        label = { Text(stringResource(R.string.default_values_bearing_manual)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !form.useDeviceSensor,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
