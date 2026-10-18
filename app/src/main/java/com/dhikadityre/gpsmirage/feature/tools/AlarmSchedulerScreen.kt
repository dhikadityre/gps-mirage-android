package com.dhikadityre.gpsmirage.feature.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.dhikadityre.gpsmirage.core.model.ReminderAlarm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSchedulerScreen(
    onBack: () -> Unit,
    viewModel: AlarmSchedulerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Permission state tracking
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        viewModel.updatePermissionStatus(isGranted, uiState.canScheduleExact)
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Alarm & Jadwal Notifikasi",
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
                    IconButton(onClick = { viewModel.openCreateDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Alarm"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Jadwal")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Android 13+ Notification Permission Banner
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                item {
                    NotificationPermissionBanner(
                        onRequestPermission = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }
            }

            // Android 12+ Exact Alarm Capability Banner
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !uiState.canScheduleExact) {
                item {
                    ExactAlarmBanner(
                        onOpenSettings = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(fallbackIntent)
                            }
                        }
                    )
                }
            }

            // Master Switch Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (uiState.isMasterEnabled) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = if (uiState.isMasterEnabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Semua Alarm Aktif",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (uiState.isMasterEnabled) {
                                        "Seluruh alarm berjalan sesuai jadwal"
                                    } else {
                                        "Semua jadwal alarm sedang dinonaktifkan"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isMasterEnabled,
                            onCheckedChange = { viewModel.toggleMaster(it) }
                        )
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAFTAR JADWAL PENGINGAT (${uiState.alarms.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Empty state
            if (uiState.alarms.isEmpty()) {
                item {
                    EmptyAlarmState(onAddClick = { viewModel.openCreateDialog() })
                }
            } else {
                items(
                    items = uiState.alarms,
                    key = { it.id }
                ) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { enabled -> viewModel.toggleAlarm(alarm.id, enabled) },
                        onEdit = { viewModel.openEditDialog(alarm) },
                        onDelete = { viewModel.requestDelete(alarm) }
                    )
                }
            }

            // Bottom spacer for FAB
            item {
                Spacer(Modifier.height(72.dp))
            }
        }
    }

    // Add / Edit Alarm Dialog
    if (uiState.isDialogOpen && uiState.dialogAlarm != null) {
        AlarmEditDialog(
            initialAlarm = uiState.dialogAlarm!!,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { label, hour, minute, days ->
                viewModel.saveAlarm(label, hour, minute, days)
            }
        )
    }

    // Delete Confirmation Dialog
    uiState.alarmToDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = {
                Text(
                    text = "Hapus Jadwal Alarm?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus alarm '${alarm.label}' (${alarm.formattedTime})? Tindakan ini tidak dapat dibatalkan.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun AlarmCard(
    alarm: ReminderAlarm,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (alarm.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alarm.formattedTime,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (alarm.isEnabled) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = alarm.repeatSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditDialog(
    initialAlarm: ReminderAlarm,
    onDismiss: () -> Unit,
    onSave: (label: String, hour: Int, minute: Int, days: Set<Int>) -> Unit
) {
    var label by remember { mutableStateOf(initialAlarm.label) }
    var selectedDays by remember { mutableStateOf(initialAlarm.daysOfWeek) }
    val timePickerState = rememberTimePickerState(
        initialHour = initialAlarm.hour,
        initialMinute = initialAlarm.minute,
        is24Hour = true
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialAlarm.id == 0L) "Tambah Jadwal Alarm" else "Edit Jadwal Alarm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                // Time Picker Material 3
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors()
                    )
                }

                // Label Input
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label / Keterangan") },
                    placeholder = { Text("Contoh: Absen Pagi / Shift Siang") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Quick Presets
                Text(
                    text = "PILIHAN PENGULANGAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedDays == setOf(1, 2, 3, 4, 5),
                        onClick = { selectedDays = setOf(1, 2, 3, 4, 5) },
                        label = { Text("Hari Kerja", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedDays == setOf(6, 7),
                        onClick = { selectedDays = setOf(6, 7) },
                        label = { Text("Akhir Pekan", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedDays.size == 7,
                        onClick = { selectedDays = setOf(1, 2, 3, 4, 5, 6, 7) },
                        label = { Text("Setiap Hari", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedDays.isEmpty(),
                        onClick = { selectedDays = emptySet() },
                        label = { Text("Sekali Saja", fontSize = 11.sp) }
                    )
                }

                // Days Selection Chips (Sen s/d Min)
                val daysList = listOf(
                    1 to "Sen",
                    2 to "Sel",
                    3 to "Rab",
                    4 to "Kam",
                    5 to "Jum",
                    6 to "Sab",
                    7 to "Min"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysList.forEach { (dayInt, dayName) ->
                        val isSelected = selectedDays.contains(dayInt)
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    selectedDays = if (isSelected) {
                                        selectedDays - dayInt
                                    } else {
                                        selectedDays + dayInt
                                    }
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = dayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                label,
                                timePickerState.hour,
                                timePickerState.minute,
                                selectedDays
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan Alarm")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAlarmState(onAddClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Belum Ada Jadwal Alarm",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Tambahkan jadwal pengingat untuk membantu Anda membuka dan mengaktifkan GPS Mirage sesuai jam kerja atau absen rutin Anda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tambah Pengingat Pertama")
            }
        }
    }
}

@Composable
private fun NotificationPermissionBanner(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Izin Notifikasi Dibutuhkan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Izinkan aplikasi menampilkan notifikasi agar pengingat jadwal dapat berbunyi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Izinkan", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ExactAlarmBanner(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Izin Alarm Tepat Waktu (Exact)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Aktifkan izin Exact Alarm di pengaturan sistem agar alarm berbunyi tepat detik yang ditentukan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onOpenSettings,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Buka", fontSize = 12.sp)
            }
        }
    }
}
