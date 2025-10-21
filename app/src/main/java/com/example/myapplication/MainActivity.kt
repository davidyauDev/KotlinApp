package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.local.AttendanceDatabase
import com.example.myapplication.data.local.AttendanceType
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.AttendanceRepository
import com.example.myapplication.navigation.BottomNavBar
import com.example.myapplication.navigation.NavItemList
import com.example.myapplication.ui.Attendance.AttendanceScreen
import com.example.myapplication.ui.Attendance.AttendanceViewModel
import com.example.myapplication.ui.Attendance.AttendanceViewModelFactory
import com.example.myapplication.ui.camera.CameraScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.login.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                AppNavigation(navController)
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    val context = LocalContext.current.applicationContext as Application
    val userPreferences = remember { UserPreferences(context) }
    val db = AttendanceDatabase.getDatabase(context)
    val dao = db.attendanceDao()
    val repository = AttendanceRepository(userPreferences, context, dao)
    val factory = AttendanceViewModelFactory(context, repository)
    val attendanceViewModel: AttendanceViewModel = viewModel(factory = factory)

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            BottomNavScreen(navController = navController, attendanceViewModel = attendanceViewModel)
        }

        composable("camera/{attendanceType}") { backStackEntry ->
            val typeString = backStackEntry.arguments?.getString("attendanceType")
            val type = if (typeString == "ENTRADA") AttendanceType.ENTRADA else AttendanceType.SALIDA
            CameraScreen(
                navController = navController,
                attendanceType = type,
                attendanceViewModel = attendanceViewModel
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun BottomNavScreen(navController: NavHostController, attendanceViewModel: AttendanceViewModel) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val routesWithBottomBar = listOf("main")
    val showBottomBar = currentRoute in routesWithBottomBar

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    navItemList = NavItemList.navItemList,
                    selectedIndex = selectedIndex,
                    onItemSelected = { index -> selectedIndex = index }
                )
            }
        }
    ) { paddingValues ->
        ContentScreen(
            selectedIndex = selectedIndex,
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            attendanceViewModel = attendanceViewModel
        )
    }
}

@Composable
fun ContentScreen(
    selectedIndex: Int,
    navController: NavHostController,
    attendanceViewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    when (selectedIndex) {
        0 -> HomeScreen(navController, attendanceViewModel = attendanceViewModel, modifier = modifier)
        1 -> AttendanceScreen(attendanceViewModel = attendanceViewModel, modifier = modifier)
        2 -> AttendanceScreen(attendanceViewModel = attendanceViewModel, modifier = modifier)
    }

}
