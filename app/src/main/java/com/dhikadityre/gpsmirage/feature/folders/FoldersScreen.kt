package com.dhikadityre.gpsmirage.feature.folders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── UI State ─────────────────────────────────────────────────────────────────

data class FoldersUiState(
    val folders: List<Folder> = emptyList(),
    val selectedFolder: Folder? = null,
    val locationsInFolder: List<Location> = emptyList(),
    val isLoading: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val folderToDelete: Folder? = null,
    val newFolderName: String = "",
    val error: String? = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FoldersUiState(isLoading = true))
    val state: StateFlow<FoldersUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            folderRepository.getAllFlow().collect { folders ->
                _state.update { it.copy(folders = folders, isLoading = false) }
            }
        }
    }

    fun onShowCreateDialog() = _state.update { it.copy(showCreateDialog = true, newFolderName = "") }

    fun onDismissCreateDialog() = _state.update {
        it.copy(showCreateDialog = false, newFolderName = "")
    }

    fun onNewFolderNameChanged(name: String) = _state.update { it.copy(newFolderName = name) }

    fun onCreateFolder() {
        val name = _state.value.newFolderName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            folderRepository.create(Folder(name = name))
            _state.update { it.copy(showCreateDialog = false, newFolderName = "") }
        }
    }

    fun onSelectFolder(folder: Folder) {
        _state.update { it.copy(selectedFolder = folder) }
        viewModelScope.launch {
            val locations = locationRepository.getByFolder(folder.id)
            _state.update { it.copy(locationsInFolder = locations) }
        }
    }

    fun onClearFolder() = _state.update { it.copy(selectedFolder = null, locationsInFolder = emptyList()) }

    fun onRenameFolder(folder: Folder, newName: String) {
        viewModelScope.launch {
            folderRepository.rename(folder.id, newName)
        }
    }

    fun onDeleteRequest(folder: Folder) = _state.update { it.copy(folderToDelete = folder) }

    fun onDeleteConfirmed() {
        val folder = _state.value.folderToDelete ?: return
        viewModelScope.launch {
            folderRepository.delete(folder.id)
            _state.update { it.copy(folderToDelete = null) }
        }
    }

    fun onDeleteDismissed() = _state.update { it.copy(folderToDelete = null) }

    fun onRemoveFromFolder(locationId: Long) {
        viewModelScope.launch {
            locationRepository.updateFolder(locationId, null)
            val folder = _state.value.selectedFolder ?: return@launch
            val locations = locationRepository.getByFolder(folder.id)
            _state.update { it.copy(locationsInFolder = locations) }
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onLocationClick: (Long) -> Unit = {},
    viewModel: FoldersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Folder detail view
    if (state.selectedFolder != null) {
        FolderDetailView(
            folder = state.selectedFolder!!,
            locations = state.locationsInFolder,
            onLocationClick = onLocationClick,
            onBack = { viewModel.onClearFolder() },
            onRemoveLocation = { viewModel.onRemoveFromFolder(it) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Folders") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Create Folder")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.folders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Folder, contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text("No folders yet.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Create folders to organize your locations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { viewModel.onShowCreateDialog() }) {
                        Text("Create Folder")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.folders, key = { it.id }) { folder ->
                    FolderCard(
                        folder = folder,
                        onClick = { viewModel.onSelectFolder(folder) },
                        onDelete = { viewModel.onDeleteRequest(folder) }
                    )
                }
            }
        }
    }

    // Create folder dialog
    if (state.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissCreateDialog() },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = state.newFolderName,
                    onValueChange = { viewModel.onNewFolderNameChanged(it) },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onCreateFolder() },
                    enabled = state.newFolderName.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissCreateDialog() }) { Text("Cancel") }
            }
        )
    }

    // Delete confirmation
    state.folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { viewModel.onDeleteDismissed() },
            title = { Text("Delete Folder?") },
            text = { Text("Delete \"${folder.name}\"? Locations inside will not be deleted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onDeleteConfirmed() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDeleteDismissed() }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FolderCard(
    folder: Folder,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(folder.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${folder.locationCount} location${if (folder.locationCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Delete") }, onClick = {
                        showMenu = false
                        onDelete()
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderDetailView(
    folder: Folder,
    locations: List<Location>,
    onLocationClick: (Long) -> Unit,
    onBack: () -> Unit,
    onRemoveLocation: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (locations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No locations in this folder.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(locations, key = { it.id }) { location ->
                    Card(
                        onClick = { onLocationClick(location.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(location.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    location.displayCoordinate,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Remove from Folder") },
                                        onClick = {
                                            showMenu = false
                                            onRemoveLocation(location.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
