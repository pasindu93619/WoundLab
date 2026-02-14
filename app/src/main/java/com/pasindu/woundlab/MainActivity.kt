package com.pasindu.woundlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pasindu.woundlab.ui.screens.camera.CameraCaptureScreen
import com.pasindu.woundlab.ui.theme.WoundLabTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity
 *
 * The single-activity container for the WoundLab Zero-Cost platform.
 *
 * Responsibilities:
 * 1. Hosts the Jetpack Compose Navigation Graph.
 * 2. Acts as the Hilt Injection Root.
 * 3. Configures Edge-to-Edge display for immersive Optical Capture.
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
                    val navController = rememberNavController()

                    // --- Navigation Graph ---
                    NavHost(
                        navController = navController,
                        startDestination = "camera_capture"
                    ) {
                        // Phase 1: Direct entry to Optical Engine
                        composable("camera_capture") {
                            CameraCaptureScreen()
                        }

                        // Future Phase: FHIR Patient List
                        // composable("patient_list") { PatientListScreen(...) }

                        // Future Phase: Analysis Dashboard
                        // composable("result_analysis") { ResultAnalysisScreen(...) }
                    }
                }
            }
        }
    }
}