package com.example.myapplication.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import android.net.Uri
import android.os.Environment
import android.provider.Settings
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.local.entity.AttendanceType
import com.example.myapplication.ui.Attendance.AttendanceViewModel
import com.example.myapplication.ui.user.UserViewModel
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.zIndex
import android.content.pm.PackageManager
import android.app.AppOpsManager
import android.location.Location
import android.util.Log
import com.example.myapplication.data.local.database.LocationDatabase

// --- Utilities moved to top so HomeScreen can reference them reliably ---
@Suppress("unused")
fun safeDeleteFile(path: String?): Boolean {
    return try {
        if (path == null) return false
        val f = File(path)
        if (f.exists()) f.delete() else false
    } catch (_: Exception) { false }
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

fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
}

fun isLocationPossiblyMocked(location: Location): Boolean {
    return try {
        @Suppress("DEPRECATION")
        location.isFromMockProvider
    } catch (_: Exception) { false }
}

@Suppress("DEPRECATION")
fun tryGetMockLocationAppName(context: Context): String? {
    return try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        for (pkg in packages) {
            try {
                val appInfo = pkg.applicationInfo ?: continue
                val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, appInfo.uid, pkg.packageName)
                if (mode == AppOpsManager.MODE_ALLOWED) {
                    val label = appInfo.loadLabel(pm).toString()
                    return "$label (${pkg.packageName})"
                }
            } catch (_: Throwable) { /* ignore per-app failures */ }
        }
        null
    } catch (_: Exception) { null }
}

