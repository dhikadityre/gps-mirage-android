package com.dhikadityre.gpsmirage.feature.tools.baseline

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EligibilityScreen(
    onBack: () -> Unit,
    viewModel: EligibilityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Status Fake GPS",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshEligibility() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val report = uiState.report
            val isFullyEligible = report?.isFullyEligible == true

            // SUMMARY HERO CARD
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isFullyEligible) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isFullyEligible) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isFullyEligible) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isFullyEligible) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFullyEligible) "Sistem Eligible & Siap" else "Perlu Tindakan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isFullyEligible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isFullyEligible) {
                                "Semua prasyarat mock location terpenuhi. Aplikasi siap digunakan untuk simulasi titik koordinat."
                            } else {
                                "Terdapat prasyarat yang belum aktif. Ketuk tombol pada item di bawah untuk mengonfigurasinya."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "PILIHAN EVALUASI KELAYAKAN SISTEM",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // PILLAR 1: DEVELOPER OPTIONS
            PillarDiagnosticCard(
                title = "1. Opsi Pengembang (Developer Options)",
                subtitle = "Prasyarat utama Android untuk mengizinkan penetapan aplikasi mock location.",
                isPassed = report?.isDeveloperOptionsEnabled == true,
                statusText = if (report?.isDeveloperOptionsEnabled == true) "Aktif" else "Tidak Aktif",
                icon = Icons.Default.Code,
                actionLabel = "Buka Opsi Pengembang",
                onActionClick = {
                    launchSettings(context, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                }
            )

            // PILLAR 2: MOCK LOCATION APP PROVIDER
            PillarDiagnosticCard(
                title = "2. Aplikasi Lokasi Tiruan (Mock App)",
                subtitle = "GPS Mirage harus ditetapkan sebagai penyedia lokasi tiruan di Opsi Pengembang.",
                isPassed = report?.isMockAppSet == true,
                statusText = if (report?.isMockAppSet == true) "GPS Mirage Terpilih" else "Belum Ditetapkan",
                icon = Icons.Default.VerifiedUser,
                actionLabel = "Atur Mock Location App",
                onActionClick = {
                    launchSettings(context, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                }
            )

            // PILLAR 3: DEVICE GPS SENSOR
            PillarDiagnosticCard(
                title = "3. Sensor GPS Fisik (Location Service)",
                subtitle = "Layanan lokasi hardware perangkat harus dalam kondisi menyala/aktif.",
                isPassed = report?.isGpsEnabled == true,
                statusText = if (report?.isGpsEnabled == true) "GPS Aktif" else "Layanan Lokasi Nonaktif",
                icon = Icons.Default.GpsFixed,
                actionLabel = "Buka Pengaturan Lokasi",
                onActionClick = {
                    launchSettings(context, Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                }
            )

            // PILLAR 4: LOCATION PERMISSIONS
            PillarDiagnosticCard(
                title = "4. Izin Lokasi Perangkat",
                subtitle = "Izin ACCESS_FINE_LOCATION diperlukan untuk membaca dan memvalidasi koordinat.",
                isPassed = report?.hasLocationPermission == true,
                statusText = if (report?.hasLocationPermission == true) "Izin Diberikan" else "Izin Belum Lengkap",
                icon = Icons.Default.LocationOn,
                actionLabel = "Beri Izin Aplikasi",
                onActionClick = {
                    launchAppDetailsSettings(context)
                }
            )

            // PILLAR 5: ROOT STATUS
            PillarDiagnosticCard(
                title = "5. Status Deteksi Root (Superuser)",
                subtitle = "Pemeriksaan biner su dan build test-keys perangkat.",
                isPassed = true, // Both rooted & non-rooted are supported!
                statusText = if (report?.isDeviceRooted == true) "Root Terdeteksi" else "Non-Rooted (Standar Android)",
                icon = Icons.Default.Security,
                badgeColor = if (report?.isDeviceRooted == true) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                actionLabel = null,
                onActionClick = null
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.refreshEligibility() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Periksa Ulang Kesiapan",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PillarDiagnosticCard(
    title: String,
    subtitle: String,
    isPassed: Boolean,
    statusText: String,
    icon: ImageVector,
    badgeColor: Color? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = (badgeColor ?: if (isPassed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error).copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeColor ?: if (isPassed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = (badgeColor ?: if (isPassed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = (badgeColor?.let { MaterialTheme.colorScheme.onSecondaryContainer } ?: if (isPassed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isPassed && actionLabel != null && onActionClick != null) {
                FilledTonalButton(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = actionLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun launchSettings(context: Context, action: String) {
    try {
        context.startActivity(Intent(action))
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) {}
    } catch (_: Exception) {}
}

private fun launchAppDetailsSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        launchSettings(context, Settings.ACTION_SETTINGS)
    } catch (_: Exception) {}
}
