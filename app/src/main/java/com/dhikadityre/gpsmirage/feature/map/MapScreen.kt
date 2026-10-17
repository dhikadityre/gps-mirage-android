package com.dhikadityre.gpsmirage.feature.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.dhikadityre.dev.tools.R
import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.RadiusConfig
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.InfoWindow

@Composable
fun MapScreen(
    onNavigateToLocations: () -> Unit,
    onSaveLocation: (Double, Double) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(uiState.event) {
        when (val event = uiState.event) {
            is MapEvent.ShowSnackbar -> {
                scope.launch { snackbarHostState.showSnackbar(event.message) }
                viewModel.clearEvent()
            }
            is MapEvent.MockStarted -> {
                scope.launch { snackbarHostState.showSnackbar("Mock location started") }
                viewModel.clearEvent()
            }
            is MapEvent.MockStopped -> {
                scope.launch { snackbarHostState.showSnackbar("Mock location stopped") }
                viewModel.clearEvent()
            }
            is MapEvent.NavigateToSetupGuide -> {
                // Handled below via showSetupDialog
            }
            null -> Unit
        }
    }

    var showSetupDialog by remember { mutableStateOf(false) }
    var showEnterCoordSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.event) {
        if (uiState.event is MapEvent.NavigateToSetupGuide) {
            showSetupDialog = true
            viewModel.clearEvent()
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { viewModel.toggleQuickLocations() },
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.List, contentDescription = "Quick locations")
                }
                FloatingActionButton(
                    onClick = { showEnterCoordSheet = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Enter coordinates")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OsmMapView(
                modifier = Modifier.fillMaxSize(),
                selectedCoordinate = uiState.selectedCoordinate,
                radiusConfig = uiState.radiusConfig,
                isResolvingAddress = uiState.isResolvingAddress,
                resolvedShortAddress = uiState.resolvedShortAddress,
                onMapTap = { lat, lng -> viewModel.onMapTap(lat, lng) },
                onMapViewReady = { mapViewRef = it }
            )

            if (uiState.isMockActive) {
                MockActiveBanner(
                    location = uiState.activeMockLocation,
                    isLoading = uiState.isLoading,
                    onStop = { viewModel.onStopMock() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (uiState.selectedCoordinate != null && !uiState.isMockActive) {
                SelectedCoordinateCard(
                    coordinate = uiState.selectedCoordinate!!,
                    locationName = uiState.selectedLocationName,
                    isResolvingAddress = uiState.isResolvingAddress,
                    resolvedShortAddress = uiState.resolvedShortAddress,
                    isLoading = uiState.isLoading,
                    radiusConfig = uiState.radiusConfig,
                    distanceToAnchorMeters = uiState.distanceToAnchorMeters,
                    isWithinRadius = uiState.isWithinRadius,
                    onSave = { name ->
                        viewModel.onSaveLocation(name)
                        onSaveLocation(
                            uiState.selectedCoordinate!!.latitude,
                            uiState.selectedCoordinate!!.longitude
                        )
                    },
                    onStart = {
                        viewModel.onStartMock()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }

    if (showSetupDialog) {
        MockNotConfiguredDialog(
            onDismiss = { showSetupDialog = false },
            onOpenSettings = {
                showSetupDialog = false
                try {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        )
    }

    if (showEnterCoordSheet) {
        EnterCoordinatesSheet(
            onDismiss = { showEnterCoordSheet = false },
            onPreview = { lat, lng, _ ->
                mapViewRef?.controller?.animateTo(GeoPoint(lat, lng))
                viewModel.onMapTap(lat, lng)
                showEnterCoordSheet = false
            }
        )
    }

    if (uiState.isQuickLocationsVisible) {
        QuickLocationsSheet(
            recentLocations = uiState.recentLocations,
            favoriteLocations = uiState.favoriteLocations,
            onDismiss = { viewModel.toggleQuickLocations() },
            onLocationTap = { viewModel.onQuickLocationTap(it) }
        )
    }
}

@Composable
private fun OsmMapView(
    modifier: Modifier = Modifier,
    selectedCoordinate: Coordinate?,
    radiusConfig: RadiusConfig,
    isResolvingAddress: Boolean,
    resolvedShortAddress: String?,
    onMapTap: (Double, Double) -> Unit,
    onMapViewReady: (MapView) -> Unit
) {
    var lastAnimatedCoord by remember { mutableStateOf<Coordinate?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setBuiltInZoomControls(true)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                overlays.add(object : org.osmdroid.views.overlay.Overlay() {
                    override fun onSingleTapConfirmed(
                        e: android.view.MotionEvent?,
                        mapView: MapView?
                    ): Boolean {
                        if (e == null || mapView == null) return false
                        val projection = mapView.projection
                        val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                        onMapTap(geoPoint.latitude, geoPoint.longitude)
                        return true
                    }
                })
                onMapViewReady(this)
            }
        },
        update = { mapView ->
            InfoWindow.closeAllInfoWindowsOn(mapView)
            mapView.overlays.removeAll { it is Marker || it is Polygon }

            // Render Geofence Circle Overlay & Anchor Marker if radius constraint is enabled with anchor
            if (radiusConfig.isEnabled && radiusConfig.anchor != null) {
                val anchorGeoPoint = GeoPoint(radiusConfig.anchor.latitude, radiusConfig.anchor.longitude)

                val circlePolygon = Polygon(mapView).apply {
                    points = Polygon.pointsAsCircle(anchorGeoPoint, radiusConfig.radiusMeters)
                    fillPaint.color = android.graphics.Color.argb(45, 33, 150, 243) // Semi-transparent blue
                    outlinePaint.color = android.graphics.Color.argb(220, 25, 118, 210) // Solid blue border
                    outlinePaint.strokeWidth = 3f * mapView.context.resources.displayMetrics.density
                    title = "Geofence Radius (${radiusConfig.radiusMeters.toInt()}m)"
                    setOnClickListener { _, _, _ -> false }
                }
                mapView.overlays.add(circlePolygon)

                val anchorMarker = Marker(mapView).apply {
                    position = anchorGeoPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = getAnchorMarkerDrawable(mapView.context)
                    title = "Anchor Point (Titik Acuan)"
                    snippet = "Radius: ${radiusConfig.radiusMeters.toInt()}m"
                }
                mapView.overlays.add(anchorMarker)

                if (selectedCoordinate == null && lastAnimatedCoord == null) {
                    mapView.controller.animateTo(anchorGeoPoint)
                    lastAnimatedCoord = radiusConfig.anchor
                }
            }

            // Render Selected Coordinate Marker with InfoWindow Preview Callout
            if (selectedCoordinate != null) {
                val titleText = when {
                    isResolvingAddress -> "Mendeteksi nama jalan..."
                    !resolvedShortAddress.isNullOrBlank() -> resolvedShortAddress
                    else -> "Lokasi Terpilih"
                }
                val snippetText = "%.5f, %.5f".format(
                    selectedCoordinate.latitude,
                    selectedCoordinate.longitude
                )

                val marker = Marker(mapView).apply {
                    position = GeoPoint(selectedCoordinate.latitude, selectedCoordinate.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = titleText
                    snippet = snippetText
                }
                mapView.overlays.add(marker)
                marker.showInfoWindow()

                if (selectedCoordinate != lastAnimatedCoord) {
                    mapView.controller.animateTo(
                        GeoPoint(selectedCoordinate.latitude, selectedCoordinate.longitude)
                    )
                    lastAnimatedCoord = selectedCoordinate
                }
            }
            mapView.invalidate()
        }
    )

    DisposableEffect(Unit) {
        onDispose { }
    }
}

private fun getAnchorMarkerDrawable(context: Context): Drawable {
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_anchor_marker)
        ?: return ColorDrawable(android.graphics.Color.TRANSPARENT)
    val density = context.resources.displayMetrics.density
    val size = (36 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return BitmapDrawable(context.resources, bitmap)
}

@Composable
private fun MockActiveBanner(
    location: Location?,
    isLoading: Boolean,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "● MOCK LOCATION ACTIVE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
                if (location != null) {
                    Text(
                        text = location.name,
                        color = Color(0xFFA5D6A7),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "%.6f, %.6f".format(location.latitude, location.longitude),
                        color = Color(0xFF81C784),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onStop,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("STOP", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SelectedCoordinateCard(
    coordinate: Coordinate,
    locationName: String,
    isResolvingAddress: Boolean,
    resolvedShortAddress: String?,
    isLoading: Boolean,
    radiusConfig: RadiusConfig,
    distanceToAnchorMeters: Double?,
    isWithinRadius: Boolean,
    onSave: (String) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInput by remember(coordinate) { mutableStateOf(locationName) }

    LaunchedEffect(resolvedShortAddress) {
        if (!resolvedShortAddress.isNullOrBlank() && (nameInput.isBlank() || nameInput == "Unnamed Location")) {
            nameInput = resolvedShortAddress
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Location Preview Card (Street Name & Lat/Lng)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        if (isResolvingAddress) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Mendeteksi nama jalan...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (!resolvedShortAddress.isNullOrBlank()) {
                            Text(
                                text = resolvedShortAddress,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Nama jalan tidak terdeteksi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "%.6f,  %.6f".format(coordinate.latitude, coordinate.longitude),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.padding(start = 26.dp)
                    )
                }
            }

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Location name") },
                placeholder = { Text("Nama lokasi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Geofence Radius Info & Warning Badge
            if (radiusConfig.isEnabled && distanceToAnchorMeters != null) {
                if (!isWithinRadius) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Di luar batas radius",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Jarak ke acuan: ${distanceToAnchorMeters.toInt()} meter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Jarak ke acuan: ${distanceToAnchorMeters.toInt()} meter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Batas: ${radiusConfig.radiusMeters.toInt()}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedButton(
                    onClick = { onSave(nameInput.ifBlank { "Unnamed Location" }) },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
                ElevatedButton(
                    onClick = onStart,
                    enabled = !isLoading && isWithinRadius,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("START MOCK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnterCoordinatesSheet(
    onDismiss: () -> Unit,
    onPreview: (lat: Double, lng: Double, name: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var latInput by remember { mutableStateOf("") }
    var lngInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Enter Coordinates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Location name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = latInput,
                onValueChange = {
                    latInput = it
                    error = null
                },
                label = { Text("Latitude") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                isError = error != null
            )

            OutlinedTextField(
                value = lngInput,
                onValueChange = {
                    lngInput = it
                    error = null
                },
                label = { Text("Longitude") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                isError = error != null
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val lat = latInput.toDoubleOrNull()
                    val lng = lngInput.toDoubleOrNull()
                    when {
                        lat == null || lng == null -> error = "Please enter valid numeric coordinates"
                        lat < -90 || lat > 90 -> error = "Latitude must be between -90 and 90"
                        lng < -180 || lng > 180 -> error = "Longitude must be between -180 and 180"
                        else -> onPreview(lat, lng, name.ifBlank { "Unnamed Location" })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preview on Map")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickLocationsSheet(
    recentLocations: List<Location>,
    favoriteLocations: List<Location>,
    onDismiss: () -> Unit,
    onLocationTap: (Location) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            item {
                Text(
                    text = "Quick Locations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            if (favoriteLocations.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                items(favoriteLocations) { location ->
                    LocationListItem(location = location, onClick = { onLocationTap(location) })
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            }

            if (recentLocations.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
                items(recentLocations) { location ->
                    LocationListItem(location = location, onClick = { onLocationTap(location) })
                }
            }

            if (favoriteLocations.isEmpty() && recentLocations.isEmpty()) {
                item {
                    Text(
                        text = "No saved locations yet.\nSave a location from the map to see it here.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationListItem(
    location: Location,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(location.name) },
        supportingContent = {
            Text(
                text = "%.6f, %.6f".format(location.latitude, location.longitude),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        },
        leadingContent = {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun MockNotConfiguredDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mock Location Not Configured") },
        text = {
            Text(
                "To use GPS Mirage, you need to set this app as the Mock Location app in Android Developer Options.\n\n" +
                    "1. Enable Developer Options (Settings → About Phone → tap Build Number 7×)\n" +
                    "2. Go to Settings → Developer Options\n" +
                    "3. Select 'Mock location app' and choose GPS Mirage\n\n" +
                    "Then try again."
            )
        },
        confirmButton = {
            Button(onClick = onOpenSettings) { Text("Open Developer Options") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
