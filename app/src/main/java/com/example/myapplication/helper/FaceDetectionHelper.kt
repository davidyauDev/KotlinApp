package com.example.myapplication.data.camera

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs

class FaceDetectionHelper(
    private val previewWidth: () -> Float,
    private val previewHeight: () -> Float,
    private val onFaceValid: (Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .enableTracking()
            .build()
    )

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->
                var valid = false
                if (faces.size == 1) {
                    val face = faces.first()
                    val box = face.boundingBox

                    val widthRatio = box.width().toFloat() / previewWidth()
                    val heightRatio = box.height().toFloat() / previewHeight()

                    val isBigEnough = widthRatio > 0.25f && heightRatio > 0.12f
                    val yaw = abs(face.headEulerAngleY)
                    val pitch = abs(face.headEulerAngleX)
                    val isFacingFront = yaw < 15 && pitch < 15

                    valid = isBigEnough && isFacingFront
                }
                onFaceValid(valid)
                imageProxy.close()
            }
            .addOnFailureListener {
                onFaceValid(false)
                imageProxy.close()
            }
    }
}

