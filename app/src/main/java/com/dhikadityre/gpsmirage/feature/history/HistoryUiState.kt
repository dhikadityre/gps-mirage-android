package com.dhikadityre.gpsmirage.feature.history

import com.dhikadityre.gpsmirage.core.model.History

data class HistoryGroup(val label: String, val items: List<History>)

data class HistoryUiState(
    val groups: List<HistoryGroup> = emptyList(),
    val isLoading: Boolean = false
)
