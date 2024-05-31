package com.example.camera_app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camera_app.presentation.screens.CameraScreen
import com.example.camera_app.ui.theme.CameraAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        val CAMERA_PERMISSION = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!arePermissionGranted()) {
            ActivityCompat.requestPermissions(
                this,
                CAMERA_PERMISSION,
                100
            )
        }
        enableEdgeToEdge()
        setContent {
            CameraAppTheme {
                CameraScreen(this)
            }
        }
    }

    fun arePermissionGranted(): Boolean {
        return CAMERA_PERMISSION.all { permission ->

            ContextCompat.checkSelfPermission(
                applicationContext,
                permission
            ) == PackageManager.PERMISSION_GRANTED

        }
    }

    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    private fun PrevCameraScreen() {
        CameraScreen(this)
    }
}