package com.dhikadityre.gpsmirage.core.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddressFormatterTest {

    @Test
    fun `formatShortAddress returns thoroughfare when only thoroughfare is present`() {
        val result = AddressFormatter.formatShortAddress(
            thoroughfare = "Jl. Sudirman",
            subThoroughfare = null,
            featureName = null,
            subLocality = "Kebayoran",
            locality = "Jakarta Selatan"
        )
        assertEquals("Jl. Sudirman", result)
    }

    @Test
    fun `formatShortAddress combines thoroughfare and subThoroughfare`() {
        val result = AddressFormatter.formatShortAddress(
            thoroughfare = "Jl. MH Thamrin",
            subThoroughfare = "10",
            featureName = "10",
            subLocality = "Menteng",
            locality = "Jakarta Pusat"
        )
        assertEquals("Jl. MH Thamrin No. 10", result)
    }

    @Test
    fun `formatShortAddress avoids duplicate number if thoroughfare already contains subThoroughfare`() {
        val result = AddressFormatter.formatShortAddress(
            thoroughfare = "Jl. Gatot Subroto No. 5",
            subThoroughfare = "5",
            featureName = "5",
            subLocality = null,
            locality = null
        )
        assertEquals("Jl. Gatot Subroto No. 5", result)
    }

    @Test
    fun `formatShortAddress falls back to featureName when thoroughfare is null`() {
        val result = AddressFormatter.formatShortAddress(
            thoroughfare = null,
            subThoroughfare = null,
            featureName = "Monumen Nasional",
            subLocality = "Gambir",
            locality = "Jakarta Pusat"
        )
        assertEquals("Monumen Nasional", result)
    }

    @Test
    fun `formatShortAddress falls back to subLocality when thoroughfare and featureName are null`() {
        val result = AddressFormatter.formatShortAddress(
            thoroughfare = null,
            subThoroughfare = null,
            featureName = null,
            subLocality = "Kebayoran Baru",
            locality = "Jakarta Selatan"
        )
        assertEquals("Kebayoran Baru", result)
    }

    @Test
    fun `formatShortAddress falls back to first address line when specific components are null`() {
        val result = AddressFormatter.formatShortAddress(
            thoroughfare = null,
            subThoroughfare = null,
            featureName = null,
            subLocality = null,
            locality = null,
            firstAddressLine = "Jl. Alternatif Cibubur, Cileungsi, Bogor"
        )
        assertEquals("Jl. Alternatif Cibubur", result)
    }

    @Test
    fun `formatShortAddress returns null when all components are null or blank`() {
        val result = AddressFormatter.formatShortAddress(
            thoroughfare = null,
            subThoroughfare = null,
            featureName = "",
            subLocality = "   ",
            locality = null,
            firstAddressLine = null
        )
        assertNull(result)
    }
}
