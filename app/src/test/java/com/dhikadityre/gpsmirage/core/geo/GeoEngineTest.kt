package com.dhikadityre.gpsmirage.core.geo

import com.dhikadityre.gpsmirage.core.model.Coordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeoEngineTest {

    private lateinit var geoEngine: GeoEngine

    @Before
    fun setUp() {
        geoEngine = GeoEngineImpl()
    }

    @Test
    fun `generateCirclePolygon returns empty list for non-positive radius or invalid points`() {
        val center = Coordinate(latitude = -6.2088, longitude = 106.8456)
        
        assertTrue(geoEngine.generateCirclePolygon(center, 0.0).isEmpty())
        assertTrue(geoEngine.generateCirclePolygon(center, -50.0).isEmpty())
        assertTrue(geoEngine.generateCirclePolygon(center, 100.0, numberOfPoints = 2).isEmpty())
    }

    @Test
    fun `generateCirclePolygon generates exact count of perimeter coordinates`() {
        val center = Coordinate(latitude = -6.2088, longitude = 106.8456)
        val radius = 500.0

        val polygon360 = geoEngine.generateCirclePolygon(center, radius, numberOfPoints = 360)
        assertEquals(360, polygon360.size)

        val polygon60 = geoEngine.generateCirclePolygon(center, radius, numberOfPoints = 60)
        assertEquals(60, polygon60.size)
    }

    @Test
    fun `generateCirclePolygon points are all at requested radius distance from center`() {
        val center = Coordinate(latitude = -6.2088, longitude = 106.8456)
        val radius = 750.0 // 750 meters

        val polygon = geoEngine.generateCirclePolygon(center, radius, numberOfPoints = 72)
        assertEquals(72, polygon.size)

        polygon.forEachIndexed { index, point ->
            val dist = geoEngine.distance(center, point)
            // Expect distance within 0.5 meter tolerance due to geodesic rounding
            assertEquals(
                "Point at index $index distance mismatch",
                radius,
                dist,
                0.5
            )
        }
    }

    @Test
    fun `generateCirclePolygon points start North and progress clockwise`() {
        val center = Coordinate(latitude = 0.0, longitude = 0.0)
        val radius = 1000.0

        val polygon = geoEngine.generateCirclePolygon(center, radius, numberOfPoints = 4)
        assertEquals(4, polygon.size)

        // Point 0: Bearing 0 (North) -> latitude > 0, longitude ~= 0
        assertTrue(polygon[0].latitude > 0.0)
        assertEquals(0.0, polygon[0].longitude, 0.0001)

        // Point 1: Bearing 90 (East) -> latitude ~= 0, longitude > 0
        assertEquals(0.0, polygon[1].latitude, 0.0001)
        assertTrue(polygon[1].longitude > 0.0)

        // Point 2: Bearing 180 (South) -> latitude < 0, longitude ~= 0
        assertTrue(polygon[2].latitude < 0.0)
        assertEquals(0.0, polygon[2].longitude, 0.0001)

        // Point 3: Bearing 270 (West) -> latitude ~= 0, longitude < 0
        assertEquals(0.0, polygon[3].latitude, 0.0001)
        assertTrue(polygon[3].longitude < 0.0)
    }

    @Test
    fun `generateRealisticSubMocks generates points strictly within radius with realistic telemetry and generated flag`() {
        val anchor = com.dhikadityre.gpsmirage.core.model.Location(
            id = 100L,
            name = "Monas Center Anchor",
            latitude = -6.175392,
            longitude = 106.827153,
            type = com.dhikadityre.gpsmirage.core.model.LocationType.CENTER_ANCHOR,
            radiusMeters = 300.0,
            altitude = 15.0
        )

        val subMocks = geoEngine.generateRealisticSubMocks(anchor, count = 5, startIndex = 1)

        assertEquals(5, subMocks.size)
        subMocks.forEachIndexed { index, mock ->
            // Verification 1: Flagging
            assertTrue("Sub-mock should have isGenerated flag true", mock.isGenerated)
            assertEquals(100L, mock.parentAnchorId)
            assertEquals("Mock %02d".format(index + 1), mock.name)

            // Verification 2: Within radius
            val distance = geoEngine.distance(anchor.coordinate, mock.coordinate)
            assertTrue(
                "Point distance $distance m must be <= anchor radius 300m",
                distance <= 300.0 + 1.0 // 1m tolerance for geodesic floating point
            )

            // Verification 3: Realistic altitude (ground level, never > 35m, not 20-story building)
            assertTrue("Altitude ${mock.altitude} must be >= 5m", mock.altitude >= 5.0)
            assertTrue("Altitude ${mock.altitude} must be <= 35m", mock.altitude <= 35.0)

            // Verification 4: Realistic mobile GPS accuracy (3.5m - 8.5m)
            assertTrue("Accuracy ${mock.accuracy} must be >= 3.5m", mock.accuracy >= 3.5f)
            assertTrue("Accuracy ${mock.accuracy} must be <= 8.5f", mock.accuracy <= 8.5f)

            // Verification 5: Speed stationary
            assertEquals(0f, mock.speed)
        }
    }
}
