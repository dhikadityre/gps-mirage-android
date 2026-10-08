package com.dhikadityre.gpsmirage.core.parser

import com.dhikadityre.gpsmirage.core.model.LocationDraft
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// ─── JSON Schema ─────────────────────────────────────────────────────────────

@Serializable
data class LocationJsonSchema(
    val version: Int = 1,
    val name: String? = null,
    val locations: List<LocationItemJson>
)

@Serializable
data class LocationItemJson(
    val name: String? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 5f,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val bearing: Float = 0f
)

// ─── Parse Result ─────────────────────────────────────────────────────────────

sealed interface ParseResult {
    data class Success(val locations: List<LocationDraft>) : ParseResult
    data class Partial(val valid: List<LocationDraft>, val errors: List<ParseError>) : ParseResult
    data class Failure(val error: ParseError) : ParseResult
}

data class ParseError(val line: Int, val raw: String, val reason: String)

// ─── Parser ──────────────────────────────────────────────────────────────────

/**
 * Smart multi-format location parser.
 * Supports:
 *   Format A: -6.200123,106.816456
 *   Format B: Jakarta Office,-6.200123,106.816456
 *   Format C: Jakarta Office | -6.200123 | 106.816456
 *   Format D: Jakarta Office\n-6.200123\n106.816456 (multi-line per location)
 *   Format E: -6.200123 106.816456
 *   JSON:     {"version":1,"locations":[...]}
 */
interface LocationParser {
    fun parse(input: String): ParseResult
    fun parseJson(json: String): ParseResult
}

@Singleton
class LocationParserImpl @Inject constructor() : LocationParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun parse(input: String): ParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ParseResult.Failure(ParseError(0, "", "Empty input"))

        // Detect JSON
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return parseJson(trimmed)
        }

        return parseText(trimmed)
    }

    override fun parseJson(jsonString: String): ParseResult {
        return try {
            val schema = json.decodeFromString<LocationJsonSchema>(jsonString)
            val drafts = schema.locations.mapIndexed { index, item ->
                LocationDraft(
                    name = item.name ?: "Location %03d".format(index + 1),
                    latitude = item.latitude,
                    longitude = item.longitude,
                    accuracy = item.accuracy,
                    altitude = item.altitude,
                    speed = item.speed,
                    bearing = item.bearing
                )
            }
            ParseResult.Success(drafts)
        } catch (e: Exception) {
            ParseResult.Failure(ParseError(0, jsonString.take(80), "Invalid JSON: ${e.message}"))
        }
    }

    private fun parseText(input: String): ParseResult {
        val valid = mutableListOf<LocationDraft>()
        val errors = mutableListOf<ParseError>()

        // Try multi-line format D first: group by blank lines or name/lat/lng triplets
        val lines = input.lines().map { it.trim() }.filter { it.isNotBlank() }
        var i = 0
        var locationIndex = 1

        while (i < lines.size) {
            val line = lines[i]

            // Try single-line formats (A, B, C, E)
            val draft = tryParseSingleLine(line, locationIndex)
            if (draft != null) {
                valid.add(draft)
                locationIndex++
                i++
                continue
            }

            // Try Format D: name on one line, lat on next, lon on next
            if (i + 2 < lines.size) {
                val name = line
                val latStr = lines[i + 1]
                val lonStr = lines[i + 2]
                val lat = latStr.toDoubleOrNull()
                val lon = lonStr.toDoubleOrNull()
                if (lat != null && lon != null && isValidLat(lat) && isValidLon(lon) &&
                    // Make sure "name" line doesn't look like a coordinate
                    name.toDoubleOrNull() == null
                ) {
                    valid.add(LocationDraft(name = name, latitude = lat, longitude = lon))
                    locationIndex++
                    i += 3
                    continue
                }
            }

            // Could not parse this line
            errors.add(ParseError(i + 1, line, "Unrecognized format"))
            i++
        }

        return when {
            valid.isEmpty() && errors.isEmpty() ->
                ParseResult.Failure(ParseError(0, "", "No locations found"))
            errors.isEmpty() ->
                ParseResult.Success(valid)
            valid.isEmpty() ->
                ParseResult.Failure(ParseError(0, input.take(80), "No valid locations found"))
            else ->
                ParseResult.Partial(valid, errors)
        }
    }

    private fun tryParseSingleLine(line: String, index: Int): LocationDraft? {
        // Format C: name | lat | lon (pipe separator)
        if (line.contains("|")) {
            val parts = line.split("|").map { it.trim() }
            if (parts.size >= 2) {
                return tryExtractNameLatLon(parts, index)
            }
        }

        // Format B/A/E: comma or space separated
        val parts = line.split(",", " ", "\t")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return tryExtractNameLatLon(parts, index)
    }

    private fun tryExtractNameLatLon(parts: List<String>, index: Int): LocationDraft? {
        if (parts.size < 2) return null

        // Try last two parts as lat/lon
        val lonStr = parts.last()
        val latStr = parts[parts.size - 2]
        val lat = latStr.toDoubleOrNull()
        val lon = lonStr.toDoubleOrNull()

        if (lat != null && lon != null && isValidLat(lat) && isValidLon(lon)) {
            val name = if (parts.size > 2) {
                parts.dropLast(2).joinToString(" ")
            } else {
                "Location %03d".format(index)
            }
            return LocationDraft(name = name, latitude = lat, longitude = lon)
        }

        // Try first two parts as lat/lon (no name)
        if (parts.size == 2) {
            val la = parts[0].toDoubleOrNull()
            val lo = parts[1].toDoubleOrNull()
            if (la != null && lo != null && isValidLat(la) && isValidLon(lo)) {
                return LocationDraft(
                    name = "Location %03d".format(index),
                    latitude = la,
                    longitude = lo
                )
            }
        }

        return null
    }

    private fun isValidLat(lat: Double) = lat in -90.0..90.0
    private fun isValidLon(lon: Double) = lon in -180.0..180.0
}
