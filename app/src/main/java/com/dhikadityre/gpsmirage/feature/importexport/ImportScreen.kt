package com.dhikadityre.gpsmirage.feature.importexport

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.LocationDraft
import com.dhikadityre.gpsmirage.core.parser.LocationParser
import com.dhikadityre.gpsmirage.core.parser.ParseResult
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DuplicateStrategy { SKIP, CREATE, REPLACE }

data class ImportUiState(
    val inputText: String = "",
    val parseResult: ParseResult? = null,
    val isLoading: Boolean = false,
    val showDuplicateDialog: Boolean = false,
    val successMessage: String? = null
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val locationParser: LocationParser,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text, parseResult = null, successMessage = null) }
    }

    fun preview() {
        val text = _uiState.value.inputText
        if (text.isBlank()) return
        val res = locationParser.parse(text)
        _uiState.update { it.copy(parseResult = res) }
    }

    fun initiateImport() {
        _uiState.update { it.copy(showDuplicateDialog = true) }
    }

    fun dismissDuplicateDialog() {
        _uiState.update { it.copy(showDuplicateDialog = false) }
    }

    fun executeImport(strategy: DuplicateStrategy) {
        val drafts = when (val res = _uiState.value.parseResult) {
            is ParseResult.Success -> res.locations
            is ParseResult.Partial -> res.valid
            else -> emptyList()
        }
        if (drafts.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDuplicateDialog = false) }
            var savedCount = 0

            for (draft in drafts) {
                val isDup = locationRepository.isDuplicate(draft.name, draft.latitude, draft.longitude)
                if (isDup && strategy == DuplicateStrategy.SKIP) {
                    continue
                }
                locationRepository.save(draft.toLocation())
                savedCount++
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    inputText = "",
                    parseResult = null,
                    successMessage = "Successfully imported $savedCount locations!"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Locations") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.updateInputText(it) },
                label = { Text("Paste coordinates or JSON here") },
                placeholder = { Text("-6.200123, 106.816456\nJakarta, -6.2, 106.8\n{\"version\":1, \"locations\":[...]}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.preview() },
                    enabled = uiState.inputText.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Preview")
                }
            }

            uiState.successMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            when (val res = uiState.parseResult) {
                is ParseResult.Success -> {
                    PreviewSection(
                        locations = res.locations,
                        errorCount = 0,
                        onImport = { viewModel.initiateImport() }
                    )
                }
                is ParseResult.Partial -> {
                    PreviewSection(
                        locations = res.valid,
                        errorCount = res.errors.size,
                        onImport = { viewModel.initiateImport() }
                    )
                }
                is ParseResult.Failure -> {
                    Text(
                        "Error parsing input: ${res.error.reason}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                null -> Unit
            }
        }
    }

    if (uiState.showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateDialog() },
            title = { Text("Duplicate Handling") },
            text = { Text("How would you like to handle duplicates if found?") },
            confirmButton = {
                TextButton(onClick = { viewModel.executeImport(DuplicateStrategy.SKIP) }) {
                    Text("Skip Duplicates")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.executeImport(DuplicateStrategy.CREATE) }) {
                    Text("Import All")
                }
            }
        )
    }
}

@Composable
private fun PreviewSection(
    locations: List<LocationDraft>,
    errorCount: Int,
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Found ${locations.size} valid location(s)${if (errorCount > 0) " ($errorCount errors)" else ""}",
                style = MaterialTheme.typography.titleSmall
            )
            Button(onClick = onImport) {
                Text("Import ${locations.size}")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(locations) { loc ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(loc.name)
                        Text(loc.displayCoordinate, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
