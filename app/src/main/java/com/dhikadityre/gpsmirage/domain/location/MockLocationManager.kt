package com.dhikadityre.gpsmirage.domain.location

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.service.MockLocationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface MockLocationManager {
    val isRunning: StateFlow<Boolean>
    val activeLocation: StateFlow<Location?>
    fun start(location: Location): MockStartResult
    fun stop()
    fun isAppSetAsMockProvider(): Boolean
}

sealed interface MockStartResult {
    object Success : MockStartResult
    object NotConfigured : MockStartResult
    object PermissionDenied : MockStartResult
    data class Error(val message: String) : MockStartResult
}

@Singleton
class MockLocationManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MockLocationManager {

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _activeLocation = MutableStateFlow<Location?>(null)
    override val activeLocation: StateFlow<Location?> = _activeLocation.asStateFlow()

    override fun start(location: Location): MockStartResult {
        return try {
            if (!isAppSetAsMockProvider()) {
                return MockStartResult.NotConfigured
            }

            val intent = MockLocationService.buildStartIntent(
                context = context,
                lat = location.latitude,
                lng = location.longitude,
                accuracy = location.accuracy,
                altitude = location.altitude,
                speed = location.speed,
                bearing = location.bearing,
                name = location.name
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }

            _isRunning.value = true
            _activeLocation.value = location
            MockStartResult.Success
        } catch (e: SecurityException) {
            MockStartResult.PermissionDenied
        } catch (e: Exception) {
            MockStartResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun stop() {
        try {
            val intent = MockLocationService.buildStopIntent(context)
            context.startService(intent)
        } catch (_: Exception) {}
        _isRunning.value = false
        _activeLocation.value = null
    }

    override fun isAppSetAsMockProvider(): Boolean {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            // Try adding a test provider — if SecurityException, we're not the mock app
            lm.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false, false, false, false, true, true, true,
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE
            )
            lm.removeTestProvider(LocationManager.GPS_PROVIDER)
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
