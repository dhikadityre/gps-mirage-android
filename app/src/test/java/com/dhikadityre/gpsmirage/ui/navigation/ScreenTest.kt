package com.dhikadityre.gpsmirage.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTest {

    @Test
    fun `tools sub-screens have distinct valid route identifiers`() {
        val toolsRoutes = listOf(
            Screen.Generator.route,
            Screen.Import.route,
            Screen.Export.route,
            Screen.GeofenceRadius.route,
            Screen.DefaultValues.route,
            Screen.AlarmScheduler.route,
            Screen.AppearanceSettings.route
        )

        assertEquals("All tools routes must be unique", toolsRoutes.size, toolsRoutes.distinct().size)
        toolsRoutes.forEach { route ->
            assertTrue("Route should not be blank", route.isNotBlank())
        }
    }

    @Test
    fun `bottom navigation items include tools tab with correct route and label`() {
        val toolsTab = BottomNavDestination.TOOLS
        assertEquals(Screen.Tools, toolsTab.screen)
        assertEquals("Tools", toolsTab.label)
        assertEquals("tools", toolsTab.screen.route)
    }

    @Test
    fun `all core bottom destinations are mapped`() {
        val destinations = BottomNavDestination.values()
        assertEquals(4, destinations.size)
        assertTrue(destinations.contains(BottomNavDestination.MAP))
        assertTrue(destinations.contains(BottomNavDestination.LOCATIONS))
        assertTrue(destinations.contains(BottomNavDestination.TOOLS))
        assertTrue(destinations.contains(BottomNavDestination.MORE))
    }
}
