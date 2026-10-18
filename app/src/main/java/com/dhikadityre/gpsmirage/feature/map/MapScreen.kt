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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.platform.LocalConfiguration
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
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
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
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

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
            is MapEvent.AnimateCamera -> {
                mapViewRef?.controller?.animateTo(GeoPoint(event.coordinate.latitude, event.coordinate.longitude))
                viewModel.clearEvent()
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                selectionMode = uiState.selectionMode,
                isPositionLocked = uiState.isPositionLocked,
                onMapTap = { lat, lng -> viewModel.onMapTap(lat, lng) },
                onMapCenterMoved = { lat, lng -> viewModel.onMapCenterMoved(lat, lng) },
                onMapViewReady = { mapViewRef = it }
            )

            if (uiState.selectionMode == MapSelectionMode.CENTER_PIN && !uiState.isPositionLocked) {
                CenterPinOverlay(
                    isResolvingAddress = uiState.isResolvingAddress,
                    resolvedShortAddress = uiState.resolvedShortAddress
                )
            }

            if (!uiState.isMockActive) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 12.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { viewModel.toggleQuickLocations() },
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Quick locations")
                    }
                    SmallFloatingActionButton(
                        onClick = { showEnterCoordSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Enter coordinates")
                    }
                }
            }

            val configuration = LocalConfiguration.current
            val isCompactScreen = configuration.screenHeightDp < 640

            MapFloatingControls(
                selectionMode = uiState.selectionMode,
                isPositionLocked = uiState.isPositionLocked,
                isRewindEnabled = uiState.isRewindEnabled,
                isCompact = isCompactScreen,
                onToggleSelectionMode = {
                    val nextMode = if (uiState.selectionMode == MapSelectionMode.CENTER_PIN) {
                        MapSelectionMode.TAP_TO_SELECT
                    } else {
                        MapSelectionMode.CENTER_PIN
                    }
                    viewModel.setSelectionMode(nextMode)
                },
                onTogglePositionLock = {
                    viewModel.togglePositionLock()
                },
                onRecenterToAnchor = {
                    val anchor = uiState.radiusConfig.anchor ?: RadiusConfig.MAYAPADA_TOWER
                    mapViewRef?.controller?.setCenter(GeoPoint(anchor.latitude, anchor.longitude))
                    if (uiState.isPositionLocked) {
                        viewModel.togglePositionLock()
                    }
                    viewModel.onMapCenterMoved(anchor.latitude, anchor.longitude)
                },
                onRewind = {
                    viewModel.onRewindClick()
                },
                onZoomIn = { mapViewRef?.controller?.zoomIn() },
                onZoomOut = { mapViewRef?.controller?.zoomOut() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(
                        y = if (isCompactScreen && uiState.selectedCoordinate != null && !uiState.isMockActive) {
                            (-100).dp
                        } else {
                            (-60).dp
                        }
                    )
                    .padding(end = 16.dp)
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
                    isPositionLocked = uiState.isPositionLocked,
                    onTogglePositionLock = { viewModel.togglePositionLock() },
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
            } else if (!uiState.isMockActive) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 6.dp,
                    tonalElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (uiState.selectionMode == MapSelectionMode.CENTER_PIN) {
                                "Geser peta untuk membidik titik lokasi di dalam radius Mayapada"
                            } else {
                                "Ketuk peta untuk memilih titik lokasi di dalam radius Mayapada"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
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
    selectionMode: MapSelectionMode,
    isPositionLocked: Boolean,
    onMapTap: (Double, Double) -> Unit,
    onMapCenterMoved: (Double, Double) -> Unit,
    onMapViewReady: (MapView) -> Unit
) {
    var lastAnimatedCoord by remember { mutableStateOf<Coordinate?>(null) }
    var hasUserInteracted by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Configuration.getInstance().apply {
                load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                userAgentValue = ctx.packageName
                osmdroidBasePath = java.io.File(ctx.cacheDir, "osmdroid")
                osmdroidTileCache = java.io.File(ctx.cacheDir, "osmdroid/tiles")
            }
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setBuiltInZoomControls(false)
                setMultiTouchControls(true)

                val initialAnchor = radiusConfig.anchor ?: RadiusConfig.MAYAPADA_TOWER
                val initialGeoPoint = GeoPoint(initialAnchor.latitude, initialAnchor.longitude)
                setExpectedCenter(initialGeoPoint)

                addOnLayoutChangeListener(object : android.view.View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        v: android.view.View?,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int
                    ) {
                        if (right - left > 0 && bottom - top > 0) {
                            removeOnLayoutChangeListener(this)
                            controller.setZoom(15.0)
                            controller.setCenter(initialGeoPoint)
                        }
                    }
                })

                post {
                    controller.setZoom(15.0)
                    controller.setCenter(initialGeoPoint)
                }

                setOnTouchListener { _, event ->
                    if (event.action == android.view.MotionEvent.ACTION_DOWN || event.action == android.view.MotionEvent.ACTION_MOVE) {
                        hasUserInteracted = true
                    }
                    false
                }

                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        if (hasUserInteracted) {
                            val center = mapCenter
                            if (center != null && (Math.abs(center.latitude) > 0.00001 || Math.abs(center.longitude) > 0.00001)) {
                                onMapCenterMoved(center.latitude, center.longitude)
                            }
                        }
                        return true
                    }

                    override fun onZoom(event: ZoomEvent?): Boolean {
                        if (hasUserInteracted) {
                            val center = mapCenter
                            if (center != null && (Math.abs(center.latitude) > 0.00001 || Math.abs(center.longitude) > 0.00001)) {
                                onMapCenterMoved(center.latitude, center.longitude)
                            }
                        }
                        return true
                    }
                })

                overlays.add(object : org.osmdroid.views.overlay.Overlay() {
                    override fun onSingleTapConfirmed(
                        e: android.view.MotionEvent?,
                        mapView: MapView?
                    ): Boolean {
                        if (e == null || mapView == null) return false
                        hasUserInteracted = true
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
                    title = "Mayapada Tower (Titik Acuan Radius)"
                    snippet = "Batas: ${radiusConfig.radiusMeters.toInt()} meter • Bukan titik mock"
                    setOnMarkerClickListener { marker, _ ->
                        marker.showInfoWindow()
                        true
                    }
                }
                mapView.overlays.add(anchorMarker)

                if (selectedCoordinate == null && lastAnimatedCoord != radiusConfig.anchor) {
                    if (mapView.width > 0 && mapView.height > 0) {
                        mapView.controller.setCenter(anchorGeoPoint)
                        lastAnimatedCoord = radiusConfig.anchor
                    } else {
                        mapView.post {
                            mapView.controller.setCenter(anchorGeoPoint)
                            lastAnimatedCoord = radiusConfig.anchor
                        }
                    }
                }
            }

            // Render Selected Coordinate Marker with InfoWindow Preview Callout
            // In CENTER_PIN mode when not locked, the pin is rendered via Compose CenterPinOverlay
            if (selectedCoordinate != null && (selectionMode == MapSelectionMode.TAP_TO_SELECT || isPositionLocked)) {
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

                if (selectedCoordinate != lastAnimatedCoord && selectionMode == MapSelectionMode.TAP_TO_SELECT) {
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
    isPositionLocked: Boolean = false,
    onTogglePositionLock: () -> Unit = {},
    onSave: (String) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInput by remember(coordinate) { mutableStateOf(locationName) }
    var isExpanded by remember(coordinate) { mutableStateOf(false) }

    LaunchedEffect(resolvedShortAddress) {
        if (!resolvedShortAddress.isNullOrBlank() && (nameInput.isBlank() || nameInput == "Unnamed Location")) {
            nameInput = resolvedShortAddress
        }
    }

    val configuration = LocalConfiguration.current
    val isCompactScreen = configuration.screenHeightDp < 640
    val maxCardHeight = if (isCompactScreen) {
        if (isExpanded) {
            (configuration.screenHeightDp * 0.60f).coerceIn(240f, 380f).dp
        } else {
            (configuration.screenHeightDp * 0.38f).coerceIn(160f, 220f).dp
        }
    } else {
        (configuration.screenHeightDp * 0.50f).coerceIn(320f, 480f).dp
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxCardHeight)
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(if (isCompactScreen) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompactScreen) 8.dp else 12.dp)
        ) {
            // Location Preview Card (Street Name, Lat/Lng, and Expand/Edit Toggle)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = if (isCompactScreen) 8.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
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
                            } else {
                                val displayName = when {
                                    nameInput.isNotBlank() && nameInput != "Unnamed Location" -> nameInput
                                    !resolvedShortAddress.isNullOrBlank() -> resolvedShortAddress
                                    else -> "Nama jalan tidak terdeteksi"
                                }
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = "%.6f,  %.6f".format(coordinate.latitude, coordinate.longitude),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            modifier = Modifier.padding(start = 26.dp)
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                            contentDescription = if (isExpanded) "Tutup edit nama" else "Ubah nama lokasi",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (isPositionLocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTogglePositionLock() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Posisi Terkunci",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Ketuk untuk buka kunci",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Location name") },
                    placeholder = { Text("Nama lokasi") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { isExpanded = false }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
                    onClick = {
                        val finalName = when {
                            nameInput.isNotBlank() -> nameInput
                            !resolvedShortAddress.isNullOrBlank() -> resolvedShortAddress
                            else -> "Unnamed Location"
                        }
                        onSave(finalName)
                    },
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
                .verticalScroll(rememberScrollState())
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

@Composable
private fun MapFloatingControls(
    selectionMode: MapSelectionMode,
    isPositionLocked: Boolean,
    isRewindEnabled: Boolean,
    isCompact: Boolean = false,
    onToggleSelectionMode: () -> Unit,
    onTogglePositionLock: () -> Unit,
    onRecenterToAnchor: () -> Unit,
    onRewind: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = if (isCompact) 40.dp else 44.dp

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            // Mode Switcher Button (Center Pin vs Tap)
            IconButton(
                onClick = onToggleSelectionMode,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = if (selectionMode == MapSelectionMode.CENTER_PIN) {
                        Icons.Default.CenterFocusStrong
                    } else {
                        Icons.Default.TouchApp
                    },
                    contentDescription = if (selectionMode == MapSelectionMode.CENTER_PIN) {
                        "Mode Bidik Tengah (Aktif)"
                    } else {
                        "Mode Ketuk Bebas (Aktif)"
                    },
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Position Lock Toggle Button
            IconButton(
                onClick = onTogglePositionLock,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = if (isPositionLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isPositionLocked) "Buka Kunci Posisi" else "Kunci Posisi",
                    tint = if (isPositionLocked) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Rewind Map Position Button
            IconButton(
                onClick = onRewind,
                enabled = isRewindEnabled,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = if (isRewindEnabled) "Kembalikan Posisi (Rewind)" else "Rewind Tidak Tersedia",
                    tint = if (isRewindEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }

            // Recenter to Anchor Button
            IconButton(
                onClick = onRecenterToAnchor,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Pusatkan ke Titik Acuan",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .width(28.dp)
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Zoom In
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Zoom Out
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CenterPinOverlay(
    isResolvingAddress: Boolean,
    resolvedShortAddress: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-20).dp)
        ) {
            // Floating micro-pill preview with street name / resolving indicator above the pin
            if (isResolvingAddress || !resolvedShortAddress.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 6.dp,
                    tonalElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        if (isResolvingAddress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Mencari jalan...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (!resolvedShortAddress.isNullOrBlank()) {
                            Text(
                                text = resolvedShortAddress,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Target Location",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            )
        }
    }
}

