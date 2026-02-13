package com.pasindu.woundlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pasindu.woundlab.ui.screens.camera.CameraCaptureScreen
import com.pasindu.woundlab.ui.theme.WoundLabTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity
 *
 * The single-activity container for the WoundLab Zero-Cost platform.
 * * Responsibilities:
 * 1. Hosts the Jetpack Compose UI.
 * 2. Acts as the Hilt Injection Root for UI components.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Maximize screen real estate for the viewfinder

        setContent {
            // Enforce the Strict Light Mode Theme
            WoundLabTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Start directly at the Camera Capture Screen (Phase 1 Workflow)
                    CameraCaptureScreen()
                }
            }
        }
    }
}