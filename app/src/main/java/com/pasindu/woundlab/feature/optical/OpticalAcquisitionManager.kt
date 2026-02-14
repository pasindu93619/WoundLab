package com.pasindu.woundlab.feature.optical

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import com.pasindu.woundlab.domain.model.LensIntrinsics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OpticalAcquisitionManager
 *
 * The hardware abstraction layer for the Zero-Cost Optical Engine.
 */
class OpticalAcquisitionManager @Inject constructor(
    @ApplicationContext private val context: Context, // FIXED: Explicit @ApplicationContext
    private val cameraManager: CameraManager
) {

    data class StereoCameraIds(
        val logicalId: String,
        val mainPhysicalId: String,
        val ultraPhysicalId: String,
        val mainIntrinsics: LensIntrinsics,
        val ultraIntrinsics: LensIntrinsics
    )

    fun findStereoCalibration(): StereoCameraIds? {
        val cameraIds = cameraManager.cameraIdList
        for (id in cameraIds) {
            val chars = cameraManager.getCameraCharacteristics(id)
            val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: continue
            val isLogical = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)

            if (isLogical) {
                val physicalIds = chars.physicalCameraIds
                if (physicalIds.size >= 2) {
                    var mainId: String? = null
                    var ultraId: String? = null
                    var mainIntrinsics: LensIntrinsics? = null
                    var ultraIntrinsics: LensIntrinsics? = null

                    for (physId in physicalIds) {
                        val physChars = cameraManager.getCameraCharacteristics(physId)
                        val focalLengths = physChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        val focalLength = focalLengths?.firstOrNull() ?: 0f

                        if (focalLength > 3.0f) {
                            mainId = physId
                            mainIntrinsics = recoverIntrinsics(physChars)
                        } else if (focalLength < 3.0f) {
                            ultraId = physId
                            ultraIntrinsics = recoverIntrinsics(physChars)
                        }
                    }

                    if (mainId != null && ultraId != null && mainIntrinsics != null && ultraIntrinsics != null) {
                        Timber.i("Stereo Pair Found: Logical=$id, Main=$mainId, Ultra=$ultraId")
                        return StereoCameraIds(id, mainId, ultraId, mainIntrinsics, ultraIntrinsics)
                    }
                }
            }
        }
        return null
    }

    private fun recoverIntrinsics(chars: CameraCharacteristics): LensIntrinsics {
        val intrinsicsArray = chars.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
        val pixelArraySize = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val physicalSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

        if (intrinsicsArray != null) {
            val distortion = if (intrinsicsArray.size >= 5) intrinsicsArray.sliceArray(0..4) else null
            return LensIntrinsics(intrinsicsArray[0], intrinsicsArray[1], intrinsicsArray[2], intrinsicsArray[3], distortion)
        } else {
            val f_mm = focalLengths?.firstOrNull() ?: 4.7f
            val w_mm = physicalSize?.width ?: 6.4f
            val w_px = pixelArraySize?.width ?: 4000
            val f_px = (f_mm / w_mm) * w_px
            return LensIntrinsics(f_px, f_px, (w_px / 2).toFloat(), ((pixelArraySize?.height ?: 3000) / 2).toFloat(), null)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun openCamera(cameraId: String): CameraDevice = suspendCancellableCoroutine { cont ->
        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) = cont.resume(camera)
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    if (cont.isActive) cont.resumeWithException(RuntimeException("Camera disconnected"))
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    if (cont.isActive) cont.resumeWithException(RuntimeException("Camera error: $error"))
                }
            }, null)
        } catch (e: Exception) {
            if (cont.isActive) cont.resumeWithException(e)
        }
    }

    @Suppress("DEPRECATION") // Suppressed for Android 8.0 (API 26) compatibility
    fun startStereoSession(
        device: CameraDevice,
        config: StereoCameraIds,
        mainSurface: Surface,
        ultraSurface: Surface,
        onSessionReady: (android.hardware.camera2.CameraCaptureSession) -> Unit
    ) {
        val outputs = listOf(mainSurface, ultraSurface)
        // Uses deprecated createCaptureSession for broad device support (Redmi Note 14 & older)
        device.createCaptureSession(outputs, object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) = onSessionReady(session)
            override fun onConfigureFailed(session: android.hardware.camera2.CameraCaptureSession) = Timber.e("Session configuration failed")
        }, null)
    }

    fun createStereoRequest(
        session: android.hardware.camera2.CameraCaptureSession,
        config: StereoCameraIds,
        mainSurface: Surface,
        ultraSurface: Surface
    ): CaptureRequest {
        val builder = session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder.addTarget(mainSurface)
        builder.addTarget(ultraSurface)
        builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF)
        builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
        return builder.build()
    }
}