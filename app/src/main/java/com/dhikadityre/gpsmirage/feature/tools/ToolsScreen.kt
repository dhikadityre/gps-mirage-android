package com.dhikadityre.gpsmirage.feature.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateToSetupGuide: () -> Unit,
    onNavigateToGeofenceRadius: () -> Unit,
    onNavigateToDefaultValues: () -> Unit,
    onNavigateToGenerator: () -> Unit,
    onNavigateToAlarmScheduler: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToExport: () -> Unit,
    onSoftReset: () -> Unit = {},
    onHardReset: () -> Unit = {}
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Reset Fake GPS",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Pilih mode pemulihan koordinat dan sistem mock:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "1. Soft Reset",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Hanya menghentikan mock dan melepas test provider. GPS fisik asli HP pulih normal tanpa mengubah posisi camera/pin peta saat ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "2. Hard Reset",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Hentikan mock, lepas provider, pulihkan GPS fisik, kembalikan posisi camera & pin peta ke Center Anchor, serta reset status Lock dan Rewind.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onHardReset()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hard Reset")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Batal")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            showResetDialog = false
                            onSoftReset()
                        }
                    ) {
                        Text("Soft Reset")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tools",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // SECTION 1: INTERCEPT GPS
            SectionHeader(
                title = "INTERCEPT GPS",
                description = "Kontrol intersep sinyal GPS, pemulihan perangkat, panduan setup, dan manajemen nilai default."
            )

            ToolItemCard(
                title = "Reset Fake GPS",
                description = "Pulihkan GPS fisik asli perangkat dan kembalikan setelan mock ke kondisi awal (Soft / Hard Reset).",
                badge = "Dual-Mode",
                icon = Icons.Default.RestartAlt,
                onClick = { showResetDialog = true }
            )

            ToolItemCard(
                title = "Cara Setup",
                description = "Panduan aktivasi Opsi Pengembang (Developer Options) dan penetapan Mock Location App.",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                onClick = onNavigateToSetupGuide
            )

            ToolItemCard(
                title = "Geofence Radius",
                description = "Atur batas area pemilihan mock location dan koordinat titik acuan (Center Anchor).",
                icon = Icons.Default.Adjust,
                onClick = onNavigateToGeofenceRadius
            )

            ToolItemCard(
                title = "Default Values",
                description = "Kelola profil nilai default bawaan (original) untuk autocompletion form input koordinat.",
                icon = Icons.Default.Tune,
                onClick = onNavigateToDefaultValues
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // SECTION 2: AUTOMATION
            SectionHeader(
                title = "AUTOMATION",
                description = "Otomasi pembuatan titik koordinat dan jadwal pengingat rutin aplikasi."
            )

            ToolItemCard(
                title = "Coordinate Generator",
                description = "Hasilkan kumpulan koordinat acak/terdistribusi di dalam radius lingkaran titik acuan.",
                icon = Icons.Default.Refresh,
                onClick = onNavigateToGenerator
            )

            ToolItemCard(
                title = "Alarm & Jadwal Notifikasi",
                description = "Pengingat terjadwal jam dan hari tertentu untuk membuka dan menjalankan GPS Mirage.",
                icon = Icons.Default.Alarm,
                onClick = onNavigateToAlarmScheduler
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // SECTION 3: APPEARANCE
            SectionHeader(
                title = "APPEARANCE",
                description = "Kustomisasi gaya visual peta, status bar, bahasa, dan mode tema aplikasi."
            )

            ToolItemCard(
                title = "Tampilan & Tema",
                description = "Pilihan layer peta (Google Maps/OSM), notifikasi status bar, bahasa, dan dark/light mode.",
                icon = Icons.Default.Palette,
                onClick = onNavigateToAppearance
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // SECTION 4: DATA MANAGEMENT
            SectionHeader(
                title = "DATA MANAGEMENT",
                description = "Penyimpanan, pencadangan, dan pemulihan data titik koordinat tersimpan."
            )

            ToolItemCard(
                title = "Import Locations",
                description = "Tempel koordinat tunggal/massal dalam berbagai format, atau impor file JSON cadangan.",
                icon = Icons.Default.FileDownload,
                onClick = onNavigateToImport
            )

            ToolItemCard(
                title = "Export Locations",
                description = "Ekspor lokasi favorit, folder tertentu, atau seluruh database koordinat ke file JSON.",
                icon = Icons.Default.FileUpload,
                onClick = onNavigateToExport
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    description: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToolItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    badge: String? = null,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    badge?.let {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
