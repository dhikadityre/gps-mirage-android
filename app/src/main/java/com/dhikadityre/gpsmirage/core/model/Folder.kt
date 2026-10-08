package com.dhikadityre.gpsmirage.core.model

/**
 * Folder / Category for organizing saved locations.
 * "Favorites" is a smart collection (isFavorite == true), NOT a folder.
 */
data class Folder(
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val locationCount: Int = 0
) {
    val isNew: Boolean
        get() = id == 0L
}
