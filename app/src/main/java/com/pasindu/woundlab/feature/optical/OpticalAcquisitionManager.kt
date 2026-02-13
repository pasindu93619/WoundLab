package com.pasindu.woundlab.feature.optical

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.SizeF
import com.pasindu.woundlab.domain.model.LensIntrinsics
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan

/**
 * OpticalAcquisitionManager
 *
 * The hardware binder for the Redmi Note 14 5G.
 *
 * Responsibilities:
 * 1. Identify the Logical Multi-Camera (Back-facing array).
 * 2. Bind to Physical Sensors: 108MP (Main) and 8MP (Ultrawide).
 * 3. Extract Lens Intrinsics for the StereoMath engine.
 *
 * STRICT REQUIREMENT:
 * Must use getPhysicalCameraIds() to bypass the OS's default zoom fusion
 * and access raw stereoscopic data.
 */
@Singleton
class OpticalAcquisitionManager @Inject constructor(
    private val cameraManager: CameraManager
) {

    /**
     * Scans for a dual-camera setup suitable for stereophotogrammetry.
     * @return Pair<MainLens, UltrawideLens> or null if incompatible.
     */
    fun findStereoCalibration(): Pair<LensIntrinsics, LensIntrinsics>? {
        try {
            val cameraIds = cameraManager.cameraIdList

            for (id in cameraIds) {
                val chars = cameraManager.getCameraCharacteristics(id)

                // 1. Must be Back-Facing
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing != CameraCharacteristics.LENS_FACING_BACK) continue

                // 2. Must be a Logical Multi-Camera (The container for physical lenses)
                val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: continue
                val isLogicalMultiCam = capabilities.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
                )

                if (isLogicalMultiCam && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val physicalIds = chars.physicalCameraIds

                    if (physicalIds.size >= 2) {
                        Timber.i("Found Logical Camera: $id with Physical IDs: $physicalIds")

                        // We need to identify which ID is Main (Standard) and which is Ultrawide.
                        // We differentiate based on Focal Length.
                        var mainLens: LensIntrinsics? = null
                        var ultraLens: LensIntrinsics? = null

                        for (physId in physicalIds) {
                            val physChars = cameraManager.getCameraCharacteristics(physId)
                            val focalLengths = physChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                            val focalLength = focalLengths?.get(0) ?: 0f // Millimeters

                            // Heuristic: Ultrawide is usually < 4mm, Main is usually > 5mm on phones
                            if (focalLength > 0) {
                                val intrinsics = extractIntrinsics(physChars)

                                if (focalLength > 4.5f) {
                                    mainLens = intrinsics
                                } else {
                                    ultraLens = intrinsics
                                }
                            }
                        }

                        if (mainLens != null && ultraLens != null) {
                            Timber.d("Stereo Pair Bound: Main=${mainLens}, Ultra=${ultraLens}")
                            return Pair(mainLens, ultraLens)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to bind physical cameras")
        }
        return null
    }

    /**
     * Extracts or Estimates the Brown-Conrady calibration data.
     */
    private fun extractIntrinsics(chars: CameraCharacteristics): LensIntrinsics {
        // Method A: Try to get the official factory calibration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val calibration = chars.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
            if (calibration != null) {
                return LensIntrinsics(
                    focalLengthX = calibration[0],
                    focalLengthY = calibration[1],
                    principalPointX = calibration[2],
                    principalPointY = calibration[3],
                    distortion = calibration.sliceArray(IntRange(4, calibration.size - 1))
                )
            }
        }

        // Method B: Fallback Estimation (Zero-Cost / Non-Calibrated Device)
        // Calculate focal length in pixels using Sensor Size and Field of View
        val focalLengthMm = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.get(0) ?: 4.8f
        val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: SizeF(6.4f, 4.8f) // Default 1/2" sensor
        val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

        val widthPixels = activeArray?.width() ?: 4000
        val heightPixels = activeArray?.height() ?: 3000

        // Pixels per millimeter
        val pixPerMmX = widthPixels / sensorSize.width
        val pixPerMmY = heightPixels / sensorSize.height

        val fx = focalLengthMm * pixPerMmX
        val fy = focalLengthMm * pixPerMmY
        val cx = widthPixels / 2f
        val cy = heightPixels / 2f

        Timber.w("Using Fallback Intrinsics (No factory calibration found)")

        return LensIntrinsics(
            focalLengthX = fx,
            focalLengthY = fy,
            principalPointX = cx,
            principalPointY = cy,
            distortion = floatArrayOf(0f, 0f, 0f, 0f, 0f) // Assume ideal pinhole if no data
        )
    }
}