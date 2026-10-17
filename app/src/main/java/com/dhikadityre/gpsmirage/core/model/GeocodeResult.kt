package com.dhikadityre.gpsmirage.core.model

/**
 * Result of reverse geocoding a coordinate.
 *
 * @property shortAddress A concise, user-friendly street or area name suitable for quick glance preview.
 * @property fullAddress Full formatted address if available.
 */
data class GeocodeResult(
    val shortAddress: String,
    val fullAddress: String? = null
)
