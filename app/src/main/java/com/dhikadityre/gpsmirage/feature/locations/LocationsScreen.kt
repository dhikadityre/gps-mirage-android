package com.dhikadityre.gpsmirage.feature.locations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.feature.folders.FoldersScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    onLocationClick: (Long) -> Unit,
    onNavigateToMap: () -> Unit,
    viewModel: LocationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSearchActive) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search locations...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Locations")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onToggleSearch() }) {
                        Icon(
                            if (uiState.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                Tab(
                    selected = uiState.selectedTab == LocationsTab.ALL,
                    onClick = { viewModel.onTabSelected(LocationsTab.ALL) },
                    text = { Text("All (${uiState.allLocations.size})") }
                )
                Tab(
                    selected = uiState.selectedTab == LocationsTab.FAVORITES,
                    onClick = { viewModel.onTabSelected(LocationsTab.FAVORITES) },
                    text = { Text("Favorites (${uiState.favorites.size})") }
                )
                Tab(
                    selected = uiState.selectedTab == LocationsTab.FOLDERS,
                    onClick = { viewModel.onTabSelected(LocationsTab.FOLDERS) },
                    text = { Text("Folders (${uiState.folders.size})") }
                )
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            when (uiState.selectedTab) {
                LocationsTab.ALL -> {
                    val displayList = if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
                        uiState.searchResults
                    } else {
                        uiState.allLocations
                    }
                    if (displayList.isEmpty()) {
                        EmptyLocationState(
                            title = if (uiState.isSearchActive) "No results found" else "No saved locations yet.",
                            message = if (uiState.isSearchActive) "Try another search term." else "Pick a location on the map and save it for quick access later.",
                            buttonText = "Open Map",
                            onAction = onNavigateToMap
                        )
                    } else {
                        LocationList(
                            locations = displayList,
                            onLocationClick = onLocationClick,
                            onToggleFavorite = { loc -> viewModel.onToggleFavorite(loc.id, loc.isFavorite) },
                            onDelete = { loc -> viewModel.onDeleteRequest(loc) },
                            onStartMock = { loc -> viewModel.onStartMock(loc) }
                        )
                    }
                }
                LocationsTab.FAVORITES -> {
                    if (uiState.favorites.isEmpty()) {
                        EmptyLocationState(
                            title = "No favorites yet.",
                            message = "Mark your frequently used locations as favorites.",
                            buttonText = "Open Map",
                            onAction = onNavigateToMap
                        )
                    } else {
                        LocationList(
                            locations = uiState.favorites,
                            onLocationClick = onLocationClick,
                            onToggleFavorite = { loc -> viewModel.onToggleFavorite(loc.id, loc.isFavorite) },
                            onDelete = { loc -> viewModel.onDeleteRequest(loc) },
                            onStartMock = { loc -> viewModel.onStartMock(loc) }
                        )
                    }
                }
                LocationsTab.FOLDERS -> {
                    FoldersScreen(onLocationClick = onLocationClick)
                }
            }
        }
    }

    uiState.locationToDelete?.let { location ->
        AlertDialog(
            onDismissRequest = { viewModel.onDeleteDismissed() },
            title = { Text("Delete Location?") },
            text = { Text("Are you sure you want to delete \"${location.name}\"?") },
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
private fun LocationList(
    locations: List<Location>,
    onLocationClick: (Long) -> Unit,
    onToggleFavorite: (Location) -> Unit,
    onDelete: (Location) -> Unit,
    onStartMock: (Location) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(locations, key = { it.id }) { location ->
            LocationCardItem(
                location = location,
                onClick = { onLocationClick(location.id) },
                onToggleFavorite = { onToggleFavorite(location) },
                onDelete = { onDelete(location) },
                onStart = { onStartMock(location) }
            )
        }
    }
}

@Composable
private fun LocationCardItem(
    location: Location,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(location.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    location.displayCoordinate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (location.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (location.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = onStart) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start mock",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLocationState(
    title: String,
    message: String,
    buttonText: String,
    onAction: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction) {
                Text(buttonText)
            }
        }
    }
}
