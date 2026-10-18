package com.dhikadityre.gpsmirage.domain.location

import com.dhikadityre.gpsmirage.core.model.Coordinate
import javax.inject.Inject
import javax.inject.Singleton

data class RewindTarget(
    val coordinate: Coordinate,
    val requiresUnlock: Boolean = false
)

@Singleton
class RewindCoordinator @Inject constructor() {

    private var centerAnchor: Coordinate? = null
    private var lastLockCoordinate: Coordinate? = null
    private var lastMockCoordinate: Coordinate? = null
    private var isPositionLocked: Boolean = false
    private var currentCoordinate: Coordinate? = null

    private var hasRewoundToLock: Boolean = false
    private var hasExecutedFinalStep: Boolean = false
    private var hasReachedFinalTarget: Boolean = false

    fun setCenterAnchor(anchor: Coordinate) {
        this.centerAnchor = anchor
        if (currentCoordinate == null) {
            currentCoordinate = anchor
        }
    }

    fun onLockPosition(coord: Coordinate) {
        this.lastLockCoordinate = coord
        this.isPositionLocked = true
        this.hasRewoundToLock = false
        this.hasExecutedFinalStep = false
        this.hasReachedFinalTarget = false
    }

    fun onUnlockPosition() {
        this.isPositionLocked = false
    }

    fun onMockStarted(coord: Coordinate) {
        this.lastMockCoordinate = coord
        this.hasExecutedFinalStep = false
        this.hasReachedFinalTarget = false
    }

    fun onMapMoved(currentCoord: Coordinate) {
        this.currentCoordinate = currentCoord
        val finalTarget = getFinalTarget()
        if (finalTarget != null && areCoordinatesClose(currentCoord, finalTarget)) {
            if (hasExecutedFinalStep || (lastLockCoordinate == null && !hasRewoundToLock)) {
                hasReachedFinalTarget = true
            }
        } else {
            // Map panned away from final target
            hasExecutedFinalStep = false
            hasReachedFinalTarget = false
        }
    }

    private fun getFinalTarget(): Coordinate? {
        return lastMockCoordinate ?: centerAnchor
    }

    fun isRewindAvailable(): Boolean {
        return getAvailableRewindTarget() != null
    }

    fun getAvailableRewindTarget(): RewindTarget? {
        val curr = currentCoordinate ?: return null
        val finalTarget = getFinalTarget() ?: return null

        // Stage 1: Lock Location (if stored and haven't rewound to lock yet)
        val lockTarget = lastLockCoordinate
        if (lockTarget != null && !hasRewoundToLock) {
            return RewindTarget(coordinate = lockTarget, requiresUnlock = false)
        }

        // Stage 2: Final Target (Mock Location or Center Anchor)
        if (!hasReachedFinalTarget && !hasExecutedFinalStep) {
            val isAtFinal = areCoordinatesClose(curr, finalTarget)
            if (!isAtFinal || hasRewoundToLock) {
                val requiresUnlock = (lastMockCoordinate != null && isPositionLocked)
                return RewindTarget(coordinate = finalTarget, requiresUnlock = requiresUnlock)
            }
        }

        return null
    }

    fun executeRewindStep(): RewindTarget? {
        val target = getAvailableRewindTarget() ?: return null

        val lockTarget = lastLockCoordinate
        if (lockTarget != null && !hasRewoundToLock) {
            hasRewoundToLock = true
            return target
        }

        val finalTarget = getFinalTarget()
        if (finalTarget != null) {
            hasExecutedFinalStep = true
            return target
        }

        return target
    }

    private fun areCoordinatesClose(c1: Coordinate, c2: Coordinate): Boolean {
        return Math.abs(c1.latitude - c2.latitude) < 0.000001 &&
               Math.abs(c1.longitude - c2.longitude) < 0.000001
    }
}
