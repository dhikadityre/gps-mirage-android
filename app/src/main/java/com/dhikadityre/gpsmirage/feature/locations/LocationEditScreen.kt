package com.dhikadityre.gpsmirage.feature.locations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.geo.UnitConverter
import com.dhikadityre.gpsmirage.core.model.DefaultValueProfile
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.LocationType
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import com.dhikadityre.gpsmirage.domain.preset.DefaultProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationEditViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val defaultProfileRepository: DefaultProfileRepository
) : ViewModel() {

    val activeProfile: StateFlow<DefaultValueProfile?> = defaultProfileRepository.activeProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _existingLocation = MutableStateFlow<Location?>(null)
    val existingLocation: StateFlow<Location?> = _existingLocation.asStateFlow()

    private val _availableAnchors = MutableStateFlow<List<Location>>(emptyList())
    val availableAnchors: StateFlow<List<Location>> = _availableAnchors.asStateFlow()

    init {
        loadAnchors()
    }

    private fun loadAnchors() {
        viewModelScope.launch {
            _availableAnchors.value = locationRepository.getByType(LocationType.CENTER_ANCHOR)
        }
    }

    fun load(id: Long) {
        if (id == 0L) return
        viewModelScope.launch {
            _existingLocation.value = locationRepository.getById(id)
        }
    }

    fun save(
        id: Long,
        name: String,
        latitude: Double,
        longitude: Double,
        type: LocationType,
        radiusMeters: Double?,
        parentAnchorId: Long?,
        accuracy: Float,
        altitude: Double,
        speed: Float,
        bearing: Float,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val loc = Location(
                id = id,
                name = name,
                latitude = latitude,
                longitude = longitude,
                type = type,
                radiusMeters = if (type == LocationType.CENTER_ANCHOR) radiusMeters ?: 500.0 else null,
                parentAnchorId = if (type == LocationType.MOCK_TARGET) parentAnchorId else null,
                accuracy = accuracy,
                altitude = altitude,
                speed = speed,
                bearing = bearing
            )
            if (id == 0L) {
                locationRepository.save(loc)
            } else {
                locationRepository.update(loc)
            }
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationEditScreen(
    locationId: Long,
    initialLat: Double = 0.0,
    initialLng: Double = 0.0,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: LocationEditViewModel = hiltViewModel()
) {
    LaunchedEffect(locationId) {
        if (locationId != 0L) {
            viewModel.load(locationId)
        }
    }

    val existing by viewModel.existingLocation.collectAsState()
    val availableAnchors by viewModel.availableAnchors.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()

    var name by remember { mutableStateOf("") }
    var latInput by remember { mutableStateOf(if (initialLat != 0.0) initialLat.toString() else "") }
    var lngInput by remember { mutableStateOf(if (initialLng != 0.0) initialLng.toString() else "") }
    var locationType by remember { mutableStateOf(LocationType.MOCK_TARGET) }
    var radiusInput by remember { mutableStateOf("500") }
    var selectedParentAnchorId by remember { mutableStateOf<Long?>(null) }
    var isAnchorDropdownExpanded by remember { mutableStateOf(false) }

    var accuracyInput by remember { mutableStateOf("5") }
    var altitudeInput by remember { mutableStateOf("0") }
    var speedInput by remember { mutableStateOf("0") }
    var bearingInput by remember { mutableStateOf("0") }
    var showAdvanced by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasPreFilledFromPreset by remember { mutableStateOf(false) }

    LaunchedEffect(activeProfile) {
        val preset = activeProfile
        if (locationId == 0L && existing == null && preset != null && !hasPreFilledFromPreset) {
            hasPreFilledFromPreset = true
            if (latInput.isBlank()) latInput = preset.latitude.toString()
            if (lngInput.isBlank()) lngInput = preset.longitude.toString()
            radiusInput = preset.radiusMeters.toInt().toString()
            accuracyInput = UnitConverter.formatTwoDecimals(preset.accuracyMeters)
            altitudeInput = UnitConverter.formatTwoDecimals(preset.altitudeMeters)
            speedInput = preset.speedKmh.toInt().toString()
            bearingInput = UnitConverter.formatTwoDecimals(preset.bearingDegrees)
        }
    }

    LaunchedEffect(existing) {
        existing?.let {
            name = it.name
            latInput = it.latitude.toString()
            lngInput = it.longitude.toString()
            locationType = it.type
            radiusInput = (it.radiusMeters ?: 500.0).toInt().toString()
            selectedParentAnchorId = it.parentAnchorId
            accuracyInput = it.accuracy.toString()
            altitudeInput = it.altitude.toString()
            speedInput = it.speed.toString()
            bearingInput = it.bearing.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (locationId == 0L) "Save Location" else "Edit Location") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val lat = latInput.toDoubleOrNull()
                            val lng = lngInput.toDoubleOrNull()
                            if (name.isBlank()) {
                                error = "Location name is required"
                                return@IconButton
                            }
                            if (lat == null || lat < -90 || lat > 90) {
                                error = "Latitude must be between -90 and 90"
                                return@IconButton
                            }
                            if (lng == null || lng < -180 || lng > 180) {
                                error = "Longitude must be between -180 and 180"
                                return@IconButton
                            }
                            val rad = radiusInput.toDoubleOrNull() ?: 500.0
                            viewModel.save(
                                id = locationId,
                                name = name,
                                latitude = lat,
                                longitude = lng,
                                type = locationType,
                                radiusMeters = if (locationType == LocationType.CENTER_ANCHOR) rad else null,
                                parentAnchorId = if (locationType == LocationType.MOCK_TARGET) selectedParentAnchorId else null,
                                accuracy = accuracyInput.toFloatOrNull() ?: 5f,
                                altitude = altitudeInput.toDoubleOrNull() ?: 0.0,
                                speed = speedInput.toFloatOrNull() ?: 0f,
                                bearing = bearingInput.toFloatOrNull() ?: 0f,
                                onDone = onSaved
                            )
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Location Category", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = locationType == LocationType.MOCK_TARGET,
                    onClick = { locationType = LocationType.MOCK_TARGET },
                    label = { Text("Mock Target") }
                )
                FilterChip(
                    selected = locationType == LocationType.CENTER_ANCHOR,
                    onClick = { locationType = LocationType.CENTER_ANCHOR },
                    label = { Text("Center Anchor (Radius)") }
                )
            }

            activeProfile?.let { preset ->
                AssistChip(
                    onClick = {
                        latInput = preset.latitude.toString()
                        lngInput = preset.longitude.toString()
                        radiusInput = preset.radiusMeters.toInt().toString()
                        accuracyInput = UnitConverter.formatTwoDecimals(preset.accuracyMeters)
                        altitudeInput = UnitConverter.formatTwoDecimals(preset.altitudeMeters)
                        speedInput = preset.speedKmh.toInt().toString()
                        bearingInput = UnitConverter.formatTwoDecimals(preset.bearingDegrees)
                    },
                    label = { Text("Terapkan dari Preset: ${preset.name}") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = { Text("Location Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = latInput,
                onValueChange = {
                    latInput = it
                    error = null
                },
                label = { Text("Latitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = lngInput,
                onValueChange = {
                    lngInput = it
                    error = null
                },
                label = { Text("Longitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            if (locationType == LocationType.CENTER_ANCHOR) {
                OutlinedTextField(
                    value = radiusInput,
                    onValueChange = { radiusInput = it },
                    label = { Text("Radius Limit (Meters)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // If MOCK_TARGET, allow selecting parent anchor if any anchors exist
                if (availableAnchors.isNotEmpty()) {
                    val selectedAnchorName = availableAnchors.find { it.id == selectedParentAnchorId }?.name
                        ?: "None (Standalone Mock)"

                    ExposedDropdownMenuBox(
                        expanded = isAnchorDropdownExpanded,
                        onExpandedChange = { isAnchorDropdownExpanded = !isAnchorDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedAnchorName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Parent Center Anchor (Optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAnchorDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isAnchorDropdownExpanded,
                            onDismissRequest = { isAnchorDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Standalone Mock)") },
                                onClick = {
                                    selectedParentAnchorId = null
                                    isAnchorDropdownExpanded = false
                                }
                            )
                            availableAnchors.forEach { anchor ->
                                DropdownMenuItem(
                                    text = { Text("${anchor.name} (${anchor.radiusMeters?.toInt() ?: 500}m)") },
                                    onClick = {
                                        selectedParentAnchorId = anchor.id
                                        isAnchorDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }

            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Hide Advanced Settings ▲" else "Show Advanced Settings ▼")
            }

            if (showAdvanced) {
                OutlinedTextField(
                    value = accuracyInput,
                    onValueChange = { accuracyInput = it },
                    label = { Text("Accuracy (meters)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = altitudeInput,
                    onValueChange = { altitudeInput = it },
                    label = { Text("Altitude (meters)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = speedInput,
                    onValueChange = { speedInput = it },
                    label = { Text("Speed (km/h)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bearingInput,
                    onValueChange = { bearingInput = it },
                    label = { Text("Bearing (0-360°)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
