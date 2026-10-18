package com.dhikadityre.gpsmirage.domain.baseline

data class EligibilityReport(
    val isDeveloperOptionsEnabled: Boolean,
    val isMockAppSet: Boolean,
    val isGpsEnabled: Boolean,
    val hasLocationPermission: Boolean,
    val isDeviceRooted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isFullyEligible: Boolean
        get() = isDeveloperOptionsEnabled && isMockAppSet && isGpsEnabled && hasLocationPermission
}

interface MockEligibilityChecker {
    fun checkEligibility(): EligibilityReport
}
