package com.dhikadityre.gpsmirage.core.geo

import com.dhikadityre.gpsmirage.core.model.Coordinate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

interface GeoEngine {
    /**
     * Calculate distance between two coordinates in meters (Haversine formula).
     */
    fun distance(from: Coordinate, to: Coordinate): Double

    /**
     * Calculate bearing from one coordinate to another in degrees.
     */
    fun bearing(from: Coordinate, to: Coordinate): Double

    /**
     * Get destination coordinate given start, bearing (degrees), and distance (meters).
     */
    fun destinationPoint(start: Coordinate, bearingDeg: Double, distanceMeters: Double): Coordinate

    /**
     * Generate [count] random points uniformly distributed inside a circle of [radiusMeters].
     * Uses correct geographic math (not naive lat/lng ±random which produces a square).
     */
    fun generatePointsWithinRadius(
        center: Coordinate,
        radiusMeters: Double,
        count: Int
    ): List<Coordinate>

    /**
     * Generate an ordered list of [numberOfPoints] coordinates forming a closed polygon circle
     * around [center] at [radiusMeters] distance.
     * The points are ordered clockwise starting from bearing 0 (North).
     */
    fun generateCirclePolygon(
        center: Coordinate,
        radiusMeters: Double,
        numberOfPoints: Int = 360
    ): List<Coordinate>

    /**
     * Validate that a coordinate is within valid ranges.
     */
    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean

    /**
     * Validate latitude only.
     */
    fun isValidLatitude(latitude: Double): Boolean

    /**
     * Validate longitude only.
     */
    fun isValidLongitude(longitude: Double): Boolean
}

@Singleton
class GeoEngineImpl @Inject constructor() : GeoEngine {

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0
        private val DEG_TO_RAD = PI / 180.0
        private val RAD_TO_DEG = 180.0 / PI
    }

    override fun distance(from: Coordinate, to: Coordinate): Double {
        val lat1 = from.latitude * DEG_TO_RAD
        val lat2 = to.latitude * DEG_TO_RAD
        val dLat = (to.latitude - from.latitude) * DEG_TO_RAD
        val dLon = (to.longitude - from.longitude) * DEG_TO_RAD

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    override fun bearing(from: Coordinate, to: Coordinate): Double {
        val lat1 = from.latitude * DEG_TO_RAD
        val lat2 = to.latitude * DEG_TO_RAD
        val dLon = (to.longitude - from.longitude) * DEG_TO_RAD

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        return (atan2(y, x) * RAD_TO_DEG + 360) % 360
    }

    override fun destinationPoint(
        start: Coordinate,
        bearingDeg: Double,
        distanceMeters: Double
    ): Coordinate {
        val lat1 = start.latitude * DEG_TO_RAD
        val lon1 = start.longitude * DEG_TO_RAD
        val brng = bearingDeg * DEG_TO_RAD
        val angularDist = distanceMeters / EARTH_RADIUS_METERS

        val lat2 = asin(
            sin(lat1) * cos(angularDist) +
                    cos(lat1) * sin(angularDist) * cos(brng)
        )

        val lon2 = lon1 + atan2(
            sin(brng) * sin(angularDist) * cos(lat1),
            cos(angularDist) - sin(lat1) * sin(lat2)
        )

        return Coordinate(
            latitude = lat2 * RAD_TO_DEG,
            longitude = ((lon2 * RAD_TO_DEG) + 540) % 360 - 180
        )
    }

    override fun generatePointsWithinRadius(
        center: Coordinate,
        radiusMeters: Double,
        count: Int
    ): List<Coordinate> {
        val result = mutableListOf<Coordinate>()
        val random = java.util.Random()

        repeat(count) {
            // Uniform random point in circle: r = R * sqrt(random)
            val angle = random.nextDouble() * 2 * PI
            val r = radiusMeters * sqrt(random.nextDouble())

            // Convert polar offset to geographic coordinate
            val bearingDeg = angle * RAD_TO_DEG
            val point = destinationPoint(center, bearingDeg, r)
            result.add(point)
        }

        return result
    }

    override fun generateCirclePolygon(
        center: Coordinate,
        radiusMeters: Double,
        numberOfPoints: Int
    ): List<Coordinate> {
        if (radiusMeters <= 0.0 || numberOfPoints < 3) return emptyList()

        val points = ArrayList<Coordinate>(numberOfPoints)
        val stepDeg = 360.0 / numberOfPoints

        for (i in 0 until numberOfPoints) {
            val bearingDeg = i * stepDeg
            points.add(destinationPoint(center, bearingDeg, radiusMeters))
        }

        return points
    }

    override fun isValidCoordinate(latitude: Double, longitude: Double): Boolean =
        isValidLatitude(latitude) && isValidLongitude(longitude)

    override fun isValidLatitude(latitude: Double): Boolean =
        latitude in -90.0..90.0

    override fun isValidLongitude(longitude: Double): Boolean =
        longitude in -180.0..180.0
}
