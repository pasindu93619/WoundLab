package com.pasindu.woundlab

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * WoundLabApp
 *
 * The strict entry point for the WoundLab application.
 *
 * Responsibilities:
 * 1. Triggers Hilt Dependency Injection via @HiltAndroidApp.
 * 2. Initializes Timber for telemetry debugging of the Optical Engine.
 *
 * Note: The FHIR Engine and Room Database are initialized lazily via
 * the DatabaseModule to ensure startup performance.
 */
@HiltAndroidApp
class WoundLabApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.i("WoundLab System Initialized - Telemetry Active")
        }
    }
}