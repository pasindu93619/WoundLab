package com.pasindu.woundlab.domain.math

import com.pasindu.woundlab.domain.model.LensIntrinsics
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * StereoMath
 *
 * The pure mathematical engine for the "Zero-Cost" Optical Layer.
 *
 * Responsibilities:
 * 1. Calculate Depth (Z) using the standard stereo triangulation formula.
 * 2. Correct Lens Distortion using the Brown-Conrady model (Essential for accurate measurements).
 *
 * Hardware Note:
 * The Redmi Note 14 5G lacks a Time-of-Flight (ToF) sensor. We rely strictly on
 * raw disparity maps generated between the Main and Ultrawide lenses.
 */
@Singleton
class StereoMath @Inject constructor() {

    /**
     * Calculates depth (Z) in millimeters.
     *
     * Formula: Z = (f * B) / d
     *
     * @param focalLengthPixels The focal length of the primary camera in pixels.
     * @param baselineMm The physical distance between the two camera lenses (approx 15mm-25mm on most phones).
     * @param disparityPixels The pixel shift of a feature point between the left and right images.
     * @return Depth Z in millimeters. Returns 0f if disparity is too low (infinity).
     */
    fun calculateDepth(focalLengthPixels: Float, baselineMm: Float, disparityPixels: Float): Float {
        if (disparityPixels < 0.1f) return 0f // Avoid divide by zero (Point is at infinity)
        return (focalLengthPixels * baselineMm) / disparityPixels
    }

    /**
     * Applies Brown-Conrady distortion correction to a raw 2D point.
     *
     * Since the distortion model maps (undistorted -> distorted), finding the undistorted
     * point requires an iterative approach (Newton-Raphson).
     *
     * @param x Raw X pixel coordinate from sensor.
     * @param y Raw Y pixel coordinate from sensor.
     * @param intrinsics The camera's physical calibration data.
     * @return Pair(Corrected X, Corrected Y)
     */
    fun undistortPoint(x: Float, y: Float, intrinsics: LensIntrinsics): Pair<Float, Float> {
        // Normalize coordinates relative to principal point
        val normX = (x - intrinsics.principalPointX) / intrinsics.focalLengthX
        val normY = (y - intrinsics.principalPointY) / intrinsics.focalLengthY

        var xU = normX
        var yU = normY

        // 5 Iterations of Newton-Raphson are usually sufficient for < 0.1px error
        for (i in 0 until 5) {
            val r2 = xU * xU + yU * yU
            val r4 = r2 * r2
            val r6 = r4 * r2

            // Radial distortion factors
            val k1 = intrinsics.distortion.getOrElse(0) { 0f }
            val k2 = intrinsics.distortion.getOrElse(1) { 0f }
            val k3 = intrinsics.distortion.getOrElse(2) { 0f }

            // Tangential distortion factors
            val p1 = intrinsics.distortion.getOrElse(3) { 0f }
            val p2 = intrinsics.distortion.getOrElse(4) { 0f }

            val radial = 1 + k1 * r2 + k2 * r4 + k3 * r6

            // Delta calculation (Forward projection)
            val deltaX = 2 * p1 * xU * yU + p2 * (r2 + 2 * xU * xU)
            val deltaY = p1 * (r2 + 2 * yU * yU) + 2 * p2 * xU * yU

            val xDistorted = xU * radial + deltaX
            val yDistorted = yU * radial + deltaY

            // Error (Residual)
            val errorX = xDistorted - normX
            val errorY = yDistorted - normY

            // Apply simplistic gradient descent step (approximate inverse)
            // Full Jacobian matrix is expensive; this approximation works for typical lens curves.
            xU -= errorX
            yU -= errorY
        }

        // Denormalize back to pixel coordinates
        val finalX = xU * intrinsics.focalLengthX + intrinsics.principalPointX
        val finalY = yU * intrinsics.focalLengthY + intrinsics.principalPointY

        return Pair(finalX, finalY)
    }

    /**
     * Calculates the surface area of a pixel at a specific depth.
     * This is critical for the "Wound Area" calculation.
     *
     * @param depthMm Distance to the wound surface.
     * @param focalLengthPixels Camera focal length.
     * @return Area of one pixel in mm^2 at that depth.
     */
    fun calculatePixelAreaAtDepth(depthMm: Float, focalLengthPixels: Float): Float {
        // GSD (Ground Sample Distance) = (Sensor Size * Depth) / (Focal Length * Image Width)
        // Simplified: Pixel Scale = Depth / Focal Length
        val pixelScale = depthMm / focalLengthPixels
        return pixelScale * pixelScale
    }
}