package com.pasindu.woundlab.ui.screens.camera

import android.Manifest
import android.view.SurfaceHolder
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.pasindu.woundlab.ui.components.AutoFitSurfaceView
import timber.log.Timber

/**
 * CameraCaptureScreen
 *
 * The primary viewfinder.
 *
 * Responsibilities:
 * 1. VIEWFINDER: Renders the Camera2 stream via AutoFitSurfaceView.
 * 2. ZERO-COST LEVELING GUIDE: Visual feedback based on ViewModel sensor data.
 * 3. HARDWARE STATUS: Shows if the 108MP Stereophotogrammetry array is active.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraCaptureScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    // 1. Permission State
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // 2. ViewModel State
    // FIXED: Use collectAsStateWithLifecycle() for safe StateFlow collection
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- Permission Check Logic ---
    if (!cameraPermissionState.status.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Text(
                text = "Camera permission is required for WoundLab Optical Engine.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
        return // Stop rendering the rest until permission is granted
    }

    // --- Lifecycle Management ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.startSensors()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.stopSensors()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopSensors()
        }
    }

    // --- Main UI Layout ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // 1. Camera Preview (Physical Surface Binding)
        AndroidView(
            factory = { ctx ->
                AutoFitSurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            Timber.d("Preview Surface Created")
                            viewModel.setMainSurface(holder.surface)
                        }

                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                            // No-op
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            Timber.d("Preview Surface Destroyed")
                            viewModel.clearMainSurface()
                        }
                    })
                    setAspectRatio(3, 4)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Heads-Up Display (HUD) - Leveling Guide
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(120.dp)
                .border(
                    width = 4.dp,
                    color = if (uiState.isDeviceLevel) Color.Green else Color.Red,
                    shape = CircleShape
                )
        ) {
            // Center Crosshair
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(8.dp)
                    .background(Color.White, CircleShape)
            )
        }

        // 3. Telemetry Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp)
        ) {
            Text(
                text = "OPTICAL ENGINE",
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = if (uiState.isStereoReady) "ACTIVE (108MP+8MP)" else "SEARCHING...",
                color = if (uiState.isStereoReady) Color.Green else Color.Yellow,
                style = MaterialTheme.typography.bodySmall
            )

            // Spacer
            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "IMU TELEMETRY",
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "PITCH: ${"%.1f".format(uiState.pitch)}°",
                color = if (uiState.isDeviceLevel) Color.Green else Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "ROLL: ${"%.1f".format(uiState.roll)}°",
                color = if (uiState.isDeviceLevel) Color.Green else Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // 4. Status Message
        Text(
            text = uiState.message,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // 5. Capture Button
        Button(
            onClick = { viewModel.captureStereoFrame() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Text("CAPTURE 3D")
        }
    }
}