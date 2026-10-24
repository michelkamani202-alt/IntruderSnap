package com.michel.intrudersnap.capture

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

/**
 * Capture silencieuse d'une seule photo via la caméra frontale, sans
 * aperçu visible (aucune Preview n'est liée). Utilisé uniquement au
 * déclenchement du seuil d'échecs de code PIN configuré par l'utilisateur.
 */
object CameraCapture {

    private const val TAG = "CameraCapture"

    fun capture(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        outputFile: File,
        onResult: (success: Boolean, file: File?) -> Unit
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get()
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageCapture
                )

                val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            cameraProvider.unbindAll()
                            onResult(true, outputFile)
                        }

                        override fun onError(exc: ImageCaptureException) {
                            Log.e(TAG, "Échec de capture", exc)
                            cameraProvider.unbindAll()
                            onResult(false, null)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Impossible d'initialiser la caméra", e)
                onResult(false, null)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
