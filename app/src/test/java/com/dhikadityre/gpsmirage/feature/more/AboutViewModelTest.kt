package com.dhikadityre.gpsmirage.feature.more

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AboutViewModelTest {

    private lateinit var viewModel: AboutViewModel

    @Before
    fun setUp() {
        viewModel = AboutViewModel()
    }

    @Test
    fun `about ui state exposes app version, developer and purpose`() {
        val state = viewModel.uiState.value

        assertEquals("GPS Mirage", state.appName)
        assertTrue("Version name should not be blank", state.versionName.isNotBlank())
        assertTrue("Version code should be positive", state.versionCode > 0)
        assertEquals("Dhikadityre", state.developerName)
        assertTrue("App purpose must be detailed", state.appPurpose.contains("geolokasi"))
        assertEquals(35, state.targetSdk)
        assertEquals(23, state.minSdk)
    }
}
