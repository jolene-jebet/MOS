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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.mos.data.FirebaseAuthRepository
import com.example.mos.data.FakeRiskRepository
import com.example.mos.data.FakeRouteRepository
import com.example.mos.data.GoogleRouteRepository
import com.example.mos.data.RiskRepository
import com.example.mos.data.RouteRepository
import com.example.mos.screens.auth.AuthViewModel
import com.example.mos.screens.auth.LoginScreen
import com.example.mos.screens.auth.SignupScreen
import com.example.mos.screens.auth.WelcomeScreen
import com.example.mos.screens.explore.ExploreScreen
import com.example.mos.screens.profile.EditProfileScreen
import com.example.mos.screens.profile.EditProfileViewModel
import com.example.mos.screens.profile.ProfileScreen
import com.example.mos.screens.profile.ProfileViewModel
import com.example.mos.screens.routedetail.RouteDetailScreen
import com.example.mos.screens.routedetail.RouteDetailViewModel
import com.example.mos.ui.theme.MOSTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Repositories are created by hand here (no Hilt).
    // TODO: swap the Fake ones for real ones and pass them to the ViewModels.
    private val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
    private val routeRepository: RouteRepository by lazy { GoogleRouteRepository(applicationContext, FakeRouteRepository()) }
    private val riskRepository: RiskRepository by lazy { FakeRiskRepository(applicationContext)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    val scope = rememberCoroutineScope()

    // Resume straight into Explore if already signed in; otherwise start at Welcome.
    val startDestination = remember { if (authRepository.currentUser() != null) "explore" else "welcome" }

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
            composable("welcome") {
                WelcomeScreen(
                    onLogin = { navController.navigate("login") },
                    onSignup = { navController.navigate("signup") }
                )
            }
            composable("login") {
                LoginScreen(
                    viewModel = viewModel { AuthViewModel(authRepository) },
                    onLogin = {
                        navController.navigate("explore") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    // popUpTo keeps the back stack as Welcome -> Signup (no Login/Signup pile-up).
                    onGoToSignup = { navController.navigate("signup") { popUpTo("welcome") } }
                )
            }
            composable("signup") {
                SignupScreen(
                    viewModel = viewModel { AuthViewModel(authRepository) },
                    onSignup = {
                        navController.navigate("explore") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onGoToLogin = { navController.navigate("login") { popUpTo("welcome") } }
                )
            }
            composable("explore") {
                ExploreScreen(
                    onRouteClick = { routeId -> navController.navigate("routedetail/$routeId") },
                    routeRepository = routeRepository,
                    riskRepository = riskRepository,
                    authRepository = authRepository
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
                // After logging out or deleting the account, start again at Welcome.
                val goToWelcome = {
                    navController.navigate("welcome") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
                ProfileScreen(
                    viewModel = viewModel { ProfileViewModel(authRepository) },
                    onEditProfile = { navController.navigate("editprofile") },
                    onSignOut = {
                        scope.launch {
                            authRepository.logout()
                            goToWelcome()
                        }
                    },
                    onAccountDeleted = goToWelcome
                )
            }
            composable("editprofile") {
                EditProfileScreen(
                    viewModel = viewModel { EditProfileViewModel(authRepository) },
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}
