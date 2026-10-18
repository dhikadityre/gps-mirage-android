package com.dhikadityre.gpsmirage.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.dhikadityre.gpsmirage.core.geo.UnitConverter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages device hardware sensors (Rotation Vector / Accelerometer + Magnetometer)
 * to provide real-time compass heading / azimuth angle.
 */
@Singleton
open class CompassSensorManager(
    private val sensorManager: SensorManager?
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    )

    constructor() : this(null)

    /**
     * Checks if device hardware supports compass orientation.
     */
    open fun isCompassAvailable(): Boolean {
        if (sensorManager == null) return false
        val hasRotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
        val hasAccelAndMag = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
        return hasRotationVector || hasAccelAndMag
    }

    /**
     * Observes real-time azimuth angle [0.0°, 360.0°) as a Coroutine Flow.
     */
    open fun observeBearing(): Flow<Float> = callbackFlow {
        if (sensorManager == null) {
            close()
            return@callbackFlow
        }

        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var lastGravity: FloatArray? = null
        var lastGeomagnetic: FloatArray? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val azimuthRad = orientation[0]
                        val azimuthDeg = Math.toDegrees(azimuthRad.toDouble())
                        val normalized = UnitConverter.normalizeBearing(azimuthDeg).toFloat()
                        trySend(normalized)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        lastGravity = event.values.clone()
                        updateFromGravityAndMag()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        lastGeomagnetic = event.values.clone()
                        updateFromGravityAndMag()
                    }
                }
            }

            private fun updateFromGravityAndMag() {
                val g = lastGravity ?: return
                val m = lastGeomagnetic ?: return
                if (SensorManager.getRotationMatrix(rotationMatrix, null, g, m)) {
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthRad = orientation[0]
                    val azimuthDeg = Math.toDegrees(azimuthRad.toDouble())
                    val normalized = UnitConverter.normalizeBearing(azimuthDeg).toFloat()
                    trySend(normalized)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            if (accelerometer != null) {
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            }
            if (magnetometer != null) {
                sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
            }
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
