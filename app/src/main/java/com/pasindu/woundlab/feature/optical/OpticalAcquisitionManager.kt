package com.pasindu.woundlab.feature.optical

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.SizeF
import android.view.Surface
import com.pasindu.woundlab.domain.model.LensIntrinsics
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * OpticalAcquisitionManager
 *
 * The hardware binder for the Redmi Note 14 5G.
 *
 * Responsibilities:
 * 1. Identify the Logical Multi-Camera (Back-facing array).
 * 2. Bind to Physical Sensors: 108MP (Main) and 8MP (Ultrawide).
 * 3. Extract Lens Intrinsics for the StereoMath engine.
 * 4. Manage Raw Stereoscopic Capture Sessions.
 *
 * STRICT REQUIREMENT:
 * Must use getPhysicalCameraIds() to bypass the OS's default zoom fusion
 * and access raw stereoscopic data.
 */
@Singleton
class OpticalAcquisitionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraManager: CameraManager
) {

    private val mainExecutor: Executor = context.mainExecutor

    data class StereoCameraIds(
        val logicalId: String,
        val mainPhysicalId: String,
        val ultraPhysicalId: String,
        val mainIntrinsics: LensIntrinsics,
        val ultraIntrinsics: LensIntrinsics
    )

    /**
     * Scans for a dual-camera setup suitable for stereophotogrammetry.
     * Returns the Logical ID and the two Physical IDs (Main, Ultrawide).
     */
    fun findStereoCalibration(): StereoCameraIds? {
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

                        var mainId: String? = null
                        var ultraId: String? = null
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
                                    mainId = physId
                                    mainLens = intrinsics
                                } else {
                                    ultraId = physId
                                    ultraLens = intrinsics
                                }
                            }
                        }

                        if (mainId != null && ultraId != null && mainLens != null && ultraLens != null) {
                            Timber.d("Stereo Pair Bound: Main=$mainId, Ultra=$ultraId")
                            return StereoCameraIds(
                                logicalId = id,
                                mainPhysicalId = mainId,
                                ultraPhysicalId = ultraId,
                                mainIntrinsics = mainLens,
                                ultraIntrinsics = ultraLens
                            )
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
     * Opens the Logical Camera.
     */
    @SuppressLint("MissingPermission")
    suspend fun openCamera(logicalCameraId: String): CameraDevice = suspendCoroutine { cont ->
        try {
            cameraManager.openCamera(logicalCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cont.resume(camera)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    Timber.w("Camera $logicalCameraId disconnected")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cont.resumeWithException(RuntimeException("Camera open error: $error"))
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }

    /**
     * Starts a Stereoscopic Capture Session.
     * Binds specific Surfaces to specific Physical IDs using OutputConfiguration.
     */
    fun startStereoSession(
        cameraDevice: CameraDevice,
        ids: StereoCameraIds,
        mainSurface: Surface,
        ultraSurface: Surface,
        onSessionConfigured: (CameraCaptureSession) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 1. Configure Main Sensor Output
            val mainConfig = OutputConfiguration(mainSurface)
            mainConfig.setPhysicalCameraId(ids.mainPhysicalId)

            // 2. Configure Ultrawide Sensor Output
            val ultraConfig = OutputConfiguration(ultraSurface)
            ultraConfig.setPhysicalCameraId(ids.ultraPhysicalId)

            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(mainConfig, ultraConfig),
                mainExecutor,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Timber.i("Stereo Session Configured")
                        onSessionConfigured(session)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Timber.e("Stereo Session Configuration Failed")
                    }
                }
            )
            cameraDevice.createCaptureSession(sessionConfig)
        } else {
            Timber.e("Stereoscopic physical binding requires Android P (API 28)+")
        }
    }

    /**
     * Creates a CaptureRequest that disables AI processing and retrieves raw optical data.
     */
    fun createStereoRequest(
        session: CameraCaptureSession,
        ids: StereoCameraIds,
        mainSurface: Surface,
        ultraSurface: Surface
    ): CaptureRequest {
        val builder = session.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

        // Add targets
        builder.addTarget(mainSurface)
        builder.addTarget(ultraSurface)

        // --- ZERO-COST OPTICAL ENGINE CONFIGURATION ---

        // 1. Disable Distortion Correction to get raw pixels for Brown-Conrady math
        builder.set(CaptureRequest.DISTORTION_CORRECTION_MODE, CaptureRequest.DISTORTION_CORRECTION_MODE_OFF)

        // 2. Enable Lens Shading Map to correct vignetting in post-process
        builder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON)

        // 3. Disable Scene Mode (AI Beautification/Scene Detection)
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        builder.set(CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.CONTROL_SCENE_MODE_DISABLED)

        // 4. Lock Optical Stabilization if possible (optional, but good for stereo consistency)
        builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON)

        return builder.build()
    }

    /**
     * Extracts or Estimates the Brown-Conrady calibration data.
     */
    private fun extractIntrinsics(chars: CameraCharacteristics): LensIntrinsics {
        // Method A: Try to get the official factory calibration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val calibration = chars.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
            if (calibration != null) {
                // Ensure we have at least 5 coefficients (K1, K2, K3, P1, P2)
                val distSize = calibration.size - 5
                val distortion = if (distSize >= 5) {
                    calibration.sliceArray(IntRange(5, 9)) // Standard Brown-Conrady usually 5 coeffs
                } else {
                    floatArrayOf(0f, 0f, 0f, 0f, 0f)
                }

                return LensIntrinsics(
                    focalLengthX = calibration[0],
                    focalLengthY = calibration[1],
                    principalPointX = calibration[2],
                    principalPointY = calibration[3],
                    distortion = distortion
                )
            }
        }

        // Method B: Fallback Estimation (Zero-Cost / Non-Calibrated Device)
        val focalLengthMm = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.get(0) ?: 4.8f
        val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: SizeF(6.4f, 4.8f)
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
            distortion = floatArrayOf(0f, 0f, 0f, 0f, 0f)
        )
    }
}