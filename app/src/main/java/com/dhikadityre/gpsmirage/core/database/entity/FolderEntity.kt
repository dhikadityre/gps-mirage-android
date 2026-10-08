package com.dhikadityre.gpsmirage.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dhikadityre.gpsmirage.core.model.Folder

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(locationCount: Int = 0): Folder = Folder(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        locationCount = locationCount
    )

    companion object {
        fun fromDomain(folder: Folder): FolderEntity = FolderEntity(
            id = folder.id,
            name = folder.name,
            createdAt = folder.createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}
