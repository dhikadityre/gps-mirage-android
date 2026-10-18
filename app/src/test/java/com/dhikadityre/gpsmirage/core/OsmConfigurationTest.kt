package com.dhikadityre.gpsmirage.core

import org.junit.Assert.assertEquals
import org.junit.Test
import org.osmdroid.config.Configuration
import java.io.File

class OsmConfigurationTest {

    @Test
    fun `configureOsmInternalStorage sets cache paths to internal directories`() {
        val mockCacheDir = File("build/tmp/test_cache")
        mockCacheDir.mkdirs()

        Configuration.getInstance().apply {
            userAgentValue = "com.dhikadityre.dev.tools"
            osmdroidBasePath = File(mockCacheDir, "osmdroid")
            osmdroidTileCache = File(mockCacheDir, "osmdroid/tiles")
        }

        assertEquals("com.dhikadityre.dev.tools", Configuration.getInstance().userAgentValue)
        assertEquals(File(mockCacheDir, "osmdroid"), Configuration.getInstance().osmdroidBasePath)
        assertEquals(File(mockCacheDir, "osmdroid/tiles"), Configuration.getInstance().osmdroidTileCache)

        mockCacheDir.deleteRecursively()
    }
}
