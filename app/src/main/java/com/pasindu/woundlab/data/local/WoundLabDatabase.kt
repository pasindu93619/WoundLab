package com.pasindu.woundlab.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pasindu.woundlab.data.local.dao.TelemetryDao
import com.pasindu.woundlab.data.local.entity.TelemetryLog

/**
 * WoundLabDatabase
 *
 * The local persistence container for non-clinical data.
 * Clinical data is handled separately by the FHIR Engine.
 */
@Database(
    entities = [TelemetryLog::class],
    version = 1,
    exportSchema = true
)
abstract class WoundLabDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
}