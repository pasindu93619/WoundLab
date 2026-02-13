package com.pasindu.woundlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasindu.woundlab.data.local.entity.TelemetryLog
import kotlinx.coroutines.flow.Flow

/**
 * TelemetryDao
 *
 * Data Access Object for high-frequency sensor logging.
 * Returns Flow<> to allow real-time graph updates in Vico charts.
 */
@Dao
interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TelemetryLog)

    @Query("SELECT * FROM telemetry_logs WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getLogsBySession(sessionId: String): Flow<List<TelemetryLog>>

    @Query("DELETE FROM telemetry_logs")
    suspend fun clearAll()
}