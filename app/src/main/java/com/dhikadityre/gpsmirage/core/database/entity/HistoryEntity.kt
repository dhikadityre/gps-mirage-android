package com.dhikadityre.gpsmirage.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dhikadityre.gpsmirage.core.model.History

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "location_id")
    val locationId: Long? = null,
    @ColumnInfo(name = "location_name")
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "started_at")
    val startedAt: Long,
    @ColumnInfo(name = "stopped_at")
    val stoppedAt: Long? = null,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long? = null
) {
    fun toDomain(): History = History(
        id = id,
        locationId = locationId,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        startedAt = startedAt,
        stoppedAt = stoppedAt,
        durationMs = durationMs
    )

    companion object {
        fun fromDomain(history: History): HistoryEntity = HistoryEntity(
            id = history.id,
            locationId = history.locationId,
            locationName = history.locationName,
            latitude = history.latitude,
            longitude = history.longitude,
            startedAt = history.startedAt,
            stoppedAt = history.stoppedAt,
            durationMs = history.durationMs
        )
    }
}
