package com.example.myapplication.data.device


import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.*

data class DeviceSnapshot(
    val timestamp: String,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val bootTime: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: Int,
    val connectedToInternet: Boolean
)


fun getDeviceSnapshot(context: Context): DeviceSnapshot {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) // ✅
    val isCharging = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0

    val bootTimeMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime()
    val bootTimeFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        .format(Date(bootTimeMillis))

    val isConnected = isDeviceOnline(context)

    val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    return DeviceSnapshot(
        timestamp = now,
        batteryLevel = batteryLevel,
        isCharging = isCharging,
        bootTime = bootTimeFormatted,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        androidVersion = Build.VERSION.SDK_INT,
        connectedToInternet = isConnected
    )
}


private fun isDeviceOnline(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}