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
import com.example.myapplication.data.local.AttendanceType
import com.example.myapplication.ui.Attendance.AttendanceViewModel
import com.example.myapplication.ui.user.UserViewModel
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.material3.SnackbarResult

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

    var isCheckingPermissions by remember { mutableStateOf(false) }
    var isNavigatingToCamera by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val userViewModel: UserViewModel = viewModel()
    val userName by userViewModel.userName.collectAsState()

    val currentDate = remember { mutableStateOf("") }
    var currentAttendanceType by remember { mutableStateOf(AttendanceType.ENTRADA) }

    // Estados para mostrar diálogos cuando permisos están denegados permanentemente o explicar por qué se piden
    var showAppSettingsDialog by remember { mutableStateOf(false) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showEnableLocationDialog by remember { mutableStateOf(false) }
    var rationaleMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        currentDate.value = SimpleDateFormat("EEEE dd, MMM yyyy", Locale("es")).format(Date())
    }

    fun openLocationSettings(ctx: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            ctx.startActivity(intent)
        } catch (_: Exception) { }
    }

    // ------------------------------- Helpers de permisos -------------------------------
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
            // fallback: abrir pantalla general de ajustes
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            ctx.startActivity(intent)
        }
    }

    // Lanzador de petición de permisos múltiples
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Siempre evaluar el resultado completo y actuar de forma determinista
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] == true
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true

        val activity = (context as? Activity)

        if (cameraGranted && locationGranted) {
            // ambos permisos concedidos -> obtener ubicación con timeout y navegar
            coroutineScope.launch {
                // si el GPS está desactivado, sugerir al usuario activarlo
                if (!isLocationEnabled(context)) {
                    isCheckingPermissions = false
                    // diagnóstico: indicar por qué se muestra el diálogo
                    Log.d("HomeScreen", "GPS desactivado: mostrando diálogo para activar ubicación (permissionLauncher)")
                    Toast.makeText(context, "GPS desactivado: mostrando diálogo para activar ubicación", Toast.LENGTH_SHORT).show()
                    showEnableLocationDialog = true
                    // también mostrar snackbar con acción para abrir ajustes (por si el diálogo no se ve)
                    coroutineScope.launch {
                        val res = snackbarHostState.showSnackbar("GPS desactivado. Actívalo para registrar tu asistencia.", "Abrir ajustes")
                        if (res == SnackbarResult.ActionPerformed) {
                            openLocationSettings(context)
                        }
                    }
                    return@launch
                }
                 isLoadingLocation = true
                 var loc = awaitLocationWithTimeout(fusedLocationClient, 10000L)
                 isLoadingLocation = false
                 if (loc != null) {
                    if (!isNavigatingToCamera) {
                        isNavigatingToCamera = true
                        val typePath = if (currentAttendanceType == AttendanceType.ENTRADA) "ENTRADA" else "SALIDA"
                        navController.navigate("camera/$typePath")
                    }
                } else {
                    // Intentar un reintento rápido una vez
                    Log.d("HomeScreen", "Ubicación no obtenida: intentando un reintento rápido (permissionLauncher)")
                    coroutineScope.launch {
                        isLoadingLocation = true
                        snackbarHostState.showSnackbar("Reintentando obtener ubicación...")
                        delay(1000L)
                        val loc2 = awaitLocationWithTimeout(fusedLocationClient, 5000L)
                        isLoadingLocation = false
                        if (loc2 != null) {
                            if (!isNavigatingToCamera) {
                                isNavigatingToCamera = true
                                val typePath = if (currentAttendanceType == AttendanceType.ENTRADA) "ENTRADA" else "SALIDA"
                                navController.navigate("camera/$typePath")
                            }
                        } else {
                            snackbarHostState.showSnackbar("No se pudo obtener la ubicación GPS. Intenta nuevamente.")
                        }
                    }
                }
                isCheckingPermissions = false
            }
            return@rememberLauncherForActivityResult
        }

        // Algún permiso no concedido -> identificar cuáles
        val missing = mutableListOf<String>()
        if (!cameraGranted) missing.add("Cámara")
        if (!locationGranted) missing.add("Ubicación")

        // Detectar si alguno fue denegado permanentemente (no mostrar rationale)
        var anyPermanentlyDenied = false
        if (activity != null) {
            val permsToCheck = listOf(android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            for (p in permsToCheck) {
                val wasGranted = when (p) {
                    android.Manifest.permission.CAMERA -> cameraGranted
                    else -> locationGranted
                }
                if (!wasGranted) {
                    val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(activity, p)
                    if (!shouldShow) {
                        anyPermanentlyDenied = true
                        break
                    }
                }
            }
        }

        if (anyPermanentlyDenied) {
            rationaleMessage = "Permisos bloqueados: ${missing.joinToString(", ")}. Ve a Ajustes de la aplicación para habilitarlos."
            showAppSettingsDialog = true
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Faltan permisos: ${missing.joinToString(", ")}. Por favor habilítalos.")
            }
        }
        isCheckingPermissions = false
    }

    // Centraliza la comprobación y petición de permisos antes de navegar a cámara
    fun startAttendanceFlow(type: AttendanceType) {
        if (isCheckingPermissions || isNavigatingToCamera) return
        isCheckingPermissions = true
        currentAttendanceType = type

        // Comprobación temprana: si GPS está apagado, pedir al usuario activarlo antes de seguir
        if (!isLocationEnabled(context)) {
            isCheckingPermissions = false
            // diagnóstico: indicar por qué se muestra el diálogo
            Log.d("HomeScreen", "GPS desactivado: mostrando diálogo para activar ubicación (startAttendanceFlow)")
            Toast.makeText(context, "GPS desactivado: mostrando diálogo para activar ubicación", Toast.LENGTH_SHORT).show()
            showEnableLocationDialog = true
            coroutineScope.launch {
                val res = snackbarHostState.showSnackbar("GPS desactivado. Actívalo para registrar tu asistencia.", "Abrir ajustes")
                if (res == SnackbarResult.ActionPerformed) openLocationSettings(context)
            }
            return
        }

        // Si ya tiene permisos -> obtener ubicación y navegar
        if (hasCameraPermission(context) && hasLocationPermission(context)) {
            // si el GPS está desactivado, mostrar diálogo para abrir ajustes de ubicación
            if (!isLocationEnabled(context)) {
                isCheckingPermissions = false
                // diagnóstico: indicar por qué se muestra el diálogo
                Log.d("HomeScreen", "GPS desactivado: mostrando diálogo para activar ubicación (startAttendanceFlow)")
                Toast.makeText(context, "GPS desactivado: mostrando diálogo para activar ubicación", Toast.LENGTH_SHORT).show()
                showEnableLocationDialog = true
                coroutineScope.launch {
                    val res = snackbarHostState.showSnackbar("GPS desactivado. Actívalo para registrar tu asistencia.", "Abrir ajustes")
                    if (res == SnackbarResult.ActionPerformed) openLocationSettings(context)
                }
                return
            }
             coroutineScope.launch {
                 isLoadingLocation = true
                 var loc = awaitLocationWithTimeout(fusedLocationClient, 10000L)
                 isLoadingLocation = false
                 if (loc != null) {
                    if (!isNavigatingToCamera) {
                        isNavigatingToCamera = true
                        val typePath = if (currentAttendanceType == AttendanceType.ENTRADA) "ENTRADA" else "SALIDA"
                        navController.navigate("camera/$typePath")
                    }
                } else {
                    // Intentar un reintento rápido una vez
                    Log.d("HomeScreen", "Ubicación no obtenida: intentando un reintento rápido (startAttendanceFlow)")
                    coroutineScope.launch {
                        isLoadingLocation = true
                        snackbarHostState.showSnackbar("Reintentando obtener ubicación...")
                        delay(1000L)
                        val loc2 = awaitLocationWithTimeout(fusedLocationClient, 5000L)
                        isLoadingLocation = false
                        if (loc2 != null) {
                            if (!isNavigatingToCamera) {
                                isNavigatingToCamera = true
                                val typePath = if (currentAttendanceType == AttendanceType.ENTRADA) "ENTRADA" else "SALIDA"
                                navController.navigate("camera/$typePath")
                            }
                        } else {
                            snackbarHostState.showSnackbar("No se pudo obtener la ubicación GPS. Activa la ubicación y vuelve a intentar.")
                        }
                    }
                }
                isCheckingPermissions = false
            }
            return
        }

        // Preparar lista de permisos a pedir
        val toRequest = mutableListOf<String>()
        if (!hasCameraPermission(context)) toRequest.add(android.Manifest.permission.CAMERA)
        if (!hasLocationPermission(context)) {
            toRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            toRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Si la lista está vacía (caso raro), terminar
        if (toRequest.isEmpty()) {
            isCheckingPermissions = false
            return
        }

        // Comprobar si debemos mostrar rationale antes de pedir
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
            rationaleMessage = "Esta app necesita acceso a la cámara y a la ubicación para registrar tu entrada/salida con foto y GPS."
            showRationaleDialog = true
            // cuando el usuario confirma, se lanzará permissionLauncher desde el UI
        } else {
            // Lanzar petición de permisos directamente
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    // Lifecycle owner para re-evaluar al volver desde ajustes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Si se mostraba el diálogo de activar ubicación y ahora está activa, reanudar flujo
                if (showEnableLocationDialog && isLocationEnabled(context) && !isCheckingPermissions && !isNavigatingToCamera) {
                    showEnableLocationDialog = false
                    // reintentar el flujo con el tipo guardado
                    startAttendanceFlow(currentAttendanceType)
                }
                // Si se mostraba diálogo de ajustes por permisos bloqueados y ahora los permisos están concedidos, intentar continuar
                if (showAppSettingsDialog && hasCameraPermission(context) && hasLocationPermission(context) && !isCheckingPermissions && !isNavigatingToCamera) {
                    showAppSettingsDialog = false
                    startAttendanceFlow(currentAttendanceType)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

                // Banner visible cuando el GPS está desactivado para instruir al usuario a activarlo
                if (!isLocationEnabled(context)) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                        .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                        .pointerInput(Unit) {}
                    ,
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Obteniendo ubicación...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Diálogo de rationale antes de pedir permisos
            if (showRationaleDialog) {
                AlertDialog(
                    onDismissRequest = { showRationaleDialog = false; isCheckingPermissions = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showRationaleDialog = false
                            // lanzar petición
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

            // Diálogo para abrir ajustes si permisos denegados permanentemente
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

            // Diálogo para invitar al usuario a activar la ubicación (GPS)
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

        }
    }
}

// Utilidad segura para eliminar ficheros; devuelve true si se borró correctamente
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

// Nota: la función previa getLastKnownLocation quedó reemplazada por awaitLocationWithTimeout dentro del composable para mayor control

@Suppress("unused")
fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
}
