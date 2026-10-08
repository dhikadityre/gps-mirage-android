package com.dhikadityre.gpsmirage.feature.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateToGenerator: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tools") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ToolItemCard(
                title = "Coordinate Generator",
                description = "Generate coordinates within a circle radius around any central point.",
                icon = Icons.Default.Refresh,
                onClick = onNavigateToGenerator
            )
            ToolItemCard(
                title = "Import Locations",
                description = "Paste single or bulk coordinates in various formats, or import JSON files.",
                icon = Icons.Default.FileDownload,
                onClick = onNavigateToImport
            )
            ToolItemCard(
                title = "Export Locations",
                description = "Export your saved favorites, specific folders, or all locations as JSON.",
                icon = Icons.Default.FileUpload,
                onClick = onNavigateToExport
            )
        }
    }
}

@Composable
private fun ToolItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
