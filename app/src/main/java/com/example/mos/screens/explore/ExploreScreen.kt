// OWNER: B — ExploreScreen implementation with Google Maps full-screen search, OSRM routes, and dismissable error alerts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mos.data.FakeRiskRepository
import com.example.mos.data.FakeRouteRepository
import com.example.mos.data.OsrmRouteRepository
import com.example.mos.data.Place
import com.example.mos.data.Route
import com.example.mos.data.TransportMode
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * ExploreScreen displays a map-centric view where osmdroid MapView takes up the full screen background.
 * Tapping the top search bar opens a full-screen Google Maps style search page.
 * Displays dismissable warning alerts if routes cannot be found.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onRouteClick: (routeId: String) -> Unit,
    viewModel: ExploreViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val fakeRepo = FakeRouteRepository()
                val osrmRepo = OsrmRouteRepository(fallbackRepository = fakeRepo)
                return ExploreViewModel(osrmRepo, FakeRiskRepository()) as T
            }
        }
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    var isSearchFullScreen by remember { mutableStateOf(false) }

    // Permission launcher for location access (ACCESS_FINE_LOCATION & ACCESS_COARSE_LOCATION)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Location permission results handled
    }

    // Configure osmdroid User-Agent synchronously BEFORE MapView creation to comply with OSM Tile Usage Policy
    remember(context) {
        Configuration.getInstance().apply {
            userAgentValue = "MOS-NairobiSafetyNavigator/1.0 (${context.packageName}; contact@mos.app)"
        }
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

    // Remember MapView and forward screen lifecycle (onResume, onPause, onDetach)
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(-1.2864, 36.8172))
        }
    }

    // Add MyLocationNewOverlay and enable my location if permitted, or fall back to CBD
    remember(mapView, context, hasLocationPermission) {
        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
        if (hasLocationPermission) {
            locationOverlay.enableMyLocation()
            locationOverlay.enableFollowLocation()
        } else {
            mapView.controller.setCenter(GeoPoint(-1.2864, 36.8172))
            mapView.controller.setZoom(14.0)
        }
        mapView.overlays.add(locationOverlay)
        locationOverlay
    }

    // Update map overlays with route Polylines colored by risk.
    LaunchedEffect(mapView, uiState.routes, uiState.origin, uiState.destination) {
        mapView.overlays.removeAll { it is Polyline }

        uiState.routes.reversed().forEach { route ->
            val isSafest = route == uiState.routes.first()
            val polyline = Polyline().apply {
                val geoPoints = route.stops.map { stop -> GeoPoint(stop.lat, stop.lng) }
                if (geoPoints.size < 2) {
                    setPoints(
                        listOf(
                            GeoPoint(uiState.origin.lat, uiState.origin.lng),
                            GeoPoint(uiState.destination.lat, uiState.destination.lng)
                        )
                    )
                } else {
                    setPoints(geoPoints)
                }

                val colorInt = when {
                    route.risk.overall < 0.15 -> Color(0xFF2DB864).toArgb() // Safe green
                    route.risk.overall <= 0.30 -> Color(0xFFF9AB00).toArgb() // Caution amber
                    else -> Color(0xFFD93025).toArgb() // Risk red
                }
                outlinePaint.color = colorInt
                outlinePaint.strokeWidth = if (isSafest) 16f else 10f
            }
            mapView.overlays.add(polyline)
        }
        mapView.invalidate()
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Configuration.getInstance().userAgentValue =
                        "${context.packageName}/1.0 (contact@mos.app; MOS-NairobiSafetyNavigator)"
                    mapView.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
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

    // Main Layout: Map + Bottom Sheet
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
                        color = Color(0xFFD7F5E2)
                    ) {
                        Text(
                            text = "● Live Safety Updates",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2DB864),
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
                        Text("No routes available.")
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
            // Full screen osmdroid MapView
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView }
            )

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
                                text = "Nairobi Safety Navigator",
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🔍", style = MaterialTheme.typography.titleMedium)
                            Column {
                                Text(
                                    text = "${uiState.origin.name} → ${uiState.destination.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
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

            // OSM Copyright Attribution Badge at Bottom End
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ) {
                Text(
                    text = "© OpenStreetMap contributors",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
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

                        // Full Screen Search Fields
                        LocationDropdownCardContent(
                            uiState = uiState,
                            onOriginQueryChanged = { viewModel.searchOrigin(it) },
                            onOriginSelected = { viewModel.selectOrigin(it) },
                            onDestinationQueryChanged = { viewModel.searchDestination(it) },
                            onDestinationSelected = { viewModel.selectDestination(it) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDropdownCardContent(
    uiState: ExploreUiState,
    onOriginQueryChanged: (String) -> Unit,
    onOriginSelected: (Place) -> Unit,
    onDestinationQueryChanged: (String) -> Unit,
    onDestinationSelected: (Place) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Origin Search Box with OSM Nominatim auto-suggestions
        var originExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = originExpanded && uiState.originSearchResults.isNotEmpty(),
            onExpandedChange = { originExpanded = it }
        ) {
            OutlinedTextField(
                value = uiState.originQuery,
                onValueChange = { query ->
                    onOriginQueryChanged(query)
                    originExpanded = true
                },
                label = { Text("Origin (Search OSM Places)") },
                trailingIcon = {
                    if (uiState.isSearchingOrigin) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = originExpanded)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
            )
            ExposedDropdownMenu(
                expanded = originExpanded && uiState.originSearchResults.isNotEmpty(),
                onDismissRequest = { originExpanded = false }
            ) {
                uiState.originSearchResults.forEach { place ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(place.name, style = MaterialTheme.typography.bodyMedium)
                                Text("Lat: ${place.lat}, Lng: ${place.lng}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = {
                            onOriginSelected(place)
                            originExpanded = false
                        }
                    )
                }
            }
        }

        // Destination Search Box with OSM Nominatim auto-suggestions
        var destExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = destExpanded && uiState.destinationSearchResults.isNotEmpty(),
            onExpandedChange = { destExpanded = it }
        ) {
            OutlinedTextField(
                value = uiState.destinationQuery,
                onValueChange = { query ->
                    onDestinationQueryChanged(query)
                    destExpanded = true
                },
                label = { Text("Destination (Search OSM Places)") },
                trailingIcon = {
                    if (uiState.isSearchingDestination) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = destExpanded)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
            )
            ExposedDropdownMenu(
                expanded = destExpanded && uiState.destinationSearchResults.isNotEmpty(),
                onDismissRequest = { destExpanded = false }
            ) {
                uiState.destinationSearchResults.forEach { place ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(place.name, style = MaterialTheme.typography.bodyMedium)
                                Text("Lat: ${place.lat}, Lng: ${place.lng}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = {
                            onDestinationSelected(place)
                            destExpanded = false
                        }
                    )
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
                    color = if (route.risk.overall < 0.15) Color(0xFF2DB864) else Color(0xFFD93025)
                )
            }

            if (isSafest) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF2DB864)
                ) {
                    Text(
                        text = "SAFEST",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
