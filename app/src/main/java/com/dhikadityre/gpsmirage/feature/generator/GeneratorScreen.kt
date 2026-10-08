package com.dhikadityre.gpsmirage.feature.generator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.geo.GeoEngine
import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GeneratorUiState(
    val centerLat: String = "-6.200000",
    val centerLng: String = "106.816000",
    val radiusMeters: String = "500",
    val count: String = "10",
    val generatedPoints: List<Coordinate> = emptyList(),
    val error: String? = null,
    val isSavedSuccess: Boolean = false
)

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val geoEngine: GeoEngine,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    fun updateCenterLat(v: String) = _uiState.update { it.copy(centerLat = v, error = null) }
    fun updateCenterLng(v: String) = _uiState.update { it.copy(centerLng = v, error = null) }
    fun updateRadius(v: String) = _uiState.update { it.copy(radiusMeters = v, error = null) }
    fun updateCount(v: String) = _uiState.update { it.copy(count = v, error = null) }

    fun generate() {
        val lat = _uiState.value.centerLat.toDoubleOrNull()
        val lng = _uiState.value.centerLng.toDoubleOrNull()
        val radius = _uiState.value.radiusMeters.toDoubleOrNull()
        val cnt = _uiState.value.count.toIntOrNull()

        if (lat == null || lat < -90 || lat > 90) {
            _uiState.update { it.copy(error = "Invalid center latitude") }
            return
        }
        if (lng == null || lng < -180 || lng > 180) {
            _uiState.update { it.copy(error = "Invalid center longitude") }
            return
        }
        if (radius == null || radius <= 0) {
            _uiState.update { it.copy(error = "Radius must be greater than 0") }
            return
        }
        if (cnt == null || cnt <= 0 || cnt > 500) {
            _uiState.update { it.copy(error = "Count must be between 1 and 500") }
            return
        }

        val points = geoEngine.generatePointsWithinRadius(Coordinate(lat, lng), radius, cnt)
        _uiState.update { it.copy(generatedPoints = points, isSavedSuccess = false) }
    }

    fun saveAll() {
        val points = _uiState.value.generatedPoints
        if (points.isEmpty()) return

        viewModelScope.launch {
            val locations = points.mapIndexed { idx, coord ->
                Location(
                    name = "Generated %03d".format(idx + 1),
                    latitude = coord.latitude,
                    longitude = coord.longitude
                )
            }
            locationRepository.saveAll(locations)
            _uiState.update { it.copy(isSavedSuccess = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    onBack: () -> Unit,
    viewModel: GeneratorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSavedSuccess) {
        if (uiState.isSavedSuccess) {
            snackbarHostState.showSnackbar("All points saved successfully!")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Coordinate Generator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.centerLat,
                    onValueChange = { viewModel.updateCenterLat(it) },
                    label = { Text("Center Lat") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = uiState.centerLng,
                    onValueChange = { viewModel.updateCenterLng(it) },
                    label = { Text("Center Lng") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.radiusMeters,
                    onValueChange = { viewModel.updateRadius(it) },
                    label = { Text("Radius (meters)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = uiState.count,
                    onValueChange = { viewModel.updateCount(it) },
                    label = { Text("Point Count") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            if (uiState.error != null) {
                Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.generate() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Generate Points")
            }

            if (uiState.generatedPoints.isNotEmpty()) {
                Text(
                    text = "Generated ${uiState.generatedPoints.size} points:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val all = uiState.generatedPoints.joinToString("\n") { "%.6f,%.6f".format(it.latitude, it.longitude) }
                            copyToClipboard(context, "All Points", all)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy All")
                    }
                    OutlinedButton(
                        onClick = {
                            val json = buildJson(uiState.generatedPoints)
                            copyToClipboard(context, "JSON", json)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy JSON")
                    }
                    Button(
                        onClick = { viewModel.saveAll() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save All")
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(uiState.generatedPoints) { index, point ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${index + 1}.  %.6f, %.6f".format(point.latitude, point.longitude),
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(onClick = {
                                    copyToClipboard(context, "Coordinate", "%.6f,%.6f".format(point.latitude, point.longitude))
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

private fun buildJson(points: List<Coordinate>): String {
    val items = points.mapIndexed { idx, p ->
        """    {"name": "Generated %03d", "latitude": %.6f, "longitude": %.6f}""".format(idx + 1, p.latitude, p.longitude)
    }.joinToString(",\n")
    return "{\n  \"version\": 1,\n  \"locations\": [\n$items\n  ]\n}"
}
