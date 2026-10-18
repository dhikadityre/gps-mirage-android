package com.dhikadityre.gpsmirage.domain.location

import com.dhikadityre.gpsmirage.core.model.Location
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class MapPreviewRequest(
    val target: Location,
    val parentAnchor: Location? = null
)

@Singleton
class MapPreviewCoordinator @Inject constructor() {

    private val _previewRequest = MutableSharedFlow<MapPreviewRequest>(replay = 1)
    val previewRequest: SharedFlow<MapPreviewRequest> = _previewRequest.asSharedFlow()

    fun requestPreview(target: Location, parentAnchor: Location? = null) {
        _previewRequest.tryEmit(MapPreviewRequest(target, parentAnchor))
    }

    fun consumePreview(): MapPreviewRequest? {
        val current = _previewRequest.replayCache.firstOrNull()
        _previewRequest.resetReplayCache()
        return current
    }
}
