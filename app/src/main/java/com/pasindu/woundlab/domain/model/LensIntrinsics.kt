package com.pasindu.woundlab.domain.model

/**
 * LensIntrinsics
 *
 * Represents the fundamental optical characteristics of a physical camera sensor.
 * These values are recovered from the Camera2 API (LENS_INTRINSIC_CALIBRATION)
 * and are essential for the pinhole camera model used in depth estimation.
 *
 * Formula:
 * x_screen = fx * (x_world / z) + cx
 * y_screen = fy * (y_world / z) + cy
 *
 * @property focalLengthX (fx): The focal length in pixels along the X-axis.
 * @property focalLengthY (fy): The focal length in pixels along the Y-axis.
 * @property principalPointX (cx): The X-coordinate of the optical center in pixels.
 * @property principalPointY (cy): The Y-coordinate of the optical center in pixels.
 * @property distortion: Radial and tangential distortion coefficients [k1, k2, k3, p1, p2].
 * If null or empty, assume zero distortion.
 */
data class LensIntrinsics(
    val focalLengthX: Float,
    val focalLengthY: Float,
    val principalPointX: Float,
    val principalPointY: Float,
    val distortion: FloatArray? = null // Added to fix 'Unresolved reference'
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LensIntrinsics

        if (focalLengthX != other.focalLengthX) return false
        if (focalLengthY != other.focalLengthY) return false
        if (principalPointX != other.principalPointX) return false
        if (principalPointY != other.principalPointY) return false
        if (distortion != null) {
            if (other.distortion == null) return false
            if (!distortion.contentEquals(other.distortion)) return false
        } else if (other.distortion != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = focalLengthX.hashCode()
        result = 31 * result + focalLengthY.hashCode()
        result = 31 * result + principalPointX.hashCode()
        result = 31 * result + principalPointY.hashCode()
        result = 31 * result + (distortion?.contentHashCode() ?: 0)
        return result
    }
}