package com.dhikadityre.gpsmirage.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dhikadityre.gpsmirage.core.model.DefaultValueProfile

@Entity(tableName = "default_profiles")
data class DefaultProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "radius_meters")
    val radiusMeters: Double,
    @ColumnInfo(name = "accuracy_meters")
    val accuracyMeters: Double,
    @ColumnInfo(name = "altitude_meters")
    val altitudeMeters: Double,
    @ColumnInfo(name = "speed_kmh")
    val speedKmh: Double,
    @ColumnInfo(name = "bearing_degrees")
    val bearingDegrees: Double,
    @ColumnInfo(name = "use_device_sensor")
    val useDeviceSensor: Boolean,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): DefaultValueProfile = DefaultValueProfile(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
        accuracyMeters = accuracyMeters,
        altitudeMeters = altitudeMeters,
        speedKmh = speedKmh,
        bearingDegrees = bearingDegrees,
        useDeviceSensor = useDeviceSensor,
        isActive = isActive,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(profile: DefaultValueProfile): DefaultProfileEntity = DefaultProfileEntity(
            id = profile.id,
            name = profile.name,
            latitude = profile.latitude,
            longitude = profile.longitude,
            radiusMeters = profile.radiusMeters,
            accuracyMeters = profile.accuracyMeters,
            altitudeMeters = profile.altitudeMeters,
            speedKmh = profile.speedKmh,
            bearingDegrees = profile.bearingDegrees,
            useDeviceSensor = profile.useDeviceSensor,
            isActive = profile.isActive,
            isDefault = profile.isDefault,
            createdAt = profile.createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}
