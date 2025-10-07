package com.example.myapplication.ui.camera

import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.launch
import kotlin.math.abs

@androidx.camera.core.ExperimentalGetImage
@Composable
fun CameraWithFaceDetection(
    onFaceDetected: () -> Unit,
    onCaptureImage: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var faceDetected by remember { mutableStateOf(false) }

    val executor = ContextCompat.getMainExecutor(context)

    val faceDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build()
        )
    }

    // 🔹 Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // Snackbar host
        SnackbarHost(hostState = snackbarHostState)

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                faceDetector.process(image)
                                    .addOnSuccessListener { faces ->
                                        faceDetected = false
                                        if (faces.size == 1) {
                                            val face = faces.first()
                                            val boundingBox = face.boundingBox

                                            val faceWidth = boundingBox.width().toFloat()
                                            val faceHeight = boundingBox.height().toFloat()
                                            val previewWidth = previewView.width.toFloat()
                                            val previewHeight = previewView.height.toFloat()

                                            val widthRatio = faceWidth / previewWidth
                                            val heightRatio = faceHeight / previewHeight

                                            val isBigEnough = widthRatio > 0.25f && heightRatio > 0.12f
                                            val yaw = abs(face.headEulerAngleY)
                                            val pitch = abs(face.headEulerAngleX)
                                            val isFacingFront = yaw < 15 && pitch < 15

                                            faceDetected = isBigEnough && isFacingFront

                                            if (faceDetected) {
                                                onFaceDetected()
                                            }
                                        }
                                        imageProxy.close()
                                    }
                                    .addOnFailureListener {
                                        faceDetected = false
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            }, executor)
        }

        // Overlay del marco
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rectSize = size.minDimension * 0.6f

            drawRect(
                color = if (faceDetected) Color.Green else Color.Red,
                topLeft = Offset(
                    (size.width - rectSize) / 2f,
                    (size.height - rectSize) / 2f
                ),
                size = Size(rectSize, rectSize),
                style = Stroke(width = 6f)
            )
        }

        // Botón solo si hay rostro detectado
        if (faceDetected) {
            Button(
                onClick = {
                    val bitmap = previewView.bitmap
                    if (bitmap != null) {
                        onCaptureImage(bitmap)

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("✔️ Asistencia registrada con éxito")
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("Tomar Foto")
            }
        }
    }
}
