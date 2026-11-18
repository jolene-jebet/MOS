// OWNER: B — ExploreScreen implementation with Material 3 BottomSheetScaffold and osmdroid map
package com.example.mos.screens.explore

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
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
 * ExploreScreen displays the interactive osmdroid map view with risk-colored polylines,
 * location dropdown selection card, transport mode selector, and suggested routes ranked safest first
 * inside a Material 3 BottomSheetScaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onRouteClick: (routeId: String) -> Unit,
    viewModel: ExploreViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExploreViewModel(FakeRouteRepository(), FakeRiskRepository()) as T
            }
        }
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    // Permission launcher for location access (ACCESS_FINE_LOCATION & ACCESS_COARSE_LOCATION)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Location permission results handled
    }

    // Before the map loads, set osmdroid user agent and request location permissions.
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
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
    // Safest route is added last (via reversed iteration) so it sits on top, and rendered thicker.
    LaunchedEffect(mapView, uiState.routes, uiState.origin, uiState.destination) {
        mapView.overlays.removeAll { it is Polyline }

        // Iterate in reverse so the safest route (index 0) is added last and sits on top
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

                // Risk colour mapping according to DESIGN.md thresholds using .toArgb()
                val colorInt = when {
                    route.risk.overall < 0.15 -> Color(0xFF2DB864).toArgb() // Safe green
                    route.risk.overall <= 0.30 -> Color(0xFFF9AB00).toArgb() // Caution amber
                    else -> Color(0xFFD93025).toArgb() // Risk red
                }
                outlinePaint.color = colorInt
                // Safest route rendered thicker so it stands out and sits on top
                outlinePaint.strokeWidth = if (isSafest) 16f else 10f
            }
            mapView.overlays.add(polyline)
        }
        // Call mapView.invalidate() after changing overlays
        mapView.invalidate()
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
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

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 280.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.routes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No routes available for this mode.")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header / App Title & Live Radar Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Nairobi Safety Navigator",
                            style = MaterialTheme.typography.titleLarge,
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
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Location Card with two ExposedDropdownMenuBoxes (Origin & Destination)
                LocationDropdownCard(
                    uiState = uiState,
                    onOriginSelected = { viewModel.setOrigin(it) },
                    onDestinationSelected = { viewModel.setDestination(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Transport Mode Selection Row
                Text(
                    text = "TRANSPORT MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
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

                Spacer(modifier = Modifier.height(12.dp))

                // osmdroid MapView wrapped in AndroidView centred on Nairobi CBD with OSM copyright notice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { mapView }
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "© OpenStreetMap contributors",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDropdownCard(
    uiState: ExploreUiState,
    onOriginSelected: (Place) -> Unit,
    onDestinationSelected: (Place) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Origin Dropdown
            var originExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = originExpanded,
                onExpandedChange = { originExpanded = !originExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.origin.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Origin") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = originExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = originExpanded,
                    onDismissRequest = { originExpanded = false }
                ) {
                    uiState.places.forEach { place ->
                        DropdownMenuItem(
                            text = { Text(place.name) },
                            onClick = {
                                onOriginSelected(place)
                                originExpanded = false
                            }
                        )
                    }
                }
            }

            // Destination Dropdown
            var destExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = destExpanded,
                onExpandedChange = { destExpanded = !destExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.destination.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Destination") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = destExpanded,
                    onDismissRequest = { destExpanded = false }
                ) {
                    uiState.places.forEach { place ->
                        DropdownMenuItem(
                            text = { Text(place.name) },
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
