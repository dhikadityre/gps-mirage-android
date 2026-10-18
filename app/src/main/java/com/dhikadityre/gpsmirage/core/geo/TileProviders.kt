package com.dhikadityre.gpsmirage.core.geo

import com.dhikadityre.gpsmirage.core.model.MapLayerStyle
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex

/**
 * Tile providers for OSMDroid supporting Google Maps Roadmap, Satellite, Hybrid,
 * and OpenStreetMap Standard.
 */
object TileProviders {

    val GOOGLE_ROADMAP: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "Google-Roadmap",
        0, 20, 256, ".png",
        arrayOf(
            "https://mt0.google.com/vt/lyrs=m",
            "https://mt1.google.com/vt/lyrs=m",
            "https://mt2.google.com/vt/lyrs=m",
            "https://mt3.google.com/vt/lyrs=m"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "$baseUrl&hl=en&x=$x&y=$y&z=$z"
        }
    }

    val GOOGLE_SATELLITE: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "Google-Satellite",
        0, 20, 256, ".jpg",
        arrayOf(
            "https://mt0.google.com/vt/lyrs=s",
            "https://mt1.google.com/vt/lyrs=s",
            "https://mt2.google.com/vt/lyrs=s",
            "https://mt3.google.com/vt/lyrs=s"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "$baseUrl&hl=en&x=$x&y=$y&z=$z"
        }
    }

    val GOOGLE_HYBRID: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "Google-Hybrid",
        0, 20, 256, ".png",
        arrayOf(
            "https://mt0.google.com/vt/lyrs=y",
            "https://mt1.google.com/vt/lyrs=y",
            "https://mt2.google.com/vt/lyrs=y",
            "https://mt3.google.com/vt/lyrs=y"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "$baseUrl&hl=en&x=$x&y=$y&z=$z"
        }
    }

    val OSM_STANDARD: ITileSource = TileSourceFactory.MAPNIK

    fun getTileSource(style: MapLayerStyle): ITileSource = when (style) {
        MapLayerStyle.GOOGLE_ROADMAP -> GOOGLE_ROADMAP
        MapLayerStyle.GOOGLE_SATELLITE -> GOOGLE_SATELLITE
        MapLayerStyle.GOOGLE_HYBRID -> GOOGLE_HYBRID
        MapLayerStyle.OSM_STANDARD -> OSM_STANDARD
    }
}
