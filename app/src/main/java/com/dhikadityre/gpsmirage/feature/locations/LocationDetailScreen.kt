package com.dhikadityre.gpsmirage.feature.locations

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.LocationType
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import com.dhikadityre.gpsmirage.domain.location.MapPreviewCoordinator
import com.dhikadityre.gpsmirage.domain.location.MockLocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationDetailViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val mockLocationManager: MockLocationManager,
    private val mapPreviewCoordinator: MapPreviewCoordinator
) : ViewModel() {

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    fun loadLocation(id: Long) {
        viewModelScope.launch {
            _location.value = locationRepository.getById(id)
        }
    }

    fun toggleFavorite() {
        val current = _location.value ?: return
        viewModelScope.launch {
            locationRepository.updateFavorite(current.id, !current.isFavorite)
            _location.value = current.copy(isFavorite = !current.isFavorite)
        }
    }

    fun deleteLocation(onDeleted: () -> Unit) {
        val current = _location.value ?: return
        viewModelScope.launch {
            locationRepository.delete(current.id)
            onDeleted()
        }
    }

    fun previewOnMap(onNavigateToMap: () -> Unit) {
        val current = _location.value ?: return
        viewModelScope.launch {
            val parent = current.parentAnchorId?.let { pid -> locationRepository.getById(pid) }
            mapPreviewCoordinator.requestPreview(current, parent)
            onNavigateToMap()
        }
    }

    fun startMock() {
        val current = _location.value ?: return
        mockLocationManager.start(current)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    locationId: Long,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    onNavigateToMap: () -> Unit = onBack,
    viewModel: LocationDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(locationId) {
        viewModel.loadLocation(locationId)
    }

    val location by viewModel.location.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(locationId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        val loc = location
        if (loc == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                        color = when (loc.type) {
                            LocationType.CENTER_ANCHOR -> MaterialTheme.colorScheme.primaryContainer
                            LocationType.MOCK_TARGET -> MaterialTheme.colorScheme.secondaryContainer
                            LocationType.NATIVE -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    ) {
                        Text(
                            text = when (loc.type) {
                                LocationType.CENTER_ANCHOR -> "Center Anchor (Radius Acuan)"
                                LocationType.MOCK_TARGET -> "Mock Target"
                                LocationType.NATIVE -> "Native Benchmark"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (loc.type) {
                                LocationType.CENTER_ANCHOR -> MaterialTheme.colorScheme.onPrimaryContainer
                                LocationType.MOCK_TARGET -> MaterialTheme.colorScheme.onSecondaryContainer
                                LocationType.NATIVE -> MaterialTheme.colorScheme.onTertiaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = loc.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { viewModel.toggleFavorite() }) {
                    Icon(
                        if (loc.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (loc.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Coordinates", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        loc.displayCoordinate,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    if (loc.type == LocationType.CENTER_ANCHOR) {
                        Spacer(Modifier.height(8.dp))
                        Text("Radius Geofence: ${(loc.radiusMeters ?: 500.0).toInt()} meter", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Hide Advanced ▲" else "Show Advanced ▼")
            }

            if (showAdvanced) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Accuracy: ${loc.accuracy} m")
                        Text("Altitude: ${loc.altitude} m")
                        Text("Speed: ${loc.speed} km/h")
                        Text("Bearing: ${loc.bearing}°")
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (loc.type == LocationType.CENTER_ANCHOR) {
                Button(
                    onClick = { viewModel.previewOnMap(onNavigateToMap) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Preview Radius on Map", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.previewOnMap(onNavigateToMap) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Preview Map", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { viewModel.startMock() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text("Start Mock", fontWeight = FontWeight.Bold)
                    }
                }
            }

        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Location?") },
            text = { Text("This will permanently remove this location from your saved list.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteLocation(onDeleted = onBack)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
