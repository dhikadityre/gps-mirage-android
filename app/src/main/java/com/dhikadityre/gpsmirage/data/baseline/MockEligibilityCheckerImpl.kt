package com.dhikadityre.gpsmirage.data.baseline

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.dhikadityre.gpsmirage.domain.baseline.EligibilityReport
import com.dhikadityre.gpsmirage.domain.baseline.MockEligibilityChecker
import com.dhikadityre.gpsmirage.domain.location.MockLocationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockEligibilityCheckerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mockLocationManager: MockLocationManager
) : MockEligibilityChecker {

    override fun checkEligibility(): EligibilityReport {
        return EligibilityReport(
            isDeveloperOptionsEnabled = isDeveloperOptionsEnabled(),
            isMockAppSet = mockLocationManager.isAppSetAsMockProvider(),
            isGpsEnabled = isGpsEnabled(),
            hasLocationPermission = hasLocationPermission(),
            isDeviceRooted = isDeviceRooted()
        )
    }

    private fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) != 0
        } catch (_: Exception) {
            false
        }
    }

    private fun isGpsEnabled(): Boolean {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    private fun isDeviceRooted(): Boolean {
        val suPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su"
        )
        val hasSuBinary = suPaths.any { path ->
            try {
                File(path).exists()
            } catch (_: Exception) {
                false
            }
        }
        val hasTestKeys = Build.TAGS?.contains("test-keys") == true
        return hasSuBinary || hasTestKeys
    }
}
