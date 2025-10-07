package com.example.myapplication.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.local.AttendanceDatabase
import com.example.myapplication.data.local.AttendanceType
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.AttendanceRepository
import com.example.myapplication.ui.Attendance.AttendanceViewModel
import com.example.myapplication.ui.camera.CameraWithFaceDetection
import com.example.myapplication.ui.user.UserViewModel
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.zIndex
@ExperimentalGetImage
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val attendanceDao = remember {
        AttendanceDatabase.getDatabase(context).attendanceDao()
    }
    val attendanceRepository = remember {
        AttendanceRepository(
            userPreferences = UserPreferences(context),
            context = context,
            dao = attendanceDao
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val snackbarHostState = remember { SnackbarHostState() }
    var showCamera by remember { mutableStateOf(false) }
    var photoLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val attendanceViewModel: AttendanceViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()

    val currentTime = remember { mutableStateOf("") }
    val currentDate = remember { mutableStateOf("") }
    var currentAttendanceType by remember { mutableStateOf(AttendanceType.ENTRADA) }
    val userName by userViewModel.userName.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        currentDate.value = SimpleDateFormat("EEEE dd, MMM yyyy", Locale("es")).format(Date())
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] == true
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true

        if (cameraGranted && locationGranted) {
            if (isLocationEnabled(context)) {
                showCamera = true
                coroutineScope.launch {
                    getLastKnownLocation( fusedLocationClient) { location ->
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!showCamera) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        BlueHeaderWithName(
                            userName = userName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .zIndex(1f)
                        )


                        // Usamos Box u otro contenedor si necesitas solapamiento
                        Box(modifier = Modifier.fillMaxSize()) {
                            RoundedTopContainer {
                                BannerCarousel()

                                Spacer(modifier = Modifier.height(16.dp))
                                LastMarkText(viewModel = attendanceViewModel)

                                Spacer(modifier = Modifier.height(24.dp))
                                EntryExitButtons(
                                    onEntry = {
                                        currentAttendanceType = AttendanceType.ENTRADA
                                        cameraPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.CAMERA,
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    onExit = {
                                        currentAttendanceType = AttendanceType.SALIDA
                                        cameraPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.CAMERA,
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    onLogout = {
                                        Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                                        navController.navigate("login") { popUpTo("home") { inclusive = true } }
                                        userViewModel.clearUser()
                                    }
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }



            }
            else {
                CameraWithFaceDetection (
                    onFaceDetected = { Log.d("Face", "Rostro detectado") },
                    onCaptureImage = { bitmap ->
                        val uri = saveBitmapToFile(context, bitmap)
                        Log.d("SELFIE", "Imagen guardada: $uri")

                        photoLocation?.let { location ->
                            coroutineScope.launch {
                                isLoading = true //
                                val result = attendanceRepository.saveAttendance(
                                    latitude = location.first,
                                    longitude = location.second,
                                    type = currentAttendanceType,
                                    photo = bitmap
                                )
                                isLoading = false //

                                if (result.isSuccess) {
                                    Log.d("isSuccess", "EXITOSO")
                                    showCamera = false
                                    snackbarHostState.showSnackbar("🎉 Asistencia registrada con éxito")
                                } else {
                                    val error = result.exceptionOrNull()
                                    Log.e("AttendanceError", " Error al registrar asistencia", error)
                                    Toast.makeText(
                                        context,
                                        "Error al registrar asistencia: ${error?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } ?: Toast.makeText(context, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
                    }
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
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
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (Location?) -> Unit
) {
    try {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L // interval in milliseconds
        )
            .setMinUpdateIntervalMillis(500L)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                onLocationReceived(result.lastLocation)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
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


