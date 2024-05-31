package com.example.camera_app.domain.repository

import androidx.camera.view.LifecycleCameraController

interface CameraRepository {

    suspend fun takePicture(controller: LifecycleCameraController)

    suspend fun recordVideo(controller: LifecycleCameraController)

}