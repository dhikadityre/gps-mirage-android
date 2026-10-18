package com.dhikadityre.gpsmirage.core.database.entity

import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.LocationType
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationEntityTest {

    @Test
    fun `toDomain and fromDomain correctly preserve LocationType NATIVE`() {
        val domain = Location(
            id = 42L,
            name = "Physical GPS Baseline",
            latitude = -6.2088,
            longitude = 106.8456,
            type = LocationType.NATIVE,
            radiusMeters = 300.0,
            accuracy = 3.8f,
            altitude = 12.0,
            speed = 4.5f,
            bearing = 90.0f
        )

        val entity = LocationEntity.fromDomain(domain)
        assertEquals("NATIVE", entity.locationType)
        assertEquals(42L, entity.id)
        assertEquals("Physical GPS Baseline", entity.name)
        assertEquals(300.0, entity.radiusMeters!!, 0.001)

        val mappedBack = entity.toDomain()
        assertEquals(LocationType.NATIVE, mappedBack.type)
        assertEquals(domain.name, mappedBack.name)
        assertEquals(domain.latitude, mappedBack.latitude, 0.000001)
        assertEquals(domain.longitude, mappedBack.longitude, 0.000001)
        assertEquals(300.0, mappedBack.radiusMeters!!, 0.001)
        assertEquals(3.8f, mappedBack.accuracy, 0.01f)
    }

    @Test
    fun `toDomain fallback to MOCK_TARGET on invalid type string`() {
        val entity = LocationEntity(
            id = 10L,
            name = "Corrupted Type",
            latitude = 0.0,
            longitude = 0.0,
            locationType = "UNKNOWN_NON_EXISTENT"
        )
        val domain = entity.toDomain()
        assertEquals(LocationType.MOCK_TARGET, domain.type)
    }
}