@ExperimentalGetImage
@Composable
fun HomeScreen(
    navController: NavController,
    attendanceViewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationDao = remember { LocationDatabase.getDatabase(context).locationDao() }

    var isCheckingPermissions by remember { mutableStateOf(false) }
    var isNavigatingToCamera by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val userViewModel: UserViewModel = viewModel()
    val userName by userViewModel.userName.collectAsState()

    val currentDate = remember { mutableStateOf("") }
    var currentAttendanceType by remember { mutableStateOf(AttendanceType.ENTRADA) }

    var showAppSettingsDialog by remember { mutableStateOf(false) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showEnableLocationDialog by remember { mutableStateOf(false) }
    var rationaleMessage by remember { mutableStateOf("") }
    var showMockLocationDialog by remember { mutableStateOf(false) }
    var mockLocationAppName by remember { mutableStateOf<String?>(null) }

    // NUEVOS: estados para mostrar un diálogo de error de ubicación asegurando visibilidad
    var showLocationErrorDialog by remember { mutableStateOf(false) }
    var locationErrorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        currentDate.value = SimpleDateFormat("EEEE dd, MMM yyyy", Locale("es")).format(Date())
    }

    fun openLocationSettings(ctx: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            ctx.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun hasCameraPermission(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationPermission(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun openAppSettings(ctx: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", ctx.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            ctx.startActivity(intent)
        }
    }

    // 🚀 Lanzador de permisos múltiples
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val cameraGranted = permissions[android.Manifest.permission.CAMERA] == true
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true

        val activity = (context as? Activity)

        if (cameraGranted && locationGranted) {
            coroutineScope.launch {
                if (!isLocationEnabled(context)) {
                    isCheckingPermissions = false
                    showEnableLocationDialog = true
                    val res = snackbarHostState.showSnackbar("GPS desactivado. Actívalo para registrar tu asistencia.", "Abrir ajustes")
                    if (res == SnackbarResult.ActionPerformed) openLocationSettings(context)
                    return@launch
                }

                // ✅ Usamos la versión mejorada
                isLoadingLocation = true
                val result = awaitLocationForAttendanceImproved(fusedLocationClient, context, locationDao, 8000L)
                isLoadingLocation = false

                when (result) {
                    is LocationResult.Success -> {
                        val loc = result.location
                        if (isLocationPossiblyMocked(loc)) {
                            mockLocationAppName = tryGetMockLocationAppName(context)
                            showMockLocationDialog = true
                            isCheckingPermissions = false
                            return@launch
                        }

                        if (!isNavigatingToCamera) {
                            isNavigatingToCamera = true
                            val typePath = if (currentAttendanceType == AttendanceType.ENTRADA) "ENTRADA" else "SALIDA"
                            navController.navigate("camera/$typePath")
                        }
                    }

                    is LocationResult.Error -> {
                        val message = when (result.reason) {
                            LocationError.PERMISSION_DENIED -> "No tienes permisos de ubicación. Actívalos en Ajustes."
                            LocationError.GPS_DISABLED -> "Tu GPS está desactivado. Actívalo e inténtalo nuevamente."
                            LocationError.TIMEOUT -> "El GPS tardó demasiado en responder. Intenta moverte o verifica la señal."
                            LocationError.NO_LOCATION_AVAILABLE -> "No se pudo obtener tu ubicación. Intenta nuevamente."
                            LocationError.INACCURATE -> "La señal GPS es imprecisa. Busca un lugar más abierto e inténtalo otra vez."
                            LocationError.UNKNOWN -> "Error desconocido al obtener la ubicación."
                        }

                        // Aseguramos visibilidad: log + toast + snackbar + diálogo modal
                        Log.d("HomeScreen", "Location error: ${result.reason} -> $message")
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message)
                        }

                        // Mostrar diálogo modal para asegurar visibilidad
                        locationErrorMessage = message
                        showLocationErrorDialog = true

                        // Acciones específicas para guiar al usuario
                        when (result.reason) {
                            LocationError.GPS_DISABLED -> {
                                showEnableLocationDialog = true
                            }
                            LocationError.PERMISSION_DENIED -> {
                                showAppSettingsDialog = true
                            }
                            else -> { /* no-op */ }
                        }
                    }
                }

                isCheckingPermissions = false
            }
            return@rememberLauncherForActivityResult
        }

        // ❌ Permisos no concedidos
        val missing = mutableListOf<String>()
        if (!cameraGranted) missing.add("Cámara")
        if (!locationGranted) missing.add("Ubicación")

        var anyPermanentlyDenied = false
        if (activity != null) {
            val permsToCheck = listOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            for (p in permsToCheck) {
                val granted = when (p) {
                    android.Manifest.permission.CAMERA -> cameraGranted
                    else -> locationGranted
                }
                if (!granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, p)) {
                    anyPermanentlyDenied = true
                    break
                }
            }
        }

        if (anyPermanentlyDenied) {
            rationaleMessage = "Permisos bloqueados: ${missing.joinToString(", ")}. Ve a Ajustes para habilitarlos."
            showAppSettingsDialog = true
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Faltan permisos: ${missing.joinToString(", ")}. Por favor habilítalos.")
            }
        }
        isCheckingPermissions = false
    }

    // 🧭 Flujo principal de asistencia
    fun startAttendanceFlow(type: AttendanceType) {
        if (isCheckingPermissions || isNavigatingToCamera) return
        isCheckingPermissions = true
        currentAttendanceType = type

        if (!isLocationEnabled(context)) {
            isCheckingPermissions = false
            showEnableLocationDialog = true
            coroutineScope.launch {
                val res = snackbarHostState.showSnackbar("GPS desactivado. Actívalo para registrar tu asistencia.", "Abrir ajustes")
                if (res == SnackbarResult.ActionPerformed) openLocationSettings(context)
            }
            return
        }

        if (hasCameraPermission(context) && hasLocationPermission(context)) {
            coroutineScope.launch {
                isLoadingLocation = true
                val result = awaitLocationForAttendanceImproved(fusedLocationClient, context, locationDao, 8000L)
                isLoadingLocation = false

                when (result) {
                    is LocationResult.Success -> {
                        val loc = result.location
                        if (isLocationPossiblyMocked(loc)) {
                            mockLocationAppName = tryGetMockLocationAppName(context)
                            showMockLocationDialog = true
                            isCheckingPermissions = false
                            return@launch
                        }

                        if (!isNavigatingToCamera) {
                            isNavigatingToCamera = true
                            val typePath = if (currentAttendanceType == AttendanceType.ENTRADA) "ENTRADA" else "SALIDA"
                            navController.navigate("camera/$typePath")
                        }
                    }

                    is LocationResult.Error -> {
                        val message = when (result.reason) {
                            LocationError.PERMISSION_DENIED -> "No tienes permisos de ubicación. Actívalos en Ajustes."
                            LocationError.GPS_DISABLED -> "Tu GPS está desactivado. Actívalo e inténtalo nuevamente."
                            LocationError.TIMEOUT -> "El GPS tardó demasiado en responder. Intenta moverte o verifica la señal."
                            LocationError.NO_LOCATION_AVAILABLE -> "No se pudo obtener tu ubicación. Intenta nuevamente."
                            LocationError.INACCURATE -> "La señal GPS es imprecisa. Busca un lugar más abierto e inténtalo otra vez."
                            LocationError.UNKNOWN -> "Error desconocido al obtener la ubicación."
                        }

                        // Aseguramos visibilidad: log + toast + snackbar + diálogo modal
                        Log.d("HomeScreen", "Location error: ${result.reason} -> $message")
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message)
                        }

                        // Mostrar diálogo modal para asegurar visibilidad
                        locationErrorMessage = message
                        showLocationErrorDialog = true

                        // Acciones específicas para guiar al usuario
                        when (result.reason) {
                            LocationError.GPS_DISABLED -> {
                                showEnableLocationDialog = true
                            }
                            LocationError.PERMISSION_DENIED -> {
                                showAppSettingsDialog = true
                            }
                            else -> { /* no-op */ }
                        }
                    }
                }

                isCheckingPermissions = false
            }
            return
        }

        // ⚠️ Si faltan permisos
        val toRequest = mutableListOf<String>()
        if (!hasCameraPermission(context)) toRequest.add(android.Manifest.permission.CAMERA)
        if (!hasLocationPermission(context)) {
            toRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            toRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (toRequest.isEmpty()) {
            isCheckingPermissions = false
            return
        }

        val activity = (context as? Activity)
        var shouldShowRationaleAny = false
        if (activity != null) {
            for (p in toRequest) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, p)) {
                    shouldShowRationaleAny = true
                    break
                }
            }
        }

        if (shouldShowRationaleAny) {
            rationaleMessage = "La app necesita acceso a cámara y ubicación para registrar tu asistencia."
            showRationaleDialog = true
        } else {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    // 🧱 UI principal
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                BlueHeaderWithName(
                    userName = userName,
                    currentDate = currentDate.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .zIndex(1f)
                )

                if (!isLocationEnabled(context)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ubicación desactivada. Actívala para registrar asistencia.", modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { openLocationSettings(context) }) { Text("Activar ubicación") }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    RoundedTopContainer {
                        BannerCarousel()
                        Spacer(modifier = Modifier.height(16.dp))
                        LastMarkText(viewModel = attendanceViewModel)
                        Spacer(modifier = Modifier.height(24.dp))
                        EntryExitButtons(
                            onEntry = { startAttendanceFlow(AttendanceType.ENTRADA) },
                            onExit = { startAttendanceFlow(AttendanceType.SALIDA) },
                            onLogout = {
                                Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                                navController.navigate("login") { popUpTo("home") { inclusive = true } }
                                userViewModel.clearUser()
                            },
                            isBusy = (isCheckingPermissions || isLoadingLocation || isNavigatingToCamera),
                            activeType = currentAttendanceType
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            if (isLoadingLocation) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .pointerInput(Unit) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Obteniendo ubicación...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Diálogos comunes
            if (showRationaleDialog) {
                AlertDialog(
                    onDismissRequest = { showRationaleDialog = false; isCheckingPermissions = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showRationaleDialog = false
                            val perms = mutableListOf<String>()
                            if (!hasCameraPermission(context)) perms.add(android.Manifest.permission.CAMERA)
                            if (!hasLocationPermission(context)) {
                                perms.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                perms.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                            if (perms.isNotEmpty()) permissionLauncher.launch(perms.toTypedArray())
                        }) { Text("Continuar") }
                    },
                    dismissButton = { TextButton(onClick = { showRationaleDialog = false; isCheckingPermissions = false }) { Text("Cancelar") } },
                    title = { Text("Permisos requeridos") },
                    text = { Text(rationaleMessage) }
                )
            }

            if (showAppSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showAppSettingsDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showAppSettingsDialog = false
                            isCheckingPermissions = false
                            openAppSettings(context)
                        }) { Text("Abrir Ajustes") }
                    },
                    dismissButton = { TextButton(onClick = { showAppSettingsDialog = false }) { Text("Cerrar") } },
                    title = { Text("Permisos bloqueados") },
                    text = { Text(rationaleMessage) }
                )
            }

            if (showEnableLocationDialog) {
                AlertDialog(
                    onDismissRequest = { showEnableLocationDialog = false; isCheckingPermissions = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showEnableLocationDialog = false
                            isCheckingPermissions = false
                            openLocationSettings(context)
                        }) { Text("Abrir ajustes de ubicación") }
                    },
                    dismissButton = { TextButton(onClick = { showEnableLocationDialog = false }) { Text("Cancelar") } },
                    title = { Text("Ubicación desactivada") },
                    text = { Text("La ubicación (GPS) está desactivada. Actívala para que la app pueda obtener tu posición al registrar la asistencia.") }
                )
            }

            if (showMockLocationDialog) {
                AlertDialog(
                    onDismissRequest = { showMockLocationDialog = false; isCheckingPermissions = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showMockLocationDialog = false
                            isCheckingPermissions = false
                            if (!mockLocationAppName.isNullOrEmpty()) {
                                try {
                                    val pkg = mockLocationAppName!!
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", pkg, null)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    openAppSettings(context)
                                }
                            } else {
                                openAppSettings(context)
                            }
                        }) { Text("Abrir ajustes") }
                    },
                    dismissButton = { TextButton(onClick = { showMockLocationDialog = false; isCheckingPermissions = false }) { Text("Cancelar") } },
                    title = { Text("Ubicación posiblemente falsa") },
                    text = { Text(if (mockLocationAppName != null) "Se detectó que la ubicación podría ser falsificada por ${mockLocationAppName}. Desactiva o desinstala esa aplicación y vuelve a intentarlo." else "Se detectó que la ubicación podría ser falsificada. Desactiva apps de ubicación falsa (mock) y vuelve a intentarlo.") }
                )
            }

            // Diálogo de error de ubicación (asegura visibilidad)
            if (showLocationErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showLocationErrorDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showLocationErrorDialog = false }) { Text("Aceptar") }
                    },
                    title = { Text("Error de ubicación") },
                    text = { Text(locationErrorMessage) }
                )
            }
        }
    }
}
