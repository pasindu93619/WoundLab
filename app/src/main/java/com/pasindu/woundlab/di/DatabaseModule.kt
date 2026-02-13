package com.pasindu.woundlab.di

import android.content.Context
import androidx.room.Room
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.pasindu.woundlab.data.local.WoundLabDatabase
import com.pasindu.woundlab.data.local.dao.TelemetryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DatabaseModule
 *
 * Provides the storage backends mandated by the blueprint:
 * 1. Open Health Stack (FHIR) -> For clinical data (Patients, Observations).
 * 2. Room Database -> For high-frequency sensor telemetry.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // --- FHIR ENGINE (Open Health Stack) ---
    // This provides the clinical database without any paid cloud APIs.
    @Provides
    @Singleton
    fun provideFhirEngine(@ApplicationContext context: Context): FhirEngine {
        return FhirEngineProvider.getInstance(context)
    }

    // --- ROOM DATABASE (Telemetry) ---
    // This provides the sensor logging database.
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WoundLabDatabase {
        return Room.databaseBuilder(
            context,
            WoundLabDatabase::class.java,
            "woundlab_telemetry.db"
        )
            .fallbackToDestructiveMigration() // Allows database updates during dev without crashes
            .build()
    }

    @Provides
    @Singleton
    fun provideTelemetryDao(database: WoundLabDatabase): TelemetryDao {
        return database.telemetryDao()
    }
}