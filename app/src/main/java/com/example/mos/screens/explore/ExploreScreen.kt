// OWNER: B — TODO
package com.example.mos.screens.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ExploreScreen(onRouteClick: (routeId: String) -> Unit) {
    // TODO: map, search, mode chips, route list (safest first)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Explore")
        // Placeholder so the Route Detail screen can be reached.
        Button(onClick = { onRouteClick("route-1") }) { Text("Open route detail") }
    }
}
