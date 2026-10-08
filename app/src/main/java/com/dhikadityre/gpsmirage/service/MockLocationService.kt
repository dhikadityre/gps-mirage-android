package com.dhikadityre.gpsmirage.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.dhikadityre.dev.tools.R
import com.dhikadityre.gpsmirage.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MockLocationService : Service() {

    companion object {
        const val CHANNEL_ID = "gps_mirage_mock_location"
        const val NOTIFICATION_ID = 1001
        const val PROVIDER_NAME = LocationManager.GPS_PROVIDER

        const val ACTION_START = "com.dhikadityre.dev.tools.ACTION_START"
        const val ACTION_STOP = "com.dhikadityre.dev.tools.ACTION_STOP"

        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_ACCURACY = "extra_accuracy"
        const val EXTRA_ALTITUDE = "extra_altitude"
        const val EXTRA_SPEED = "extra_speed"
        const val EXTRA_BEARING = "extra_bearing"
        const val EXTRA_NAME = "extra_name"

        const val UPDATE_INTERVAL_MS = 1000L

        fun buildStartIntent(
            context: Context,
            lat: Double,
            lng: Double,
            accuracy: Float = 5f,
            altitude: Double = 0.0,
            speed: Float = 0f,
            bearing: Float = 0f,
            name: String = ""
        ): Intent = Intent(context, MockLocationService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_LATITUDE, lat)
            putExtra(EXTRA_LONGITUDE, lng)
            putExtra(EXTRA_ACCURACY, accuracy)
            putExtra(EXTRA_ALTITUDE, altitude)
            putExtra(EXTRA_SPEED, speed)
            putExtra(EXTRA_BEARING, bearing)
            putExtra(EXTRA_NAME, name)
        }

        fun buildStopIntent(context: Context): Intent =
            Intent(context, MockLocationService::class.java).apply {
                action = ACTION_STOP
            }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mockJob: Job? = null
    private var locationManager: LocationManager? = null

    private var currentLat = 0.0
    private var currentLng = 0.0
    private var currentAccuracy = 5f
    private var currentAltitude = 0.0
    private var currentSpeed = 0f
    private var currentBearing = 0f
    private var locationName = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentLat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
                currentLng = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
                currentAccuracy = intent.getFloatExtra(EXTRA_ACCURACY, 5f)
                currentAltitude = intent.getDoubleExtra(EXTRA_ALTITUDE, 0.0)
                currentSpeed = intent.getFloatExtra(EXTRA_SPEED, 0f)
                currentBearing = intent.getFloatExtra(EXTRA_BEARING, 0f)
                locationName = intent.getStringExtra(EXTRA_NAME) ?: ""
                startMocking()
            }
            ACTION_STOP -> {
                stopMocking()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startMocking() {
        startForeground(NOTIFICATION_ID, buildNotification())
        setupMockProvider()

        mockJob?.cancel()
        mockJob = serviceScope.launch {
            while (isActive) {
                try {
                    pushMockLocation()
                } catch (e: Exception) {
                    // Security exception = not set as mock app
                }
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun setupMockProvider() {
        try {
            val lm = locationManager ?: return
            try {
                lm.removeTestProvider(PROVIDER_NAME)
            } catch (_: Exception) {}

            lm.addTestProvider(
                PROVIDER_NAME,
                false, false, false, false, true, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE
            )
            lm.setTestProviderEnabled(PROVIDER_NAME, true)
        } catch (e: SecurityException) {
            // App not set as mock location app in developer options
        }
    }

    private fun pushMockLocation() {
        val lm = locationManager ?: return
        val mockLocation = Location(PROVIDER_NAME).apply {
            latitude = currentLat
            longitude = currentLng
            accuracy = currentAccuracy
            altitude = currentAltitude
            speed = currentSpeed
            bearing = currentBearing
            time = System.currentTimeMillis()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
        }
        lm.setTestProviderLocation(PROVIDER_NAME, mockLocation)
    }

    private fun stopMocking() {
        mockJob?.cancel()
        try {
            locationManager?.removeTestProvider(PROVIDER_NAME)
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mock Location",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "GPS Mirage mock location active"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0, buildStopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (locationName.isNotBlank()) locationName else "Mock Location Active"
        val content = "%.6f, %.6f".format(currentLat, currentLng)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(0, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopMocking()
        serviceScope.launch { } // cancel all
        super.onDestroy()
    }
}
