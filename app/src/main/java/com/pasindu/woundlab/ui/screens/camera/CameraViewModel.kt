package com.pasindu.woundlab.ui.screens.camera

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.woundlab.data.local.dao.TelemetryDao
import com.pasindu.woundlab.data.local.entity.TelemetryLog
import com.pasindu.woundlab.feature.optical.OpticalAcquisitionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * CameraViewModel
 *
 * The brain of the Capture Screen.
 *
 * Responsibilities:
 * 1. REAL-TIME LEVELING: Calculates Pitch/Roll from raw sensor data to guide the user.
 * 2. TELEMETRY LOGGING: Saves sensor stats to Room to validate clinical accuracy.
 * 3. OPTICAL BINDING: Checks if the Redmi Note 14 5G hardware is ready.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application,
    private val telemetryDao: TelemetryDao,
    private val opticalManager: OpticalAcquisitionManager
) : AndroidViewModel(application), SensorEventListener {

    // --- State Management ---
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    // --- Sensors ---
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    // Session ID for this specific capture attempt
    private val currentSessionId = UUID.randomUUID().toString()

    init {
        checkOpticalHardware()
    }

    private fun checkOpticalHardware() {
        viewModelScope.launch(Dispatchers.IO) {
            val lensPair = opticalManager.findStereoCalibration()
            if (lensPair != null) {
                _uiState.value = _uiState.value.copy(
                    isStereoReady = true,
                    message = "Stereo Array Bound: 108MP + 8MP Ready"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isStereoReady = false,
                    message = "WARNING: Stereo sensors not found. Metrics may be inaccurate."
                )
            }
        }
    }

    fun startSensors() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopSensors() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values
        }
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values
        }

        if (gravity != null && geomagnetic != null) {
            val r = FloatArray(9)
            val i = FloatArray(9)

            if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)

                // Convert radians to degrees
                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

                // "Level" is defined as Pitch & Roll being close to 0 (Phone flat)
                // or Pitch ~90 (Phone upright). For wound photography, we usually want
                // the phone parallel to the gravity vector of the wound.
                // Here we assume "Top Down" photo: Pitch/Roll should be near 0.
                val isLevel = abs(pitch) < 5 && abs(roll) < 5

                _uiState.value = _uiState.value.copy(
                    pitch = pitch,
                    roll = roll,
                    isDeviceLevel = isLevel
                )

                // Log high-frequency telemetry for post-capture validation
                // We sample heavily; in production, you might throttle this.
                logTelemetry(pitch, roll, azimuth)
            }
        }
    }

    private fun logTelemetry(pitch: Float, roll: Float, azimuth: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            telemetryDao.insertLog(
                TelemetryLog(
                    timestamp = System.currentTimeMillis(),
                    sessionId = currentSessionId,
                    pitch = pitch,
                    roll = roll,
                    azimuth = azimuth,
                    luminosity = 0f // Placeholder, requires light sensor
                )
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

/**
 * UI State for the Camera Screen
 */
data class CameraUiState(
    val isStereoReady: Boolean = false,
    val isDeviceLevel: Boolean = false,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val message: String = "Initializing..."
)