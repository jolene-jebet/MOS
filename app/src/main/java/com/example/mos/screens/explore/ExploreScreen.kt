// OWNER: B — ExploreScreen implementation with Google Maps full-screen search, Google Routes API routes, and dismissable error alerts
package com.example.mos.screens.explore

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mos.data.level
import com.example.mos.ui.theme.Safe
import com.example.mos.ui.theme.SafeContainer
import com.example.mos.ui.theme.color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mos.data.AuthRepository
import com.example.mos.data.Place
import com.example.mos.data.RiskRepository
import com.example.mos.data.Route
import com.example.mos.data.RouteRepository
import com.example.mos.data.TransportMode
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CancellationException

/**
 * ExploreScreen displays a map-centric view powered by Google Maps Compose.
 * Features a compact search bar opening a full-screen search overlay with real-time Google Geocoding place suggestions,
 * Google Directions API routes sorted safest first, and dismissable error alerts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onRouteClick: (routeId: String) -> Unit,
    routeRepository: RouteRepository,
    riskRepository: RiskRepository,
    authRepository: AuthRepository,
    viewModel: ExploreViewModel = rememberExploreViewModel(routeRepository, riskRepository, authRepository)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var isSearchFullScreen by remember { mutableStateOf(false) }

    // Permission launcher for location access (ACCESS_FINE_LOCATION & ACCESS_COARSE_LOCATION)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Location permission results handled
    }

    // Request location permissions on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Check if location permission is granted
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Camera position state (Nairobi CBD center default)
    val defaultLocation = LatLng(-1.2864, 36.8172)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    // Animate camera to destination or origin whenever a place is searched or selected
    LaunchedEffect(uiState.origin, uiState.destination) {
        val targetPlace = uiState.destination ?: uiState.origin
        targetPlace?.let {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                    LatLng(it.lat, it.lng),
                    14.5f
                )
            )
        }
    }

    // When routes arrive, zoom the map so the whole route is visible
    val density = LocalDensity.current
    LaunchedEffect(uiState.routes) {
        val points = uiState.routes.flatMap { it.stops }
        if (points.size >= 2) {
            val bounds = LatLngBounds.builder().apply {
                points.forEach { include(LatLng(it.lat, it.lng)) }
            }.build()
            val padding = with(density) { 120.dp.roundToPx() }
            try {
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, padding)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: IllegalStateException) {
                // Map not laid out yet; keep the current camera.
            }
        }
    }

    // Dismissable Warning Alert if a route cannot be found
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = {
                Text(
                    text = "Route Not Found",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = uiState.errorMessage ?: "A route could not be found between the selected locations.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    val scaffoldState = rememberBottomSheetScaffoldState()

    // Main Layout: Google Map + Bottom Sheet
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 120.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUGGESTED ROUTES (SAFEST FIRST)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SafeContainer
                    ) {
                        Text(
                            text = "● Live Safety Updates",
                            style = MaterialTheme.typography.labelSmall,
                            color = Safe,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.routes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Search for a starting point and destination above to view safe routes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        items(uiState.routes) { route ->
                            RouteCardItem(
                                route = route,
                                isSafest = route == uiState.routes.first(),
                                onClick = { onRouteClick(route.id) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // Full screen map container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Full screen Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
            ) {
                // Origin Marker
                uiState.origin?.let { origin ->
                    val originState = remember(origin.lat, origin.lng) { MarkerState(position = LatLng(origin.lat, origin.lng)) }
                    Marker(
                        state = originState,
                        title = origin.name,
                        snippet = "Origin"
                    )
                }

                // Destination Marker
                uiState.destination?.let { dest ->
                    val destState = remember(dest.lat, dest.lng) { MarkerState(position = LatLng(dest.lat, dest.lng)) }
                    Marker(
                        state = destState,
                        title = dest.name,
                        snippet = "Destination"
                    )
                }

                // Route Polylines (safest route rendered last and thicker with higher zIndex)
                uiState.routes.reversed().forEach { route ->
                    if (route.stops.size >= 2) {
                        val isSafest = route == uiState.routes.first()
                        val points = route.stops.map { LatLng(it.lat, it.lng) }
                        val color = route.risk.level().color()
                        Polyline(
                            points = points,
                            color = color,
                            width = if (isSafest) 16f else 10f,
                            zIndex = if (isSafest) 2f else 1f
                        )
                    }
                }
            }

            // Top Floating Header & Compact Search Bar Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row (App Title & Live Radar)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MOS",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "GPS Safe • Nairobi NW",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "● LIVE RADAR",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Compact Search Bar (Google Maps style)
                val searchLabel = if (uiState.origin != null && uiState.destination != null) {
                    "${uiState.origin?.name?.substringBefore(", ")} → ${uiState.destination?.name?.substringBefore(", ")}"
                } else {
                    "Search origin & destination..."
                }

                Surface(
                    onClick = { isSearchFullScreen = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🔍", style = MaterialTheme.typography.titleMedium)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = searchLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tap to search routes or change mode (${uiState.selectedMode.name})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "▶",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Full-Screen Google Maps Style Search Overlay Page
            AnimatedVisibility(
                visible = isSearchFullScreen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Top Bar with Back Arrow
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { isSearchFullScreen = false }) {
                                Text("←", style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Search & Directions",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Origin Search Box with Live Auto-suggestions
                        LocationSearchInputSection(
                            label = "Origin (Starting Location)",
                            query = uiState.originQuery,
                            searchResults = uiState.originSearchResults,
                            isSearching = uiState.isSearchingOrigin,
                            onQueryChanged = { viewModel.searchOrigin(it) },
                            onPlaceSelected = { viewModel.selectOrigin(it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Destination Search Box with Live Auto-suggestions
                        LocationSearchInputSection(
                            label = "Destination (Where to?)",
                            query = uiState.destinationQuery,
                            searchResults = uiState.destinationSearchResults,
                            isSearching = uiState.isSearchingDestination,
                            onQueryChanged = { viewModel.searchDestination(it) },
                            onPlaceSelected = { viewModel.selectDestination(it) }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "TRANSPORT MODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(TransportMode.entries) { mode ->
                                val isSelected = mode == uiState.selectedMode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setTransportMode(mode) },
                                    label = { Text(mode.name) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                viewModel.loadRoutes()
                                isSearchFullScreen = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Text("Show Routes on Map", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberExploreViewModel(
    routeRepository: RouteRepository,
    riskRepository: RiskRepository,
    authRepository: AuthRepository
): ExploreViewModel {
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExploreViewModel(routeRepository, riskRepository, authRepository) as T
            }
        }
    )
}

@Composable
fun LocationSearchInputSection(
    label: String,
    query: String,
    searchResults: List<Place>,
    isSearching: Boolean,
    onQueryChanged: (String) -> Unit,
    onPlaceSelected: (Place) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            label = { Text(label) },
            placeholder = { Text("Search location in Nairobi...") },
            leadingIcon = { Text("📍") },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Text("✕", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Live Location Suggestions Cards List
        if (searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(searchResults) { place ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaceSelected(place) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📍 ", style = MaterialTheme.typography.titleMedium)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = place.name.substringBefore(", "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = place.name.substringAfter(", ", "Nairobi, Kenya"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
fun RouteCardItem(
    route: Route,
    isSafest: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSafest) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mode: ${route.mode} • Stops: ${route.stops.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Overall Risk: ${(route.risk.overall * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = route.risk.level().color()
                )
            }

            if (isSafest) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Safe
                ) {
                    Text(
                        text = "SAFEST",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
