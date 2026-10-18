package com.dhikadityre.gpsmirage.core.geo

import java.util.Locale

/**
 * Utility functions for bidirectional unit conversion (meters <-> feet)
 * and spatial angle normalization.
 */
object UnitConverter {
    /**
     * Standard conversion factor: 1 meter = 3.28084 feet.
     */
    const val FEET_PER_METER = 3.28084
    const val METERS_PER_FOOT = 1.0 / FEET_PER_METER

    /**
     * Converts meters to feet.
     */
    fun metersToFeet(meters: Double): Double = meters * FEET_PER_METER

    /**
     * Converts feet to meters.
     */
    fun feetToMeters(feet: Double): Double = feet * METERS_PER_FOOT

    /**
     * Normalizes bearing angle into the range [0.0, 360.0).
     */
    fun normalizeBearing(degrees: Double): Double {
        val mod = degrees % 360.0
        return if (mod < 0) mod + 360.0 else if (mod == 360.0) 0.0 else mod
    }

    /**
     * Formats a floating point value to two decimal places using US Locale.
     */
    fun formatTwoDecimals(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }
}
