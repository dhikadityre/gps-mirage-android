package com.dhikadityre.gpsmirage.domain.location

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ResetMode {
    SOFT,
    HARD
}

data class MapResetRequest(
    val mode: ResetMode,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class MapResetCoordinator @Inject constructor() {

    private val _resetRequest = MutableSharedFlow<MapResetRequest>(replay = 1)
    val resetRequest: SharedFlow<MapResetRequest> = _resetRequest.asSharedFlow()

    fun requestReset(mode: ResetMode) {
        _resetRequest.tryEmit(MapResetRequest(mode))
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun consumeReset(): MapResetRequest? {
        val current = _resetRequest.replayCache.firstOrNull()
        _resetRequest.resetReplayCache()
        return current
    }
}
