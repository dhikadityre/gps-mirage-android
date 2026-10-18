package com.dhikadityre.gpsmirage.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.LocationType

@Entity(
    tableName = "locations",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_anchor_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("folder_id"),
        Index("parent_anchor_id")
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "location_type")
    val locationType: String = "MOCK_TARGET",
    @ColumnInfo(name = "radius_meters")
    val radiusMeters: Double? = null,
    @ColumnInfo(name = "parent_anchor_id")
    val parentAnchorId: Long? = null,
    val accuracy: Float = 5f,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "folder_id")
    val folderId: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Location = Location(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        type = try {
            LocationType.valueOf(locationType)
        } catch (e: Exception) {
            LocationType.MOCK_TARGET
        },
        radiusMeters = radiusMeters,
        parentAnchorId = parentAnchorId,
        accuracy = accuracy,
        altitude = altitude,
        speed = speed,
        bearing = bearing,
        isFavorite = isFavorite,
        folderId = folderId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(location: Location): LocationEntity = LocationEntity(
            id = location.id,
            name = location.name,
            latitude = location.latitude,
            longitude = location.longitude,
            locationType = location.type.name,
            radiusMeters = location.radiusMeters,
            parentAnchorId = location.parentAnchorId,
            accuracy = location.accuracy,
            altitude = location.altitude,
            speed = location.speed,
            bearing = location.bearing,
            isFavorite = location.isFavorite,
            folderId = location.folderId,
            createdAt = location.createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}

