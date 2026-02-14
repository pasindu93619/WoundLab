package com.pasindu.woundlab.domain.math

import com.pasindu.woundlab.domain.model.LensIntrinsics
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * StereoMath
 *
 * The core mathematical engine for the Zero-Cost Optical Engine.
 * Implements the pinhole camera model and standard stereo disparity formulas.
 *
 * Responsibilities:
 * 1. DEPTH ESTIMATION: Calculates Z (depth) from disparity (d).
 * Formula: Z = (f * B) / d
 * Where:
 * f = Focal length in pixels (recovered from LensIntrinsics)
 * B = Baseline in millimeters (distance between Main and Ultra sensors)
 * d = Disparity in pixels (shift between feature points)
 *
 * 2. DISTORTION CORRECTION: Applies Brown-Conrady un-distortion to raw points
 * BEFORE calculating disparity. This is critical for the "Medical Grade" accuracy
 * requirement, as raw wide-angle lenses have significant barrel distortion.
 */
class StereoMath @Inject constructor() {

    /**
     * Calculates depth (Z) in millimeters.
     *
     * @param focalLengthPixels The focal length (fx) from the Main camera's intrinsics.
     * @param baselineMm The physical distance between the two camera sensors (approx 12mm-20mm on phones).
     * @param disparityPixels The calculated pixel shift for a matched feature point.
     * @return Depth Z in millimeters. Returns Float.MAX_VALUE if disparity is effectively zero (infinity).
     */
    fun calculateDepth(
        focalLengthPixels: Float,
        baselineMm: Float,
        disparityPixels: Float
    ): Float {
        if (disparityPixels < 0.1f) return Float.MAX_VALUE // Avoid divide-by-zero
        return (focalLengthPixels * baselineMm) / disparityPixels
    }

    /**
     * Undistorts a 2D point using the Brown-Conrady model.
     * Required because the Ultrawide lens introduces barrel distortion that ruins depth accuracy.
     *
     * @param x Raw X pixel coordinate.
     * @param y Raw Y pixel coordinate.
     * @param intrinsics The lens intrinsics containing distortion coefficients [k1, k2, k3, p1, p2].
     * @return Pair(correctedX, correctedY)
     */
    fun undistortPoint(
        x: Float,
        y: Float,
        intrinsics: LensIntrinsics
    ): Pair<Float, Float> {
        val distortion = intrinsics.distortion

        // If no distortion coefficients exist, return raw point (assume perfect lens)
        if (distortion == null || distortion.isEmpty()) {
            return Pair(x, y)
        }

        // Normalize coordinates (centered at principal point)
        val cx = intrinsics.principalPointX
        val cy = intrinsics.principalPointY
        val fx = intrinsics.focalLengthX
        val fy = intrinsics.focalLengthY

        var xNorm = (x - cx) / fx
        var yNorm = (y - cy) / fy

        // r^2 = x^2 + y^2
        val r2 = xNorm.pow(2) + yNorm.pow(2)
        val r4 = r2 * r2
        val r6 = r2 * r4

        // Extract coefficients (Standard Android Camera2 order: [k1, k2, k3, p1, p2])
        val k1 = distortion.getOrElse(0) { 0f }
        val k2 = distortion.getOrElse(1) { 0f }
        val k3 = distortion.getOrElse(2) { 0f }
        val p1 = distortion.getOrElse(3) { 0f }
        val p2 = distortion.getOrElse(4) { 0f }

        // Radial distortion factor: 1 + k1*r^2 + k2*r^4 + k3*r^6
        val radial = 1f + (k1 * r2) + (k2 * r4) + (k3 * r6)

        // Tangential distortion correction
        // x_corrected = x_norm * radial + 2*p1*x*y + p2*(r^2 + 2*x^2)
        // y_corrected = y_norm * radial + p1*(r^2 + 2*y^2) + 2*p2*x*y
        val xTangential = (2f * p1 * xNorm * yNorm) + (p2 * (r2 + 2f * xNorm.pow(2)))
        val yTangential = (p1 * (r2 + 2f * yNorm.pow(2))) + (2f * p2 * xNorm * yNorm)

        val xCorrectedNorm = (xNorm * radial) + xTangential
        val yCorrectedNorm = (yNorm * radial) + yTangential

        // Denormalize back to pixel coordinates
        val xFinal = (xCorrectedNorm * fx) + cx
        val yFinal = (yCorrectedNorm * fy) + cy

        return Pair(xFinal, yFinal)
    }

    /**
     * Calculates the surface area of a wound mask in mm².
     *
     * @param maskPixelCount Total number of pixels in the wound mask.
     * @param depthMm The average depth of the wound in millimeters.
     * @param focalLengthPixels Focal length of the camera in pixels.
     * @return Area in square millimeters.
     */
    fun calculateSurfaceArea(
        maskPixelCount: Int,
        depthMm: Float,
        focalLengthPixels: Float
    ): Float {
        // Pixel scale (mm per pixel) = Depth / FocalLength
        // This is derived from similar triangles: X_mm / Z_mm = x_px / f_px
        // => X_mm = x_px * (Z / f)
        // Area scale factor = (Z / f)^2

        val scaleFactor = depthMm / focalLengthPixels
        val pixelAreaMm2 = scaleFactor * scaleFactor

        return maskPixelCount * pixelAreaMm2
    }
}