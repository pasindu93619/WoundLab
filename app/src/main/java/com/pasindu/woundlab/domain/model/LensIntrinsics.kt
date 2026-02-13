package com.pasindu.woundlab.domain.model

/**
 * LensIntrinsics
 *
 * Defines the physical optical characteristics of the Redmi Note 14 5G sensors.
 * These values are extracted via Camera2 (CameraCharacteristics) in the Feature Layer.
 *
 * @param focalLengthPixels The lens focal length measured in pixels (fx, fy).
 * @param principalPoint The optical center of the sensor (cx, cy).
 * @param distortion Radial and Tangential distortion coefficients [k1, k2, k3, p1, p2].
 * Used for Brown-Conrady correction.
 */
data class LensIntrinsics(
    val focalLengthX: Float,
    val focalLengthY: Float,
    val principalPointX: Float,
    val principalPointY: Float,
    val distortion: FloatArray // [k1, k2, k3, p1, p2] from Camera2
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LensIntrinsics

        if (focalLengthX != other.focalLengthX) return false
        if (focalLengthY != other.focalLengthY) return false
        if (principalPointX != other.principalPointX) return false
        if (principalPointY != other.principalPointY) return false
        if (!distortion.contentEquals(other.distortion)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = focalLengthX.hashCode()
        result = 31 * result + focalLengthY.hashCode()
        result = 31 * result + principalPointX.hashCode()
        result = 31 * result + principalPointY.hashCode()
        result = 31 * result + distortion.contentHashCode()
        return result
    }
}