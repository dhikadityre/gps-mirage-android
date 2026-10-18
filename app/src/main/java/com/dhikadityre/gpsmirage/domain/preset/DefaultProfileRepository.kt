package com.dhikadityre.gpsmirage.domain.preset

import com.dhikadityre.gpsmirage.core.model.DefaultValueProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Default Value Presets.
 */
interface DefaultProfileRepository {
    /**
     * Stream of all saved default value presets, ordered with the system default first.
     */
    val allProfiles: Flow<List<DefaultValueProfile>>

    /**
     * Stream of the currently active default value preset.
     */
    val activeProfile: Flow<DefaultValueProfile?>

    /**
     * Retrieves a profile by its ID.
     */
    suspend fun getById(id: Long): DefaultValueProfile?

    /**
     * Saves (inserts or updates) a profile. Returns the assigned ID.
     */
    suspend fun saveProfile(profile: DefaultValueProfile): Long

    /**
     * Deletes a user-created profile by its ID. System default presets cannot be deleted.
     */
    suspend fun deleteProfile(id: Long)

    /**
     * Sets the specified profile as the currently active default.
     */
    suspend fun setActiveProfile(id: Long)

    /**
     * Resets the system preset 'original' back to factory values.
     */
    suspend fun resetOriginalPreset()

    /**
     * Ensures default preset seeding.
     */
    suspend fun ensureDefaultSeeded()
}
