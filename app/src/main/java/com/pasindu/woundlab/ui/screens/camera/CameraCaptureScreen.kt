package com.pasindu.woundlab.ui.screens.camera

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * CameraCaptureScreen
 *
 * The primary viewfinder.
 *
 * Features:
 * 1. Zero-Cost Leveling Guide: Visual feedback based on ViewModel sensor data.
 * 2. Hardware Status: Shows if the 108MP Stereophotogrammetry array is active.
 * 3. Permission Handling: Requests CAMERA access.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraCaptureScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- Permission Check ---
    if (!cameraPermissionState.status.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Text("Camera permission is required for WoundLab Optical Engine.")
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
        return
    }

    // --- Lifecycle Management for Sensors ---
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
        }
    }

    // --- UI Layout ---
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Placeholder for Camera Preview Surface
        // (Camera2 SurfaceView will be injected here in the next step)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .border(2.dp, Color.DarkGray)
        ) {
            Text(
                text = "Camera Preview Area",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // --- Heads-Up Display (HUD) ---

        // 1. Leveling Indicator (The "Bubble Level")
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(100.dp)
                .border(
                    width = 4.dp,
                    color = if (uiState.isDeviceLevel) Color.Green else Color.Red,
                    shape = CircleShape
                )
        ) {
            // Crosshair
            Box(Modifier.align(Alignment.Center).size(10.dp).background(Color.White, CircleShape))
        }

        // 2. Telemetry Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            Text(
                text = "OPTICAL ENGINE: ${if (uiState.isStereoReady) "ACTIVE (108MP+8MP)" else "SEARCHING..."}",
                color = if (uiState.isStereoReady) Color.Green else Color.Yellow,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "PITCH: ${"%.1f".format(uiState.pitch)}°",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "ROLL: ${"%.1f".format(uiState.roll)}°",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // 3. Status Message
        Text(
            text = uiState.message,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
        )
    }
}