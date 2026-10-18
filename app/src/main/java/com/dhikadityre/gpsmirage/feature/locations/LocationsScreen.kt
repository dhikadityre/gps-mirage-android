package com.dhikadityre.gpsmirage.feature.locations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikadityre.gpsmirage.core.model.AnchorWithMocks
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.LocationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    onLocationClick: (Long) -> Unit,
    onAddLocation: () -> Unit = {},
    onNavigateToMap: () -> Unit,
    viewModel: LocationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        Text("Locations & Radius Anchors")
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddLocation,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Location")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row: Anchors, Mock Targets, Grouped
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                Tab(
                    selected = uiState.selectedTab == LocationsTab.ANCHORS,
                    onClick = { viewModel.onTabSelected(LocationsTab.ANCHORS) },
                    text = { Text("Anchors (${uiState.anchors.size})") }
                )
                Tab(
                    selected = uiState.selectedTab == LocationsTab.MOCK_TARGETS,
                    onClick = { viewModel.onTabSelected(LocationsTab.MOCK_TARGETS) },
                    text = { Text("Mock Targets (${uiState.mockTargets.size})") }
                )
                Tab(
                    selected = uiState.selectedTab == LocationsTab.GROUPED,
                    onClick = { viewModel.onTabSelected(LocationsTab.GROUPED) },
                    text = { Text("Grouped (${uiState.groupedList.size})") }
                )
            }

            // Filter Row (Favorites & Folders)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.isFilterFavoriteOnly,
                    onClick = { viewModel.onToggleFavoriteFilter() },
                    label = { Text("Favorites") },
                    leadingIcon = {
                        Icon(
                            if (uiState.isFilterFavoriteOnly) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                if (uiState.folders.isNotEmpty()) {
                    var showFolderMenu by remember { mutableStateOf(false) }
                    val currentFolderName = uiState.folders.find { it.id == uiState.selectedFolderId }?.name ?: "All Folders"

                    Box {
                        FilterChip(
                            selected = uiState.selectedFolderId != null,
                            onClick = { showFolderMenu = true },
                            label = { Text(currentFolderName) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                        )
                        DropdownMenu(
                            expanded = showFolderMenu,
                            onDismissRequest = { showFolderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Folders") },
                                onClick = {
                                    viewModel.onFolderSelected(null)
                                    showFolderMenu = false
                                }
                            )
                            uiState.folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = {
                                        viewModel.onFolderSelected(folder.id)
                                        showFolderMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            when (uiState.selectedTab) {
                LocationsTab.ANCHORS -> {
                    val filteredAnchors = filterLocations(
                        list = if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
                            uiState.searchResults.filter { it.type == LocationType.CENTER_ANCHOR }
                        } else {
                            uiState.anchors
                        },
                        favoriteOnly = uiState.isFilterFavoriteOnly,
                        folderId = uiState.selectedFolderId
                    )

                    if (filteredAnchors.isEmpty()) {
                        EmptyLocationState(
                            title = "No Center Anchors yet",
                            message = "Create Center Anchors to preview radius geofence areas and set boundary centers.",
                            buttonText = "Add Anchor",
                            onAction = onAddLocation
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredAnchors, key = { it.id }) { anchor ->
                                LocationCardItem(
                                    location = anchor,
                                    tagLabel = "Center Anchor",
                                    tagContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    tagContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    radiusText = "Radius: ${(anchor.radiusMeters ?: 500.0).toInt()}m",
                                    duplicateAnchor = null,
                                    onClick = { onLocationClick(anchor.id) },
                                    onToggleFavorite = { viewModel.onToggleFavorite(anchor.id, anchor.isFavorite) },
                                    onDelete = { viewModel.onDeleteRequest(anchor) },
                                    onPreview = {
                                        viewModel.onPreviewLocation(anchor, parentAnchor = null, onNavigateToMap)
                                    },
                                    onLinkToAnchor = {}
                                )
                            }
                        }
                    }
                }

                LocationsTab.MOCK_TARGETS -> {
                    val filteredMocks = filterLocations(
                        list = if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
                            uiState.searchResults.filter { it.type == LocationType.MOCK_TARGET }
                        } else {
                            uiState.mockTargets
                        },
                        favoriteOnly = uiState.isFilterFavoriteOnly,
                        folderId = uiState.selectedFolderId
                    )

                    if (filteredMocks.isEmpty()) {
                        EmptyLocationState(
                            title = "No Mock Targets yet",
                            message = "Save target locations that you want to mock on the map.",
                            buttonText = "Open Map",
                            onAction = onNavigateToMap
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredMocks, key = { it.id }) { mockLocation ->
                                val duplicateAnchor = uiState.duplicateAnchorMap[mockLocation.id]
                                val parentAnchor = mockLocation.parentAnchorId?.let { pid ->
                                    uiState.anchors.find { it.id == pid }
                                }
                                LocationCardItem(
                                    location = mockLocation,
                                    tagLabel = "Mock Target",
                                    tagContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    tagContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    radiusText = null,
                                    duplicateAnchor = duplicateAnchor,
                                    parentAnchorName = parentAnchor?.name,
                                    onClick = { onLocationClick(mockLocation.id) },
                                    onToggleFavorite = { viewModel.onToggleFavorite(mockLocation.id, mockLocation.isFavorite) },
                                    onDelete = { viewModel.onDeleteRequest(mockLocation) },
                                    onPreview = {
                                        viewModel.onPreviewLocation(mockLocation, parentAnchor = parentAnchor, onNavigateToMap)
                                    },
                                    onLinkToAnchor = { anchorId ->
                                        viewModel.onLinkToAnchor(mockLocation.id, anchorId)
                                    }
                                )
                            }
                        }
                    }
                }

                LocationsTab.GROUPED -> {
                    if (uiState.groupedList.isEmpty()) {
                        EmptyLocationState(
                            title = "No Grouped Anchors yet",
                            message = "Create Center Anchors and link Mock Targets inside their radius.",
                            buttonText = "Add Anchor",
                            onAction = onAddLocation
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.groupedList, key = { it.anchor.id }) { anchorWithMocks ->
                                val isExpanded = uiState.expandedAnchorIds.contains(anchorWithMocks.anchor.id)
                                GroupedAnchorCardItem(
                                    item = anchorWithMocks,
                                    isExpanded = isExpanded,
                                    onToggleExpand = { viewModel.onToggleAnchorExpanded(anchorWithMocks.anchor.id) },
                                    onAnchorClick = { onLocationClick(anchorWithMocks.anchor.id) },
                                    onPreviewAnchor = {
                                        viewModel.onPreviewLocation(anchorWithMocks.anchor, parentAnchor = null, onNavigateToMap)
                                    },
                                    onSubMockClick = onLocationClick,
                                    onPreviewSubMock = { subMock ->
                                        viewModel.onPreviewLocation(subMock, parentAnchor = anchorWithMocks.anchor, onNavigateToMap)
                                    },
                                    onToggleFavorite = { loc -> viewModel.onToggleFavorite(loc.id, loc.isFavorite) },
                                    onDeleteLocation = { loc -> viewModel.onDeleteRequest(loc) }
                                )
                            }
                        }
                    }
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

private fun filterLocations(
    list: List<Location>,
    favoriteOnly: Boolean,
    folderId: Long?
): List<Location> {
    return list.filter { item ->
        (!favoriteOnly || item.isFavorite) &&
        (folderId == null || item.folderId == folderId)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocationCardItem(
    location: Location,
    tagLabel: String,
    tagContainerColor: androidx.compose.ui.graphics.Color,
    tagContentColor: androidx.compose.ui.graphics.Color,
    radiusText: String?,
    duplicateAnchor: Location?,
    parentAnchorName: String? = null,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onPreview: () -> Unit,
    onLinkToAnchor: (Long) -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Tags on left with FlowRow, Action Buttons on right with compact size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = tagContainerColor
                    ) {
                        Text(
                            text = tagLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = tagContentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (parentAnchorName != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Terkait: $parentAnchorName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (radiusText != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = radiusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Compact Actions on Top-Right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (location.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (location.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onPreview,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Preview on Map",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    onClick()
                                }
                            )
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

            Spacer(Modifier.height(8.dp))

            // Title & Coordinate occupy full width!
            Text(
                text = location.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = location.displayCoordinate,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Warning Badge when duplicate coordinates are detected with a Center Anchor
            if (duplicateAnchor != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Identik dengan Center Anchor: \"${duplicateAnchor.name}\"",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Hapus Duplikat", fontSize = 12.sp, maxLines = 1)
                            }
                            Button(
                                onClick = { onLinkToAnchor(duplicateAnchor.id) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Kaitkan Anchor", fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupedAnchorCardItem(
    item: AnchorWithMocks,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAnchorClick: () -> Unit,
    onPreviewAnchor: () -> Unit,
    onSubMockClick: (Long) -> Unit,
    onPreviewSubMock: (Location) -> Unit,
    onToggleFavorite: (Location) -> Unit,
    onDeleteLocation: (Location) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Parent Center Anchor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(14.dp)
            ) {
                // Top Row: Badges on left with FlowRow, Action Buttons on right with compact size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Center Anchor",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Radius ${(item.anchor.radiusMeters ?: 500.0).toInt()}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "${item.mocks.size} Sub-mocks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Compact Action Buttons on Top-Right
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onPreviewAnchor,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Preview Anchor on Map",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleExpand,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Title & Coordinate (Full card width with ellipsis)
                Text(
                    text = item.anchor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.anchor.displayCoordinate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded Sub-mocks list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider()
                    Spacer(Modifier.height(2.dp))

                    if (item.mocks.isEmpty()) {
                        Text(
                            text = "Belum ada sub-mock location yang terhubung ke Center Anchor ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        item.mocks.forEach { subMock ->
                            Card(
                                onClick = { onSubMockClick(subMock.id) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Text(
                                                text = "Sub-Mock",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = subMock.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = subMock.displayCoordinate,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onToggleFavorite(subMock) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                if (subMock.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "Favorite",
                                                tint = if (subMock.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onPreviewSubMock(subMock) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Preview Sub-Mock on Map",
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(18.dp)
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
