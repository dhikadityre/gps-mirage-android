package com.dhikadityre.gpsmirage.core.geo

import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.GeocodeResult

/**
 * Reverse geocoder interface to resolve geographic coordinates into human-readable addresses.
 */
interface ReverseGeocoder {

    /**
     * Resolves the coordinate into a user-friendly short street name.
     * Returns null if geocoding backend is unavailable, network is offline, or no address is found.
     */
    suspend fun getShortAddress(coordinate: Coordinate): GeocodeResult?
}
