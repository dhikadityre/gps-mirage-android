package com.dhikadityre.gpsmirage.feature.locations

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikadityre.dev.tools.R
import com.dhikadityre.gpsmirage.core.geo.MapIntentHelper
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
    val context = LocalContext.current
    var selectedAnchorForGenerate by remember { mutableStateOf<Location?>(null) }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.exitSelectionMode()
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    selectedAnchorForGenerate?.let { anchor ->
        GenerateSubMockDialog(
            anchor = anchor,
            onDismiss = { selectedAnchorForGenerate = null },
            onConfirm = { count ->
                viewModel.generateSubMocks(anchor, count)
                selectedAnchorForGenerate = null
            }
        )
    }

    // Determine currently visible locations for active tab & filters
    val currentVisibleLocations: List<Location> = remember(
        uiState.selectedTab,
        uiState.anchors,
        uiState.mockTargets,
        uiState.nativeLocations,
        uiState.groupedList,
        uiState.isFilterFavoriteOnly,
        uiState.selectedFolderId,
        uiState.isSearchActive,
        uiState.searchQuery,
        uiState.searchResults
    ) {
        val baseList = when (uiState.selectedTab) {
            LocationsTab.ANCHORS -> if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
                uiState.searchResults.filter { it.type == LocationType.CENTER_ANCHOR }
            } else uiState.anchors
            LocationsTab.MOCK_TARGETS -> if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
                uiState.searchResults.filter { it.type == LocationType.MOCK_TARGET }
            } else uiState.mockTargets
            LocationsTab.GROUPED -> if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
                uiState.searchResults.filter { it.type == LocationType.CENTER_ANCHOR }
            } else uiState.groupedList.map { it.anchor }
            LocationsTab.NATIVE -> if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
                uiState.searchResults.filter { it.type == LocationType.NATIVE }
            } else uiState.nativeLocations
        }
        filterLocations(baseList, uiState.isFilterFavoriteOnly, uiState.selectedFolderId)
    }

    val visibleIds = remember(currentVisibleLocations) { currentVisibleLocations.map { it.id } }
    val groupedChildMap: Map<Long, List<Long>> = remember(uiState.groupedList) {
        uiState.groupedList.associate { it.anchor.id to it.mocks.map { m -> m.id } }
    }

    val allTargetIds: Set<Long> = remember(visibleIds, groupedChildMap, uiState.selectedTab) {
        val set = mutableSetOf<Long>()
        set.addAll(visibleIds)
        if (uiState.selectedTab == LocationsTab.GROUPED) {
            visibleIds.forEach { anchorId ->
                groupedChildMap[anchorId]?.let { set.addAll(it) }
            }
        }
        set
    }

    val isAllSelected = allTargetIds.isNotEmpty() && uiState.selectedLocationIds.containsAll(allTargetIds)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.locations_selected_count, uiState.selectedLocationIds.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleSelectAll(visibleIds, groupedChildMap) }) {
                            Icon(
                                if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (isAllSelected) "Deselect All" else "Select All"
                            )
                        }
                        IconButton(
                            onClick = { viewModel.onRequestBulkDelete() },
                            enabled = uiState.selectedLocationIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus Terpilih",
                                tint = if (uiState.selectedLocationIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        if (uiState.isSearchActive) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text(stringResource(R.string.locations_search_placeholder)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(stringResource(R.string.locations_title))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.enterSelectionMode() }) {
                            Icon(Icons.Default.Checklist, contentDescription = null)
                        }
                        IconButton(onClick = { viewModel.onToggleSearch() }) {
                            Icon(
                                if (uiState.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = stringResource(R.string.action_search)
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = onAddLocation,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row: Anchors, Mock Targets, Grouped, Native
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = uiState.selectedTab == LocationsTab.ANCHORS,
                    enabled = !uiState.isSelectionMode,
                    onClick = { if (!uiState.isSelectionMode) viewModel.onTabSelected(LocationsTab.ANCHORS) },
                    text = { Text(stringResource(R.string.locations_tab_anchors, uiState.anchors.size)) }
                )
                Tab(
                    selected = uiState.selectedTab == LocationsTab.MOCK_TARGETS,
                    enabled = !uiState.isSelectionMode,
                    onClick = { if (!uiState.isSelectionMode) viewModel.onTabSelected(LocationsTab.MOCK_TARGETS) },
                    text = { Text(stringResource(R.string.locations_tab_mock_targets, uiState.mockTargets.size)) }
                )
                Tab(
                    selected = uiState.selectedTab == LocationsTab.GROUPED,
                    enabled = !uiState.isSelectionMode,
                    onClick = { if (!uiState.isSelectionMode) viewModel.onTabSelected(LocationsTab.GROUPED) },
                    text = { Text(stringResource(R.string.locations_tab_grouped, uiState.groupedList.size)) }
                )
                Tab(
                    selected = uiState.selectedTab == LocationsTab.NATIVE,
                    enabled = !uiState.isSelectionMode,
                    onClick = { if (!uiState.isSelectionMode) viewModel.onTabSelected(LocationsTab.NATIVE) },
                    text = { Text(stringResource(R.string.locations_tab_native, uiState.nativeLocations.size)) }
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
                                val isSelected = uiState.selectedLocationIds.contains(anchor.id)
                                LocationCardItem(
                                    location = anchor,
                                    tagLabel = "Center Anchor",
                                    tagContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    tagContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    radiusText = "Radius: ${(anchor.radiusMeters ?: 500.0).toInt()}m",
                                    duplicateAnchor = null,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = isSelected,
                                    onToggleSelect = { viewModel.toggleSelectLocation(anchor.id) },
                                    onLongClick = {
                                        if (!uiState.isSelectionMode) {
                                            viewModel.enterSelectionMode(anchor.id)
                                        }
                                    },
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleSelectLocation(anchor.id)
                                        } else {
                                            onLocationClick(anchor.id)
                                        }
                                    },
                                    onToggleFavorite = { viewModel.onToggleFavorite(anchor.id, anchor.isFavorite) },
                                    onDelete = { viewModel.onDeleteRequest(anchor) },
                                    onOpenGoogleMaps = {
                                        MapIntentHelper.launchMaps(context, anchor.latitude, anchor.longitude, anchor.name)
                                    },
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
                                val isSelected = uiState.selectedLocationIds.contains(mockLocation.id)
                                LocationCardItem(
                                    location = mockLocation,
                                    tagLabel = "Mock Target",
                                    tagContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    tagContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    radiusText = null,
                                    duplicateAnchor = duplicateAnchor,
                                    parentAnchorName = parentAnchor?.name,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = isSelected,
                                    onToggleSelect = { viewModel.toggleSelectLocation(mockLocation.id) },
                                    onLongClick = {
                                        if (!uiState.isSelectionMode) {
                                            viewModel.enterSelectionMode(mockLocation.id)
                                        }
                                    },
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleSelectLocation(mockLocation.id)
                                        } else {
                                            onLocationClick(mockLocation.id)
                                        }
                                    },
                                    onToggleFavorite = { viewModel.onToggleFavorite(mockLocation.id, mockLocation.isFavorite) },
                                    onDelete = { viewModel.onDeleteRequest(mockLocation) },
                                    onOpenGoogleMaps = {
                                        MapIntentHelper.launchMaps(context, mockLocation.latitude, mockLocation.longitude, mockLocation.name)
                                    },
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
                                val isAnchorSelected = uiState.selectedLocationIds.contains(anchorWithMocks.anchor.id)
                                val childIds = anchorWithMocks.mocks.map { it.id }
                                GroupedAnchorCardItem(
                                    item = anchorWithMocks,
                                    isExpanded = isExpanded,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isAnchorSelected = isAnchorSelected,
                                    selectedSubMockIds = uiState.selectedLocationIds,
                                    onToggleExpand = { viewModel.onToggleAnchorExpanded(anchorWithMocks.anchor.id) },
                                    onToggleSelectAnchor = {
                                        viewModel.toggleSelectLocation(anchorWithMocks.anchor.id, isAnchor = true, childIds = childIds)
                                    },
                                    onToggleSelectSubMock = { subMockId ->
                                        viewModel.toggleSelectLocation(subMockId)
                                    },
                                    onLongClickAnchor = {
                                        if (!uiState.isSelectionMode) {
                                            viewModel.enterSelectionMode(anchorWithMocks.anchor.id)
                                        }
                                    },
                                    onLongClickSubMock = { subMockId ->
                                        if (!uiState.isSelectionMode) {
                                            viewModel.enterSelectionMode(subMockId)
                                        }
                                    },
                                    onAnchorClick = { onLocationClick(anchorWithMocks.anchor.id) },
                                    onPreviewAnchor = {
                                        viewModel.onPreviewLocation(anchorWithMocks.anchor, parentAnchor = null, onNavigateToMap)
                                    },
                                    onGenerateSubMock = {
                                        selectedAnchorForGenerate = anchorWithMocks.anchor
                                    },
                                    onOpenGoogleMapsAnchor = {
                                        MapIntentHelper.launchMaps(context, anchorWithMocks.anchor.latitude, anchorWithMocks.anchor.longitude, anchorWithMocks.anchor.name)
                                    },
                                    onSubMockClick = onLocationClick,
                                    onPreviewSubMock = { subMock ->
                                        viewModel.onPreviewLocation(subMock, parentAnchor = anchorWithMocks.anchor, onNavigateToMap)
                                    },
                                    onOpenGoogleMapsSubMock = { subMock ->
                                        MapIntentHelper.launchMaps(context, subMock.latitude, subMock.longitude, subMock.name)
                                    },
                                    onToggleFavorite = { loc -> viewModel.onToggleFavorite(loc.id, loc.isFavorite) },
                                    onDeleteLocation = { loc -> viewModel.onDeleteRequest(loc) }
                                )
                            }
                        }
                    }
                }

                LocationsTab.NATIVE -> {
                    val filteredNatives = filterLocations(
                        list = if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
                            uiState.searchResults.filter { it.type == LocationType.NATIVE }
                        } else {
                            uiState.nativeLocations
                        },
                        favoriteOnly = uiState.isFilterFavoriteOnly,
                        folderId = uiState.selectedFolderId
                    )

                    if (filteredNatives.isEmpty()) {
                        EmptyLocationState(
                            title = "No Native Locations yet",
                            message = "Capture and save your unmocked physical hardware GPS baseline coordinates.",
                            buttonText = "Open Map",
                            onAction = onNavigateToMap
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredNatives, key = { it.id }) { nativeLocation ->
                                val isSelected = uiState.selectedLocationIds.contains(nativeLocation.id)
                                LocationCardItem(
                                    location = nativeLocation,
                                    tagLabel = "Native",
                                    tagContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    tagContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    radiusText = nativeLocation.radiusMeters?.let { "Radius ${it.toInt()}m" },
                                    duplicateAnchor = null,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = isSelected,
                                    onToggleSelect = { viewModel.toggleSelectLocation(nativeLocation.id) },
                                    onLongClick = {
                                        if (!uiState.isSelectionMode) {
                                            viewModel.enterSelectionMode(nativeLocation.id)
                                        }
                                    },
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleSelectLocation(nativeLocation.id)
                                        } else {
                                            onLocationClick(nativeLocation.id)
                                        }
                                    },
                                    onToggleFavorite = { viewModel.onToggleFavorite(nativeLocation.id, nativeLocation.isFavorite) },
                                    onDelete = { viewModel.onDeleteRequest(nativeLocation) },
                                    onOpenGoogleMaps = {
                                        MapIntentHelper.launchMaps(context, nativeLocation.latitude, nativeLocation.longitude, nativeLocation.name)
                                    },
                                    onPreview = {
                                        viewModel.onPreviewLocation(nativeLocation, parentAnchor = null, onNavigateToMap)
                                    },
                                    onLinkToAnchor = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showBulkDeleteDialog) {
        val allLocations = (uiState.anchors + uiState.mockTargets + uiState.nativeLocations).distinctBy { it.id }
        val selectedLocations = allLocations.filter { uiState.selectedLocationIds.contains(it.id) }
        val hasSelectedAnchor = selectedLocations.any { it.type == LocationType.CENTER_ANCHOR }

        AlertDialog(
            onDismissRequest = { viewModel.onDismissBulkDelete() },
            icon = {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(stringResource(R.string.locations_delete_selected_title, uiState.selectedLocationIds.size))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.locations_delete_selected_message, uiState.selectedLocationIds.size),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            selectedLocations.take(4).forEach { loc ->
                                Text(
                                    text = "• ${loc.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (selectedLocations.size > 4) {
                                Text(
                                    text = "  ...dan ${selectedLocations.size - 4} lokasi lainnya",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (hasSelectedAnchor) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onToggleDeleteLinkedChildren(!uiState.deleteLinkedChildren) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = uiState.deleteLinkedChildren,
                                onCheckedChange = { viewModel.onToggleDeleteLinkedChildren(it) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.locations_delete_linked_targets),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onConfirmBulkDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.locations_delete_selected_btn, uiState.selectedLocationIds.size))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissBulkDelete() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    uiState.locationToDelete?.let { location ->
        AlertDialog(
            onDismissRequest = { viewModel.onDeleteDismissed() },
            icon = {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Delete Location?") },
            text = { Text("Are you sure you want to delete \"${location.name}\"?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.onDeleteConfirmed() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun LocationCardItem(
    location: Location,
    tagLabel: String,
    tagContainerColor: androidx.compose.ui.graphics.Color,
    tagContentColor: androidx.compose.ui.graphics.Color,
    radiusText: String?,
    duplicateAnchor: Location?,
    parentAnchorName: String? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onOpenGoogleMaps: () -> Unit = {},
    onPreview: () -> Unit,
    onLinkToAnchor: (Long) -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        colors = if (isSelected) {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            )
        } else {
            CardDefaults.elevatedCardColors()
        }
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
                    if (location.isGenerated) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Generated",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
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

                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.size(32.dp)
                    )
                } else {
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
                                    text = { Text(stringResource(R.string.locations_btn_open_google_maps)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        showMenu = false
                                        onOpenGoogleMaps()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_edit)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        showMenu = false
                                        onClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.location_delete)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
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
                                Text(stringResource(R.string.locations_btn_delete_duplicates), fontSize = 12.sp, maxLines = 1)
                            }
                            Button(
                                onClick = { onLinkToAnchor(duplicateAnchor.id) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(stringResource(R.string.locations_btn_link_anchor), fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun GroupedAnchorCardItem(
    item: AnchorWithMocks,
    isExpanded: Boolean,
    isSelectionMode: Boolean = false,
    isAnchorSelected: Boolean = false,
    selectedSubMockIds: Set<Long> = emptySet(),
    onToggleExpand: () -> Unit,
    onToggleSelectAnchor: () -> Unit = {},
    onToggleSelectSubMock: (Long) -> Unit = {},
    onLongClickAnchor: () -> Unit = {},
    onLongClickSubMock: (Long) -> Unit = {},
    onAnchorClick: () -> Unit,
    onPreviewAnchor: () -> Unit,
    onGenerateSubMock: () -> Unit,
    onOpenGoogleMapsAnchor: () -> Unit,
    onSubMockClick: (Long) -> Unit,
    onPreviewSubMock: (Location) -> Unit,
    onOpenGoogleMapsSubMock: (Location) -> Unit,
    onToggleFavorite: (Location) -> Unit,
    onDeleteLocation: (Location) -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isAnchorSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            ),
        shape = shape,
        colors = if (isAnchorSelected) {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            )
        } else {
            CardDefaults.elevatedCardColors()
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Parent Center Anchor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelectAnchor()
                            } else {
                                onToggleExpand()
                            }
                        },
                        onLongClick = onLongClickAnchor
                    )
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

                    if (isSelectionMode) {
                        Checkbox(
                            checked = isAnchorSelected,
                            onCheckedChange = { onToggleSelectAnchor() },
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
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

                            var showAnchorMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { showAnchorMenu = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Menu Anchor",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showAnchorMenu,
                                    onDismissRequest = { showAnchorMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.locations_btn_generate_sub_mock)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            showAnchorMenu = false
                                            onGenerateSubMock()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.locations_btn_open_google_maps)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            showAnchorMenu = false
                                            onOpenGoogleMapsAnchor()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_edit)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            showAnchorMenu = false
                                            onAnchorClick()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.location_delete)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            showAnchorMenu = false
                                            onDeleteLocation(item.anchor)
                                        }
                                    )
                                }
                            }
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
                            val isSubMockSelected = selectedSubMockIds.contains(subMock.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelectionMode) {
                                                onToggleSelectSubMock(subMock.id)
                                            } else {
                                                onSubMockClick(subMock.id)
                                            }
                                        },
                                        onLongClick = { onLongClickSubMock(subMock.id) }
                                    ),
                                colors = if (isSubMockSelected) {
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    )
                                } else {
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                },
                                border = if (isSubMockSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
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
                                            if (subMock.isGenerated) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Text(
                                                        text = "Generated",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
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

                                    if (isSelectionMode) {
                                        Checkbox(
                                            checked = isSubMockSelected,
                                            onCheckedChange = { onToggleSelectSubMock(subMock.id) },
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
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

                                            var showSubMockMenu by remember { mutableStateOf(false) }
                                            Box {
                                                IconButton(
                                                    onClick = { showSubMockMenu = true },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.MoreVert,
                                                        contentDescription = "Menu Sub-Mock",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showSubMockMenu,
                                                    onDismissRequest = { showSubMockMenu = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.locations_btn_open_google_maps)) },
                                                        leadingIcon = {
                                                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        },
                                                        onClick = {
                                                            showSubMockMenu = false
                                                            onOpenGoogleMapsSubMock(subMock)
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.action_edit)) },
                                                        leadingIcon = {
                                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        },
                                                        onClick = {
                                                            showSubMockMenu = false
                                                            onSubMockClick(subMock.id)
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.location_delete)) },
                                                        leadingIcon = {
                                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        },
                                                        onClick = {
                                                            showSubMockMenu = false
                                                            onDeleteLocation(subMock)
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
            }
        }
    }
}

@Composable
fun GenerateSubMockDialog(
    anchor: Location,
    onDismiss: () -> Unit,
    onConfirm: (count: Int) -> Unit
) {
    var count by remember { mutableStateOf(3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Generate Sub-Mock",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Buat titik sub-mock acak realistis di dalam radius ${anchor.radiusMeters?.toInt() ?: 500}m dari Center Anchor \"${anchor.name}\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.locations_generate_count_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { if (count > 1) count-- },
                                enabled = count > 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 20.dp)
                            )
                            FilledTonalIconButton(
                                onClick = { if (count < 10) count++ },
                                enabled = count < 10,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }

                Text(
                    text = "• Telemetri dihitung realistis (ketinggian tanah perkotaan 10m - 25m, akurasi 3.5m - 8.5m, posisi diam 0 m/s).\n• Titik hasil generate ditandai dengan label 'Generated'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(count) }
            ) {
                Text(stringResource(R.string.locations_generate_count_btn, count))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
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
