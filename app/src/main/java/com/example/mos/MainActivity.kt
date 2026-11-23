// OWNER: C — TODO
package com.example.mos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mos.data.AuthRepository
import com.example.mos.data.FakeAuthRepository
import com.example.mos.data.RISK_API_BASE_URL
import com.example.mos.data.FakeRouteRepository
import com.example.mos.data.GoogleRouteRepository
import com.example.mos.data.RiskRepository
import com.example.mos.data.RouteRepository
import com.example.mos.screens.auth.LoginScreen
import com.example.mos.screens.auth.SignupScreen
import com.example.mos.screens.explore.ExploreScreen
import com.example.mos.screens.profile.ProfileScreen
import com.example.mos.screens.routedetail.RouteDetailScreen
import com.example.mos.screens.routedetail.RouteDetailViewModel
import com.example.mos.ui.theme.MOSTheme
import androidx.lifecycle.lifecycleScope
import com.example.mos.data.RemoteRiskRepository
import com.example.mos.data.RiskApiService
import com.example.mos.data.createRiskApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import android.util.Log
import com.example.mos.data.RiskPredictRequest

class MainActivity : ComponentActivity() {

    // Repositories are created by hand here (no Hilt).
    // TODO: swap the Fake ones for real ones and pass them to the ViewModels.
    private val authRepository: AuthRepository = FakeAuthRepository()
    private val routeRepository: RouteRepository by lazy { GoogleRouteRepository(applicationContext, FakeRouteRepository()) }
    //private val riskRepository: RiskRepository by lazy { FakeRiskRepository(applicationContext)}
    private val riskApi: RiskApiService by lazy { createRiskApiService() }
    private val riskRepository: RiskRepository by lazy { RemoteRiskRepository(riskApi) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        android.widget.Toast.makeText(
            this,
            "risk: ${riskRepository::class.simpleName} @ $RISK_API_BASE_URL",
            android.widget.Toast.LENGTH_LONG
        ).show()
        android.util.Log.d("MOS-Risk", "MainActivity risk repo = ${riskRepository::class.simpleName}, url = $RISK_API_BASE_URL")
        lifecycleScope.launch {
            try { riskApi.health() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { }

            try {
                val r = riskApi.predict(
                    RiskPredictRequest(
                        listOf(listOf(-1.2864, 36.8172), listOf(-1.3192, 36.9278)), "MATATU", 17
                    )
                )
                Log.d("MOS-Risk", "TEST predict OK: $r")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MOS-Risk", "TEST predict failed", e)
            }
        }
        setContent {
            MOSTheme {
                MOSApp(
                    authRepository = authRepository,
                    routeRepository = routeRepository,
                    riskRepository = riskRepository
                )
            }
        }
    }
}

@Composable
fun MOSApp(
    authRepository: AuthRepository,
    routeRepository: RouteRepository,
    riskRepository: RiskRepository
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Resume straight into Explore if already signed in; otherwise start at Login.
    val startDestination = remember { if (authRepository.currentUser() != null) "explore" else "login" }

    // Bottom bar is shown only on the two main tabs.
    val showBottomBar = currentRoute == "explore" || currentRoute == "profile"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "explore",
                        onClick = { navController.navigate("explore") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                        label = { Text("Explore") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "profile",
                        onClick = { navController.navigate("profile") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
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
                    onRouteClick = { routeId -> navController.navigate("routedetail/$routeId") },
                    routeRepository = routeRepository,
                    riskRepository = riskRepository
                )
            }
            composable("routedetail/{routeId}") { entry ->
                val routeDetailViewModel: RouteDetailViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = viewModelFactory {
                        initializer {
                            RouteDetailViewModel(
                                savedStateHandle = createSavedStateHandle(),
                                routeRepository = routeRepository,
                                riskRepository = riskRepository
                            )
                        }
                    }
                )
                RouteDetailScreen(
                    viewModel = routeDetailViewModel,
                    onBack = { navController.popBackStack() }
                )
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
