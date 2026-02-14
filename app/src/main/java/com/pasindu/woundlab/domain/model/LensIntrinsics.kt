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
 */
data class LensIntrinsics(
    val focalLengthX: Float,
    val focalLengthY: Float,
    val principalPointX: Float,
    val principalPointY: Float
)