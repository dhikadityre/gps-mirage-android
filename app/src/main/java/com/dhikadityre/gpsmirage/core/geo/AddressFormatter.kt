package com.dhikadityre.gpsmirage.core.geo

object AddressFormatter {

    /**
     * Formats address components into a concise, human-readable short street/location name.
     */
    fun formatShortAddress(
        thoroughfare: String?,
        subThoroughfare: String?,
        featureName: String?,
        subLocality: String?,
        locality: String?,
        firstAddressLine: String? = null
    ): String? {
        val cleanThoroughfare = thoroughfare?.trim()?.ifBlank { null }
        val cleanSubThoroughfare = subThoroughfare?.trim()?.ifBlank { null }
        val cleanFeatureName = featureName?.trim()?.ifBlank { null }
        val cleanSubLocality = subLocality?.trim()?.ifBlank { null }
        val cleanLocality = locality?.trim()?.ifBlank { null }

        // 1. If street name (thoroughfare) is present
        if (cleanThoroughfare != null) {
            // Check if subThoroughfare (house/street number) exists and is not already part of thoroughfare
            if (cleanSubThoroughfare != null && !cleanThoroughfare.contains(cleanSubThoroughfare, ignoreCase = true)) {
                return "$cleanThoroughfare No. $cleanSubThoroughfare"
            }
            // If featureName is purely numeric and not in thoroughfare, treat as number
            if (cleanFeatureName != null && cleanFeatureName.all { it.isDigit() } && !cleanThoroughfare.contains(cleanFeatureName)) {
                return "$cleanThoroughfare No. $cleanFeatureName"
            }
            return cleanThoroughfare
        }

        // 2. If no thoroughfare, check featureName (e.g. POI name / building / monument)
        if (cleanFeatureName != null) {
            if (!isRawCoordinate(cleanFeatureName)) {
                return cleanFeatureName
            }
        }

        // 3. Fallback to subLocality (Kelurahan / Kecamatan)
        if (cleanSubLocality != null) {
            return cleanSubLocality
        }

        // 4. Fallback to locality (City / Town)
        if (cleanLocality != null) {
            return cleanLocality
        }

        // 5. Fallback to first line of address (take the first segment before comma)
        if (!firstAddressLine.isNullOrBlank()) {
            val firstSegment = firstAddressLine.split(",").firstOrNull()?.trim()
            if (!firstSegment.isNullOrBlank() && !isRawCoordinate(firstSegment)) {
                return firstSegment
            }
        }

        return null
    }

    private fun isRawCoordinate(text: String): Boolean {
        return text.matches(Regex("^-?\\d+(\\.\\d+)?(,\\s*-?\\d+(\\.\\d+)?)?$"))
    }
}
