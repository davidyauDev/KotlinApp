// HomeScreen.kt
package com.example.myapplication.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.android.gms.location.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val snackbarHostState = remember { SnackbarHostState() }
    var showSnackbar by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var photoLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val currentTime = remember { mutableStateOf("") }
    val currentDate = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        currentDate.value = SimpleDateFormat("EEEE dd, MMM yyyy", Locale("es")).format(Date())
    }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar("✔️ Registro exitoso con selfie y ubicación")
            showSnackbar = false
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        if (cameraGranted && locationGranted) {
            if (isLocationEnabled(context)) {
                showCamera = true
                coroutineScope.launch {
                    getLastKnownLocation(context, fusedLocationClient) { location ->
                        photoLocation = location?.let { Pair(it.latitude, it.longitude) }
                        Log.d("LOC", "Ubicación recibida en background")
                    }
                }
            } else {
                Toast.makeText(context, "GPS desactivado. Actívalo para usar la ubicación.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Permisos denegados", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(hostState = snackbarHostState)

        if (!showCamera) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 🟦 Header azul con reloj y nombre
                ClockHeader(
                    name = "Valentina",
                    time = currentTime.value,
                    date = currentDate.value.replaceFirstChar { it.uppercaseChar() }
                )

                // ⬜ Parte blanca con resto de contenido
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    ShiftCard(shiftText = "No Planificado")

                    LastMarkText(lastMark = "11:27 (dic/13)")

                    Spacer(modifier = Modifier.height(24.dp))

                    EntryExitButtons(
                        onEntry = {
                            cameraPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onExit = {
                            // Acción de salida
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } else {
            CameraWithFaceDetection(
                onFaceDetected = { Log.d("Face", "Rostro detectado") },
                onCaptureImage = { bitmap ->
                    val uri = saveBitmapToFile(context, bitmap)
                    Log.d("SELFIE", "Imagen guardada: $uri")

                    photoLocation?.let {
                        Log.d("LOCATION", "Lat: ${it.first}, Lon: ${it.second}")
                    } ?: Toast.makeText(context, "Ubicación aún no disponible", Toast.LENGTH_SHORT).show()

                    showCamera = false
                    showSnackbar = true
                }
            )
        }
    }
}


@Composable
fun LastMarkText(lastMark: String = "11:27 (dic/13)") {
    Text(
        text = "Última marca de salida: $lastMark",
        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    )
}


@Composable
fun ClockHeader(
    name: String = "Valentina",
    time: String,
    date: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.55f) // Ocupa más alto
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0051A8), // Azul oscuro arriba
                        Color(0xFF0051A8)  // Azul claro abajo
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Cechriza",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 25.sp)
            )

            Text(
                text = "Hola, $name",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = time,
                color = Color.White,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp)
            )

            Text(
                text = "H      M      S",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = date,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ShiftCard(shiftText: String = "No Planificado") {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Calendario",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text("Turno", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF1976D2)))
                Text(shiftText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun EntryExitButtons(
    onEntry: () -> Unit,
    onExit: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Button(
            onClick = onEntry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFB300), // Mostaza
                contentColor = Color.White
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ENTRADA")
        }

        OutlinedButton(
            onClick = onExit,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("SALIDA")
        }
    }
}


fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "JPEG_${timestamp}.jpg")
    file.outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        it.flush()
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun getLastKnownLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (android.location.Location?) -> Unit
) {
    try {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
            numUpdates = 1
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    onLocationReceived(result.lastLocation)
                }
            },
            Looper.getMainLooper()
        )
    } catch (e: SecurityException) {
        onLocationReceived(null)
    }
}

fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
}
