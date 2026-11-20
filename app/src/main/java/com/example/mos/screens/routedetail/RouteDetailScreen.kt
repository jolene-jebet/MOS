// OWNER: C — RouteDetailScreen: ETA, Safety Breakdown, Active Incidents, Stops timeline
package com.example.mos.screens.routedetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mos.data.Incident
import com.example.mos.data.Risk
import com.example.mos.data.RiskLevel
import com.example.mos.data.Route
import com.example.mos.data.Stop
import com.example.mos.ui.RiskBar
import com.example.mos.ui.RiskChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    viewModel: RouteDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Route Detail", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is RouteDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is RouteDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is RouteDetailUiState.Success -> {
                RouteDetailContent(
                    route = state.route,
                    risk = state.risk,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun RouteDetailContent(route: Route, risk: Risk, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { EtaCard(route = route, risk = risk) }
        item { SafetyBreakdownSection(risk = risk) }
        item { ActiveIncidentsSection(incidents = route.incidents) }
        item { StopsSection(stops = route.stops) }
    }
}

@Composable
private fun EtaCard(route: Route, risk: Risk) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${route.durationMin} min",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${"%.1f".format(route.distanceKm)} km • ${route.mode.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // A's RiskChip (ui/Components.kt) — currently an empty TODO, renders nothing yet.
            RiskChip(risk = risk)
        }
    }
}

@Composable
private fun SafetyBreakdownSection(risk: Risk) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(title = "Safety Breakdown")
        // A's RiskBar (ui/Components.kt) — currently an empty TODO, renders nothing yet.
        RiskBar(risk = risk)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RiskFactorRow(icon = Icons.Default.Warning, label = "Accident Risk", value = risk.accident)
            RiskFactorRow(icon = Icons.Default.Opacity, label = "Flooding Risk", value = risk.flooding)
            RiskFactorRow(icon = Icons.Default.Brightness2, label = "Lighting (Night Safety)", value = risk.lighting)
        }
    }
}

@Composable
private fun ActiveIncidentsSection(incidents: List<Incident>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Active Incidents Nearby")
        if (incidents.isEmpty()) {
            Text(
                text = "No incidents reported",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                incidents.forEach { incident -> IncidentCard(incident = incident) }
            }
        }
    }
}

@Composable
private fun StopsSection(stops: List<Stop>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Stops Along Route")
        // Google routes decode to raw polyline points named "Waypoint" — not real checkpoints.
        val namedStops = stops.filter { it.name != "Waypoint" }
        val displayStops = if (namedStops.isEmpty()) {
            listOf(Stop(name = "Start"), Stop(name = "Destination"))
        } else {
            namedStops
        }
        StopsTimeline(stops = displayStops)
    }
}

// ---------- Placeholders ----------
// These don't exist yet in ui/Components.kt (A's file): SectionHeader, RiskFactorRow,
// IncidentCard, StopsTimeline. RiskChip and RiskBar already exist there (as empty
// @Composable TODOs) and are called directly above instead of being re-implemented here.
//
// Note: DESIGN.md's RiskChip variants include label-only tags ("Safest", "Destination",
// "Slow", "Notice") that A's current RiskChip(risk: Risk) signature can't express — it only
// takes a percentage. So incident/stop status tags below use a small local badge instead of
// RiskChip; once A's RiskChip supports a label variant, these can switch to it.
// Also: no Safe/Caution/Risk/Info colour roles exist in MOSTheme yet, so colouring below is
// limited to neutral Material3 roles (primary/error/surfaceVariant) rather than true risk colours.

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun RiskFactorRow(
    icon: ImageVector,
    label: String,
    value: Double,
    modifier: Modifier = Modifier
) {
    val percent = (value * 100).toInt()
    val barColor = if (value > 0.30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = value.toFloat().coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun StatusBadge(text: String, emphasized: Boolean, modifier: Modifier = Modifier) {
    val containerColor = if (emphasized) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (emphasized) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(shape = RoundedCornerShape(50), color = containerColor, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun RiskLevel.label(): String = when (this) {
    RiskLevel.LOW -> "Low"
    RiskLevel.MODERATE -> "Moderate"
    RiskLevel.HIGH -> "High"
}

@Composable
private fun IncidentCard(incident: Incident, modifier: Modifier = Modifier) {
    val title = incident.title.ifBlank { incident.type }
    val place = incident.place.ifBlank { incident.description }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (place.isNotBlank() || incident.minutesAgo > 0) {
                    Text(
                        text = "$place • ${incident.minutesAgo}m ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusBadge(text = incident.severity.label(), emphasized = incident.severity == RiskLevel.HIGH)
        }
    }
}

@Composable
private fun StopsTimeline(stops: List<Stop>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        stops.forEachIndexed { index, stop ->
            val isDestination = index == stops.lastIndex
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isDestination) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Destination",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    val dotColor = when (stop.status) {
                        RiskLevel.HIGH -> MaterialTheme.colorScheme.error
                        RiskLevel.MODERATE, RiskLevel.LOW -> MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stop.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (stop.time.isNotBlank()) {
                        Text(
                            text = if (stop.offsetMin != 0) "${stop.time} (+${stop.offsetMin}m)" else stop.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadge(
                    text = if (isDestination) "Destination" else stop.status.label(),
                    emphasized = !isDestination && stop.status == RiskLevel.HIGH
                )
            }
        }
    }
}
