// OWNER: B — Explore and RouteRepository implementation
package com.example.mos.data

import android.content.Context
import com.google.gson.Gson

data class FakeDataRoot(
    val routes: List<Route> = emptyList(),
    val incidents: List<Incident> = emptyList(),
    val places: List<Place> = emptyList()
)

interface RouteRepository {
    suspend fun getRoutes(from: Place, to: Place, mode: TransportMode, waypoints: List<Place> = emptyList()): List<Route>
    suspend fun getRoute(routeId: String): Route?
    suspend fun getPlaces(): List<Place>
    suspend fun searchPlaces(query: String): List<Place>
}

class FakeRouteRepository(private val context: Context? = null) : RouteRepository {

    private val mockPlaces = listOf(
        Place("p1", "CBD Centre", -1.2921, 36.8219),
        Place("p2", "Westlands", -1.2670, 36.8110),
        Place("p3", "Kilimani", -1.2950, 36.7920),
        Place("p4", "Upper Hill", -1.2980, 36.8100),
        Place("p5", "Karen", -1.3200, 36.7090),
        Place("p6", "Eastleigh", -1.2730, 36.8500),
        Place("p7", "Kasarani", -1.2210, 36.8900),
        Place("p8", "Lavington", -1.2780, 36.7650),
        Place("p9", "South B", -1.3100, 36.8400),
        Place("p10", "Parklands", -1.2600, 36.8200)
    )

    private val mockRoutes = listOf(
        Route(
            id = "route-1",
            name = "Via Ngong Road (Safe Corridor)",
            mode = TransportMode.WALK,
            stops = listOf(
                Stop("CBD Centre", -1.2921, 36.8219),
                Stop("Kilimani Stage", -1.2950, 36.7920),
                Stop("Junction Mall", -1.2985, 36.7750)
            ),
            risk = Risk(overall = 0.12, accident = 0.10, flooding = 0.05, lighting = 0.15)
        ),
        Route(
            id = "route-2",
            name = "Via Valley Road Shortcut",
            mode = TransportMode.WALK,
            stops = listOf(
                Stop("CBD Centre", -1.2921, 36.8219),
                Stop("Upper Hill Junction", -1.2980, 36.8100),
                Stop("Hurlingham", -1.2940, 36.7900)
            ),
            risk = Risk(overall = 0.35, accident = 0.40, flooding = 0.20, lighting = 0.45)
        ),
        Route(
            id = "route-3",
            name = "Boda Express via Uhuru Highway",
            mode = TransportMode.BODA,
            stops = listOf(
                Stop("CBD Centre", -1.2921, 36.8219),
                Stop("Uhuru Highway Roundabout", -1.2880, 36.8230),
                Stop("Westlands", -1.2670, 36.8110)
            ),
            risk = Risk(overall = 0.28, accident = 0.45, flooding = 0.10, lighting = 0.20)
        ),
        Route(
            id = "route-4",
            name = "Matatu No. 24 via Waiyaki Way",
            mode = TransportMode.MATATU,
            stops = listOf(
                Stop("Bus Station", -1.2860, 36.8220),
                Stop("Museum Hill", -1.2750, 36.8150),
                Stop("Sarit Centre", -1.2630, 36.8020)
            ),
            risk = Risk(overall = 0.18, accident = 0.22, flooding = 0.15, lighting = 0.12)
        ),
        Route(
            id = "route-5",
            name = "Private Car Route via Express Way",
            mode = TransportMode.PRIVATE,
            stops = listOf(
                Stop("Mlolongo Toll", -1.3500, 36.9300),
                Stop("Nyayo Stadium", -1.3050, 36.8280),
                Stop("Westlands Exit", -1.2670, 36.8110)
            ),
            risk = Risk(overall = 0.08, accident = 0.09, flooding = 0.02, lighting = 0.05)
        )
    )

    private fun loadRoot(): FakeDataRoot? {
        if (context == null) return null
        return try {
            val json = context.assets.open("fake_data.json").bufferedReader().use { it.readText() }
            Gson().fromJson(json, FakeDataRoot::class.java)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getRoutes(from: Place, to: Place, mode: TransportMode, waypoints: List<Place>): List<Route> {
        val root = loadRoot()
        val routes = if (root != null && root.routes.isNotEmpty() && root.routes.any { it.name != "TODO" }) {
            root.routes
        } else {
            mockRoutes
        }
        return routes
            .filter { it.mode == mode || mode == TransportMode.WALK }
            .sortedBy { it.risk.overall }
    }

    override suspend fun getRoute(routeId: String): Route? {
        val root = loadRoot()
        val routes = if (root != null && root.routes.isNotEmpty() && root.routes.any { it.name != "TODO" }) {
            root.routes
        } else {
            mockRoutes
        }
        return routes.find { it.id == routeId }
    }

    override suspend fun getPlaces(): List<Place> {
        val root = loadRoot()
        if (root != null && root.places.isNotEmpty()) {
            return root.places
        }
        return mockPlaces
    }

    override suspend fun searchPlaces(query: String): List<Place> {
        val places = getPlaces()
        if (query.isBlank()) return places
        val filtered = places.filter { it.name.contains(query, ignoreCase = true) }
        return if (filtered.isNotEmpty()) filtered else places
    }
}
