package com.dhikadityre.gpsmirage.feature.importexport

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.model.Folder
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.domain.folder.FolderRepository
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    init {
        viewModelScope.launch {
            folderRepository.getAllFlow().collect { _folders.value = it }
        }
    }

    fun exportAll(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val locs = locationRepository.getAll()
            onReady(toJson(locs))
        }
    }

    fun exportFavorites(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val favs = locationRepository.getFavorites()
            onReady(toJson(favs))
        }
    }

    fun exportFolder(folder: Folder, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val locs = locationRepository.getByFolder(folder.id)
            onReady(toJson(locs, folder.name))
        }
    }

    private fun toJson(locations: List<Location>, folderName: String? = null): String {
        val listStr = locations.joinToString(",\n") { loc ->
            """    {
      "name": "${loc.name}",
      "latitude": ${loc.latitude},
      "longitude": ${loc.longitude},
      "accuracy": ${loc.accuracy},
      "altitude": ${loc.altitude},
      "speed": ${loc.speed},
      "bearing": ${loc.bearing}
    }"""
        }
        val namePart = if (folderName != null) "\"name\": \"$folderName\",\n  " else ""
        return "{\n  \"version\": 1,\n  $namePart\"locations\": [\n$listStr\n  ]\n}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val folders by viewModel.folders.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Locations") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Export Options", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.exportAll { shareJson(context, "GPS_Mirage_All.json", it) } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export All Locations (JSON)")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.exportFavorites { shareJson(context, "GPS_Mirage_Favorites.json", it) } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Favorites (JSON)")
                    }
                }
            }

            if (folders.isNotEmpty()) {
                Text("Export by Folder", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folders) { folder ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(folder.name, style = MaterialTheme.typography.titleSmall)
                                OutlinedButton(
                                    onClick = {
                                        viewModel.exportFolder(folder) {
                                            shareJson(context, "GPS_Mirage_${folder.name}.json", it)
                                        }
                                    }
                                ) {
                                    Text("Export")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shareJson(context: Context, filename: String, json: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, json)
        putExtra(Intent.EXTRA_TITLE, filename)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share JSON via"))
}
