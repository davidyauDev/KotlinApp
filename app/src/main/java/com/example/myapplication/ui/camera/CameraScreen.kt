package com.example.myapplication.ui.camera

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import com.example.myapplication.data.local.AttendanceDatabase
import com.example.myapplication.data.local.AttendanceType
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.AttendanceRepository
import com.example.myapplication.ui.Attendance.AttendanceViewModel
import com.example.myapplication.ui.home.saveBitmapToFile
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.camera.core.Preview
import androidx.camera.core.ImageCapture
import androidx.camera.core.CameraSelector
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.asImageBitmap
import com.example.myapplication.ui.home.awaitLocationForAttendance

@androidx.camera.core.ExperimentalGetImage
@Composable
fun CameraScreen(
    navController: NavController,
    attendanceType: AttendanceType,
    attendanceViewModel: AttendanceViewModel
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
    AttendanceCameraView(
        onCaptureImage = { bitmap ->
            val uri = saveBitmapToFile(context, bitmap)
            Log.d("SELFIE", "Imagen guardada: $uri")
            coroutineScope.launch {
                isLoading = true
                try {
                    // Obtener ubicación usando la versión suspendible con timeout (10s primer intento)
                    var loc = awaitLocationForAttendance(fusedLocationClient, 10000L)
                    if (loc == null) {
                        Log.d("CameraScreen", "Ubicación no obtenida en primer intento; reintentando...")
                        // reintento rápido
                        delay(1000L)
                        loc = awaitLocationForAttendance(fusedLocationClient, 5000L)
                    }

                    if (loc != null) {
                        val result = attendanceRepository.saveAttendance(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            type = attendanceType,
                            photo = bitmap
                        )

                        if (result.isSuccess) {
                            Toast.makeText(context, "🎉 Asistencia registrada con éxito", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            val error = result.exceptionOrNull()
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
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            }, executor)
        }

        androidx.compose.material3.IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                contentDescription = "Cerrar cámara",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Button(
            onClick = {
                val bitmap = previewView.bitmap
                if (bitmap != null) {
                    capturedBitmap = bitmap
                } else {
                    Toast.makeText(context, "❌ No se pudo capturar la imagen", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            Text("Tomar Foto")
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