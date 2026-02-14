package com.pasindu.woundlab.di

import android.content.Context
import android.hardware.camera2.CameraManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * CameraModule
 *
 * Responsibilities:
 * 1. Provides the system CameraManager service to the OpticalAcquisitionManager.
 *
 * Note: OpticalAcquisitionManager itself is bound via @Inject constructor,
 * so it does not need an explicit provider here.
 */
@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideCameraManager(
        @ApplicationContext context: Context
    ): CameraManager {
        return context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
}