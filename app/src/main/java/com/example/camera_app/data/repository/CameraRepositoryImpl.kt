package com.example.camera_app.data.repository

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import com.example.camera_app.R
import com.example.camera_app.domain.repository.CameraRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class CameraRepositoryImpl @Inject constructor(
    private val application: Application
) : CameraRepository {

    private var recording: Recording? = null

    override suspend fun takePicture(controller: LifecycleCameraController) {
        controller.takePicture(

            // Call back for taking picture :->
            ContextCompat.getMainExecutor(application),
            object : ImageCapture.OnImageCapturedCallback() {

                // In this we have two methods for taking picture :->
                // First is on Capture Success :->
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    // For image rotation we needed matrix :->
                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }

                    val imageBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0, 0,
                        image.width, image.height,
                        matrix, true
                    )

                    // Saving image to gallery :->
                    CoroutineScope(Dispatchers.IO).launch {
                        saveImageToGallery(imageBitmap)
                    }
                }

                // Second is on Capture Failed :->
                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                }
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun recordVideo(controller: LifecycleCameraController) {

        if (recording != null) {
            recording?.stop()
            recording = null
            return
        }

        val timeInMillis = System.currentTimeMillis()
        val file = File(
            application.filesDir,
            "${timeInMillis}_video" + ".mp4"
        )

        recording = controller.startRecording(
            FileOutputOptions.Builder(file).build(),
            AudioConfig.create(true),
            ContextCompat.getMainExecutor(application)
        ) { event ->
            if (event is VideoRecordEvent.Finalize){
                if (!event.hasError()){
                    val msg = "Video capture succeeded: ${file.absolutePath}"
                    recording?.close()
                    recording = null
                }else {
                    CoroutineScope(Dispatchers.IO).launch {
                        saveVideoToGallery(file)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveImageToGallery(bitmap: Bitmap) {
        withContext(Dispatchers.IO){
            // Content resolver :->
            val resolver: ContentResolver = application.contentResolver

            val imageCollection = MediaStore.Images.Media.getContentUri(

                // Where you want to save the image :->
                MediaStore.VOLUME_EXTERNAL_PRIMARY  // External storage.

            )

            // Getting app name :->
            val appName = application.getString(R.string.app_name)

            // Checking when the image is saved or not :->
            val timeInMillis = System.currentTimeMillis()

            // Going to put some data to image :->
            val imageDetails = ContentValues().apply {

                // Path were the image will be saved.
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/$appName")

                // Other Details :->
                put(MediaStore.Images.Media.DISPLAY_NAME, "${timeInMillis}_image" + ".jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_TAKEN, timeInMillis)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            // Getting uri from image content resolver in our image collection :->
            val imageMediaStoreUri = resolver.insert(imageCollection, imageDetails)
            imageMediaStoreUri?.let { uri ->
                try {

                    // Saving the image :->
                    resolver.openOutputStream(uri)?.let { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }

                    // Updating the image details :->
                    imageDetails.clear()
                    imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, imageDetails, null, null)

                } catch (e: Exception){
                    e.printStackTrace()
                    resolver.delete(uri, null, null)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveVideoToGallery(file: File) {
        withContext(Dispatchers.IO){
            // Content resolver :->
            val resolver: ContentResolver = application.contentResolver

            val videoCollection = MediaStore.Video.Media.getContentUri(

                // Where you want to save the video :->
                MediaStore.VOLUME_EXTERNAL_PRIMARY  // External storage.

            )

            // Getting app name :->
            val appName = application.getString(R.string.app_name)

            // Checking when the video is saved or not :->
            val timeInMillis = System.currentTimeMillis()

            // Going to put some data to video :->
            val videoDetails = ContentValues().apply {

                // Path were the video will be saved.
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/$appName")

                // Other Details :->
                put(MediaStore.Video.Media.DISPLAY_NAME, "${timeInMillis}_video")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATE_ADDED, timeInMillis)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            // Getting uri from video content resolver in our video collection :->
            val videoMediaStoreUri = resolver.insert(videoCollection, videoDetails)
            videoMediaStoreUri?.let { uri ->
                try {

                    // Saving the video :->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        // Reading the input stream and copying it to output stream :->
                        resolver.openInputStream(
                            Uri.fromFile(file)
                        ).use { inputStream ->
                            inputStream?.copyTo(outputStream)
                        }
                    }

                    // Updating the video details :->
                    videoDetails.clear()
                    videoDetails.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, videoDetails, null, null)

                } catch (e: Exception){
                    e.printStackTrace()
                    resolver.delete(uri, null, null)
                }
            }
        }
    }
}