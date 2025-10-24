package com.example.myapplication.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Environment
import android.os.Looper
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.zIndex
import android.content.pm.PackageManager

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
    var rationaleMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        currentDate.value = SimpleDateFormat("EEEE dd, MMM yyyy", Locale("es")).format(Date())
    }

    // Helper: verificar permisos
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

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] == true
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true

        val activity = (context as? Activity)

        if (cameraGranted && locationGranted) {
            // permisos concedidos -> proceder igual que antes
            if (isLocationEnabled(context)) {
                isLoadingLocation = true
                coroutineScope.launch {
                    getLastKnownLocation(fusedLocationClient) { location ->
                        isLoadingLocation = false
                        if (location != null) {
                            if (!isNavigatingToCamera) {
                                isNavigatingToCamera = true
                                val typePath = if (currentAttendanceType == AttendanceType.ENTRADA) "ENTRADA" else "SALIDA"
                                navController.navigate("camera/$typePath")
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("No se pudo obtener la ubicación GPS.")
                            }
                        }
                        isCheckingPermissions = false
                    }
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("GPS desactivado. Actívalo para usar la ubicación.")
                }
                isCheckingPermissions = false
             }
         } else {
             // Algún permiso denegado. Detectar si es denegación permanente -> abrir ajustes
             var anyPermanentlyDenied = false
             if (activity != null) {
                 val permsToCheck = listOf(android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                 for (p in permsToCheck) {
                     val granted = permissions[p] == true
                     if (!granted) {
                         // Si NO se debe mostrar rationale y además no está concedido, se asumirá como denegado permanentemente
                         val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(activity, p)
                         if (!shouldShow) {
                             anyPermanentlyDenied = true
                             break
                         }
                     }
                 }
             }

             // Construir lista de permisos faltantes para mostrar un mensaje más claro
             val missing = mutableListOf<String>()
             if (!(permissions[android.Manifest.permission.CAMERA] == true)) missing.add("Cámara")
             if (!((permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) || (permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true))) missing.add("Ubicación")

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
     }

     // Función que centraliza la comprobación+petición de permisos antes de navegar
     fun startAttendanceFlow(type: AttendanceType) {
         val activity = (context as? Activity)
         currentAttendanceType = type

         // Si ya tiene permisos, comprobar GPS y obtener ubicación
         if (hasCameraPermission(context) && hasLocationPermission(context)) {
             if (isLocationEnabled(context)) {
                 isLoadingLocation = true
                 coroutineScope.launch {
                     getLastKnownLocation(fusedLocationClient) { location ->
                         isLoadingLocation = false
                         if (location != null) {
                             if (!isNavigatingToCamera) {
                                 isNavigatingToCamera = true
                                 val typePath = if (currentAttendanceType == AttendanceType.ENTRADA) "ENTRADA" else "SALIDA"
                                 navController.navigate("camera/$typePath")
                             }
                         } else {
                             coroutineScope.launch {
                                 snackbarHostState.showSnackbar("No se pudo obtener la ubicación GPS.")
                             }
                         }
                         isCheckingPermissions = false
                     }
                 }
             } else {
                 coroutineScope.launch {
                     snackbarHostState.showSnackbar("GPS desactivado. Actívalo para usar la ubicación.")
                 }
                 isCheckingPermissions = false
             }
             return
         }

         // Si no tiene permisos, preparar lista de permisos a solicitar
         val toRequest = mutableListOf<String>()
         if (!hasCameraPermission(context)) toRequest.add(android.Manifest.permission.CAMERA)
         if (!hasLocationPermission(context)) {
             // pedimos FINE; en caso de no conceder, el launcher resolverá
             toRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
             toRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
         }

         // Antes de lanzar el request, si alguna permission requiere rationale (explicación), mostrar dialogo de rationale
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
             // cuando el usuario acepte el rationale, lanzaremos el launcher
         } else {
             // Lanzar directamente la petición
             cameraPermissionLauncher.launch(toRequest.toTypedArray())
         }
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

                 Box(modifier = Modifier.fillMaxSize()) {
                     RoundedTopContainer {
                         BannerCarousel()
                         Spacer(modifier = Modifier.height(16.dp))
                         LastMarkText(viewModel = attendanceViewModel)
                         Spacer(modifier = Modifier.height(24.dp))
                         EntryExitButtons(
                             onEntry = {
                                 if (!isCheckingPermissions && !isNavigatingToCamera) {
                                     isCheckingPermissions = true
                                     startAttendanceFlow(AttendanceType.ENTRADA)
                                 }
                             },
                             onExit = {
                                 if (!isCheckingPermissions && !isNavigatingToCamera) {
                                     isCheckingPermissions = true
                                     startAttendanceFlow(AttendanceType.SALIDA)
                                 }
                             },
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
                             if (perms.isNotEmpty()) cameraPermissionLauncher.launch(perms.toTypedArray())
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
             1000L
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
     } catch (_: SecurityException) {
         onLocationReceived(null)
     }
 }

 fun isLocationEnabled(context: Context): Boolean {
     val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
     return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
             locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
 }
