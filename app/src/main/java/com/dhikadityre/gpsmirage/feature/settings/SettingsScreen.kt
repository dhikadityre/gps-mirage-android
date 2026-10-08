package com.dhikadityre.gpsmirage.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    var defaultAccuracy by remember { mutableFloatStateOf(5f) }
    var defaultSpeed by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Default Accuracy: ${defaultAccuracy.toInt()} meters", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = defaultAccuracy,
                        onValueChange = { defaultAccuracy = it },
                        valueRange = 1f..100f
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Default Speed: ${defaultSpeed.toInt()} km/h", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = defaultSpeed,
                        onValueChange = { defaultSpeed = it },
                        valueRange = 0f..120f
                    )
                }
            }
        }
    }
}
