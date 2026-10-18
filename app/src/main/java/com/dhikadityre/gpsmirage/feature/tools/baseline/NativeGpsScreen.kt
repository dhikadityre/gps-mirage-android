package com.dhikadityre.gpsmirage.feature.tools.baseline

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import androidx.compose.ui.res.stringResource
import com.dhikadityre.dev.tools.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeGpsScreen(
    onBack: () -> Unit,
    viewModel: NativeGpsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.message, uiState.errorMessage) {
        val msg = uiState.message ?: uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.native_gps_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.onOpenSaveDialog() },
                        enabled = uiState.latitude != null && uiState.longitude != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.native_gps_save_point),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Signal Connection Status Banner
            if (uiState.isAcquiring && uiState.latitude == null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.native_gps_connecting),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = stringResource(R.string.native_gps_connecting_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.native_gps_active),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.native_gps_hardware_accuracy, uiState.accuracy),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Live Telemetry Grid
            Text(
                text = stringResource(R.string.native_gps_live_section),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMetricCard(
                    title = "Latitude",
                    value = uiState.latitude?.let { "%.6f".format(it) } ?: "--",
                    icon = Icons.Default.LocationOn,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCard(
                    title = "Longitude",
                    value = uiState.longitude?.let { "%.6f".format(it) } ?: "--",
                    icon = Icons.Default.LocationOn,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMetricCard(
                    title = stringResource(R.string.native_gps_accuracy),
                    value = if (uiState.accuracy > 0f) "±%.1f m".format(uiState.accuracy) else "--",
                    icon = Icons.Default.GpsFixed,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCard(
                    title = stringResource(R.string.native_gps_altitude),
                    value = if (uiState.altitude != 0.0) "%.1f m".format(uiState.altitude) else "--",
                    icon = Icons.Default.Height,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMetricCard(
                    title = stringResource(R.string.native_gps_speed),
                    value = "%.1f km/h".format(uiState.speed),
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCard(
                    title = stringResource(R.string.native_gps_bearing),
                    value = "%.1f°".format(uiState.bearing),
                    icon = Icons.Default.Explore,
                    modifier = Modifier.weight(1f)
                )
            }

            // Radius Acuan Benchmark
            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Radar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.native_gps_radius_benchmark),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${uiState.radiusMeters.toInt()} meter",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Slider(
                        value = uiState.radiusMeters.toFloat(),
                        onValueChange = { viewModel.onRadiusChanged(it.toDouble()) },
                        valueRange = 50f..5000f,
                        steps = 98,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100.0, 500.0, 1000.0, 2000.0).forEach { preset ->
                            FilterChip(
                                selected = uiState.radiusMeters.toInt() == preset.toInt(),
                                onClick = { viewModel.onRadiusChanged(preset) },
                                label = { Text("${preset.toInt()}m", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // OSMDroid Map Preview
            Text(
                text = stringResource(R.string.native_gps_preview_section),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                val lat = uiState.latitude
                val lng = uiState.longitude
                val radius = uiState.radiusMeters

                if (lat != null && lng != null) {
                    AndroidView(
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
                                controller.setZoom(16.0)
                                val point = GeoPoint(lat, lng)
                                controller.setCenter(point)
                            }
                        },
                        update = { mapView ->
                            mapView.overlays.clear()
                            val point = GeoPoint(lat, lng)

                            // Accuracy/Radius circle
                            val circle = Polygon(mapView).apply {
                                points = Polygon.pointsAsCircle(point, radius)
                                fillPaint.color = android.graphics.Color.argb(40, 103, 80, 164) // Tertiary tone
                                outlinePaint.color = android.graphics.Color.argb(200, 103, 80, 164)
                                outlinePaint.strokeWidth = 3f * mapView.context.resources.displayMetrics.density
                                setOnClickListener { _, _, _ -> false }
                            }
                            mapView.overlays.add(circle)

                            // Physical position pin
                            val marker = Marker(mapView).apply {
                                position = point
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = mapView.context.getString(R.string.native_gps_marker_title)
                                snippet = "%.6f, %.6f (%s: ±%.1fm)".format(
                                    lat,
                                    lng,
                                    mapView.context.getString(R.string.native_gps_accuracy),
                                    uiState.accuracy
                                )
                                setOnMarkerClickListener { m, _ ->
                                    m.showInfoWindow()
                                    true
                                }
                            }
                            mapView.overlays.add(marker)
                            marker.showInfoWindow()

                            mapView.controller.setCenter(point)
                            mapView.invalidate()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Text(
                                text = stringResource(R.string.native_gps_waiting_satellites),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // Save Location Dialog
    if (uiState.showSaveDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissSaveDialog() },
            title = {
                Text(
                    text = stringResource(R.string.native_gps_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.native_gps_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.native_gps_type_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    OutlinedTextField(
                        value = uiState.saveLocationName,
                        onValueChange = { viewModel.onSaveLocationNameChanged(it) },
                        label = { Text(stringResource(R.string.location_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(
                            R.string.native_gps_coordinates_format,
                            uiState.latitude ?: 0.0,
                            uiState.longitude ?: 0.0
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onConfirmSaveLocation() }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onDismissSaveDialog() }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun TelemetryMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
