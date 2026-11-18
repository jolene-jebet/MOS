// OWNER: C — TODO
package com.example.mos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mos.data.AuthRepository
import com.example.mos.data.FakeAuthRepository
import com.example.mos.data.FakeRiskRepository
import com.example.mos.data.FakeRouteRepository
import com.example.mos.data.RiskRepository
import com.example.mos.data.RouteRepository
import com.example.mos.screens.auth.LoginScreen
import com.example.mos.screens.auth.SignupScreen
import com.example.mos.screens.explore.ExploreScreen
import com.example.mos.screens.profile.ProfileScreen
import com.example.mos.screens.routedetail.RouteDetailScreen
import com.example.mos.ui.theme.MOSTheme

class MainActivity : ComponentActivity() {

    // Repositories are created by hand here (no Hilt).
    // TODO: swap the Fake ones for real ones and pass them to the ViewModels.
    private val authRepository: AuthRepository = FakeAuthRepository()
    private val routeRepository: RouteRepository = FakeRouteRepository()
    private val riskRepository: RiskRepository = FakeRiskRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Globally configure osmdroid User-Agent for all tile downloader threads across screen transitions
        val prefs = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)
        org.osmdroid.config.Configuration.getInstance().apply {
            load(this@MainActivity, prefs)
            userAgentValue = "${packageName}/1.0 (contact@mos.app; MOS-NairobiSafetyNavigator)"
            save(this@MainActivity, prefs)
        }

        enableEdgeToEdge()
        setContent {
            MOSTheme {
                MOSApp()
            }
        }
    }
}

@Composable
fun MOSApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Bottom bar is shown only after login.
    val showBottomBar = currentRoute != null && currentRoute != "login" && currentRoute != "signup"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    // TODO: add icons
                    NavigationBarItem(
                        selected = currentRoute == "explore",
                        onClick = { navController.navigate("explore") { launchSingleTop = true } },
                        icon = {},
                        label = { Text("Explore") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "profile",
                        onClick = { navController.navigate("profile") { launchSingleTop = true } },
                        icon = {},
                        label = { Text("Profile") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onLogin = {
                        navController.navigate("explore") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onGoToSignup = { navController.navigate("signup") }
                )
            }
            composable("signup") {
                SignupScreen(
                    onSignup = {
                        navController.navigate("explore") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onGoToLogin = { navController.popBackStack() }
                )
            }
            composable("explore") {
                ExploreScreen(
                    onRouteClick = { routeId -> navController.navigate("routedetail/$routeId") }
                )
            }
            composable("routedetail/{routeId}") { entry ->
                RouteDetailScreen(routeId = entry.arguments?.getString("routeId").orEmpty())
            }
            composable("profile") {
                ProfileScreen(
                    onSignOut = {
                        navController.navigate("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
