package com.pasindu.woundlab.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TelemetryLog
 *
 * Stores raw sensor data for the Optical Engine's math verification.
 *
 * Blueprint Requirement:
 * - "Room for telemetry" to log sensor drift and leveling accuracy.
 * - Used to validate that the device was held parallel to the wound surface
 * during the stereophotogrammetry capture.
 */
@Entity(tableName = "telemetry_logs")
data class TelemetryLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sessionId: String, // Links logs to a specific capture session
    val pitch: Float,      // X-axis rotation (Device tilt)
    val roll: Float,       // Y-axis rotation (Device leveling)
    val azimuth: Float,    // Z-axis rotation (Compass direction)
    val luminosity: Float  // Ambient light level (Lux) - Critical for exposure checks
)