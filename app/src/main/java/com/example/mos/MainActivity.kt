// OWNER: C — TODO
package com.example.mos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.example.mos.data.RISK_API_BASE_URL
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
    private val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
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

    val scope = rememberCoroutineScope()

    // Resume straight into Explore if already signed in; otherwise start at Welcome.
    val startDestination = remember { if (authRepository.currentUser() != null) "explore" else "welcome" }

    // Bottom bar is shown only on the two main tabs.
    val showBottomBar = currentRoute == "explore" || currentRoute == "profile"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                MosBottomBar(currentRoute) { route ->
                    navController.navigate(route) {
                        // Keep one copy of each tab, and remember Explore's map and routes.
                        popUpTo("explore") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
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

// ---------- Bottom navigation ----------

private data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab("explore", "Explore", Icons.Rounded.Map, Icons.Outlined.Map),
    BottomTab("profile", "Profile", Icons.Rounded.Person, Icons.Outlined.Person)
)

// Green bar with rounded top corners. The selected tab gets a filled icon on a light-green pill.
@Composable
private fun MosBottomBar(currentRoute: String?, onTabClick: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp
    ) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.primary, tonalElevation = 0.dp) {
            bottomTabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabClick(tab.route) },
                    icon = {
                        Icon(
                            if (selected) tab.selectedIcon else tab.icon,
                            contentDescription = tab.label
                        )
                    },
                    label = { Text(tab.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}
