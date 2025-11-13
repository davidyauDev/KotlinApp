package com.example.myapplication.ui.camera

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.myapplication.data.local.database.AttendanceDatabase
import com.example.myapplication.data.local.entity.AttendanceType
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.AttendanceRepository
import com.example.myapplication.ui.home.saveBitmapToFile
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.camera.core.Preview
import androidx.camera.core.ImageCapture
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.asImageBitmap
import com.example.myapplication.ui.home.awaitLocationForAttendanceImproved
import com.example.myapplication.ui.home.LocationResult
import com.example.myapplication.data.local.database.LocationDatabase

// ML Kit imports
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

@androidx.camera.core.ExperimentalGetImage
@Composable
fun CameraScreen(
    navController: NavController,
    attendanceType: AttendanceType
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val attendanceDao = remember { AttendanceDatabase.getDatabase(context).attendanceDao() }
    val attendanceRepository = remember {
        AttendanceRepository(
            userPreferences = UserPreferences(context),
            context = context,
            dao = attendanceDao
        )
    }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationDao = remember { LocationDatabase.getDatabase(context).locationDao() }
    AttendanceCameraView(
        onCaptureImage = { bitmap ->
            val uri = saveBitmapToFile(context, bitmap)
            Log.d("SELFIE", "Imagen guardada: $uri")
            coroutineScope.launch {
                isLoading = true
                try {
                    // Obtener ubicación usando la versión mejorada que devuelve LocationResult
                    var loc: android.location.Location? = null

                    var result = awaitLocationForAttendanceImproved(fusedLocationClient, context, locationDao, 10000L)
                    if (result is LocationResult.Success) {
                        loc = result.location
                    } else {
                        Log.d("CameraScreen", "Ubicación no obtenida en primer intento; reintentando... (reason=${(result as? LocationResult.Error)?.reason})")
                        // reintento rápido
                        delay(1000L)
                        result = awaitLocationForAttendanceImproved(fusedLocationClient, context, locationDao, 5000L)
                        if (result is LocationResult.Success) {
                            loc = result.location
                        }
                    }

                    if (loc != null) {
                        val saveResult = attendanceRepository.saveAttendance(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            type = attendanceType,
                            photo = bitmap
                        )

                        if (saveResult.isSuccess) {
                            Toast.makeText(context, "🎉 Asistencia registrada con éxito", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            val error = saveResult.exceptionOrNull()
                            Log.e("AttendanceError", "Error al registrar asistencia", error)
                            Toast.makeText(
                                context,
                                "Error al registrar asistencia: ${error?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Log.w("CameraScreen", "Ubicación no disponible después de reintentos")
                        Toast.makeText(context, "Ubicación no disponible al momento de la captura.", Toast.LENGTH_SHORT).show()
                    }
                } catch (ex: Exception) {
                    Log.e("CameraScreen", "Excepción al obtener ubicación o guardar asistencia", ex)
                    Toast.makeText(context, "Error inesperado: ${ex.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        },
        onClose = {
            navController.popBackStack()
        },
        isLoading = isLoading
    )
}


@androidx.camera.core.ExperimentalGetImage
@Composable
fun AttendanceCameraView(
    onCaptureImage: (Bitmap) -> Unit,
    onClose: () -> Unit,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { androidx.camera.view.PreviewView(context) }
    val executor = ContextCompat.getMainExecutor(context)

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessingFace by remember { mutableStateOf(false) }
    var faceDetected by remember { mutableStateOf(false) }

    // Estabilización: requerir N frames consecutivos para confirmar detección
    var consecutiveDetectedFrames by remember { mutableStateOf(0) }
    var consecutiveLostFrames by remember { mutableStateOf(0) }
    val requiredStableFrames = 2 // menos estricto: 2 frames estables
    val lostToleranceFrames = 3   // tolerar más pequeñas pérdidas

    // Configuración más precisa pero tolerante de ML Kit
    val detectorOptions = remember {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.05f) // menos estricto: aceptar rostros más pequeños en preview
            .build()
    }
    val faceDetector: FaceDetector = remember { FaceDetection.getClient(detectorOptions) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                previewView
            },
            modifier = Modifier.fillMaxSize()
        ) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // ImageAnalysis para detección continua de rostros
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val rotation = imageProxy.imageInfo.rotationDegrees
                        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

                        // Procesar con ML Kit
                        faceDetector.process(inputImage)
                            .addOnSuccessListener { faces: List<Face> ->
                                if (faces.isNotEmpty()) {
                                    val mainFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

                                    // Calcular proporciones relativas
                                    val frameWidth = imageProxy.width.toFloat()
                                    val frameHeight = imageProxy.height.toFloat()
                                    val faceWidth = mainFace?.boundingBox?.width()?.toFloat() ?: 0f
                                    val faceHeight = mainFace?.boundingBox?.height()?.toFloat() ?: 0f

                                    val faceWidthRatio = if (frameWidth > 0) faceWidth / frameWidth else 0f
                                    val faceHeightRatio = if (frameHeight > 0) faceHeight / frameHeight else 0f

                                    // Márgenes: requerir que el bounding box esté completamente dentro del frame con un pequeño margen
                                    val marginX = (frameWidth * 0.04f) // 4% margen horizontal (menos estricto)
                                    val marginY = (frameHeight * 0.04f) // 4% margen vertical
                                    val box = mainFace?.boundingBox
                                    val insideHorizontally = box != null && box.left >= marginX && box.right <= (frameWidth - marginX)
                                    val insideVertically = box != null && box.top >= marginY && box.bottom <= (frameHeight - marginY)

                                    // Umbrales menos estrictos: aceptar rostros que ocupen ~9% del ancho/alto
                                    val widthOk = faceWidthRatio >= 0.09f
                                    val heightOk = faceHeightRatio >= 0.09f

                                    val detectedNow = (widthOk && heightOk && insideHorizontally && insideVertically)

                                    if (detectedNow) {
                                        consecutiveDetectedFrames += 1
                                        consecutiveLostFrames = 0
                                        if (consecutiveDetectedFrames >= requiredStableFrames) {
                                            if (!faceDetected) faceDetected = true
                                        }
                                    } else {
                                        consecutiveDetectedFrames = 0
                                        consecutiveLostFrames += 1
                                        if (consecutiveLostFrames >= lostToleranceFrames) {
                                            if (faceDetected) faceDetected = false
                                        }
                                    }

                                } else {
                                    consecutiveDetectedFrames = 0
                                    consecutiveLostFrames += 1
                                    if (consecutiveLostFrames >= lostToleranceFrames) {
                                        if (faceDetected) faceDetected = false
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("FaceDetect", "Error en análisis continuo", e)
                                consecutiveDetectedFrames = 0
                                consecutiveLostFrames += 1
                                if (consecutiveLostFrames >= lostToleranceFrames) {
                                    if (faceDetected) faceDetected = false
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            }, executor)
        }

        // Indicador de estado de detección en tiempo real
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .zIndex(5f)
        ) {
            val bgColor by animateColorAsState(targetValue = if (faceDetected) Color(0xFF0B8043) else Color(0xAA000000), animationSpec = tween(durationMillis = 280))
            val iconScale by animateFloatAsState(targetValue = if (faceDetected) 1.12f else 1f, animationSpec = tween(durationMillis = 280))

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = bgColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = if (faceDetected) Icons.Default.CheckCircle else Icons.Default.Face
                    val iconTint = Color.White
                    Icon(
                        imageVector = icon,
                        contentDescription = if (faceDetected) "Rostro detectado" else "Buscando rostro",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp).scale(iconScale)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (faceDetected) "Listo para capturar" else "Ajusta tu rostro",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (faceDetected) "Pulsa 'Tomar Foto' para confirmar" else "Colócate dentro del marco",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar cámara",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Ghost-frame (marco guía) para ayudar al usuario a posicionar el rostro
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.62f)
                .aspectRatio(0.8f)
                .zIndex(4f)
                .background(Color.Transparent)
                .border(width = 2.dp, color = Color.White.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        ) {
            // guía simple, no dinámico
        }



        // Mejorar estilo del botón: colores más claros y visual de deshabilitado
        val captureEnabled = faceDetected && !isLoading && !isProcessingFace

        Button(
            onClick = {
                val bitmap = previewView.bitmap
                if (bitmap != null) {
                    // Ya validamos en tiempo real; simplemente guardar la imagen para vista previa
                    capturedBitmap = bitmap
                } else {
                    Toast.makeText(context, "❌ No se pudo capturar la imagen", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = captureEnabled,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = if (captureEnabled) Color(0xFF0B8043) else Color(0xFF555555)
            )
        ) {
            Text("Tomar Foto")
        }

        // Indicador cuando ML Kit está procesando la detección facial puntual (si se usa)
        if (isProcessingFace) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
                    .zIndex(4f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .zIndex(2f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (capturedBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .zIndex(3f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Vista previa",
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp), // moved buttons a bit higher
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(onClick = { capturedBitmap = null }) {
                        Text("Cancelar")
                    }
                    Button(onClick = {
                        onCaptureImage(capturedBitmap!!)
                        capturedBitmap = null
                    }) {
                        Text("Aceptar")
                    }
                }
            }
        }
    }
}
