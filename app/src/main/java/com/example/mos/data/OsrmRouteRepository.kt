// OWNER: B — OsrmRouteRepository implementation supporting driving/foot profiles and Nominatim place search
package com.example.mos.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

// OSRM API Response DTOs
data class OsrmResponse(
    val routes: List<OsrmRoute> = emptyList()
)

data class OsrmRoute(
    val geometry: OsrmGeometry? = null,
    val distance: Double = 0.0, // distance in metres
    val duration: Double = 0.0  // duration in seconds
)

data class OsrmGeometry(
    val coordinates: List<List<Double>> = emptyList(), // [lng, lat] pairs (GeoJSON)
    val type: String = "LineString"
)

// Retrofit Service Interface supporting dynamic full URLs for driving and foot profiles
interface OsrmApiService {
    @GET
    suspend fun getRoutes(
        @Url url: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("alternatives") alternatives: Boolean = true
    ): OsrmResponse
}

// Nominatim Geocoding API DTOs & Service
data class NominatimPlace(
    val place_id: Long = 0,
    val display_name: String = "",
    val lat: String = "0.0",
    val lon: String = "0.0"
) {
    fun toPlace(): Place {
        val shortName = display_name.split(",").firstOrNull()?.trim() ?: display_name
        return Place(
            id = place_id.toString(),
            name = shortName,
            lat = lat.toDoubleOrNull() ?: 0.0,
            lng = lon.toDoubleOrNull() ?: 0.0
        )
    }
}

interface NominatimApiService {
    @GET("search")
    suspend fun searchPlaces(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 8,
        @Query("viewbox") viewbox: String = "36.65,-1.45,37.05,-1.15",
        @Query("bounded") bounded: Int = 1
    ): List<NominatimPlace>
}

/**
 * OsrmRouteRepository fetches real route geometry and alternatives from OSRM,
 * and searches real places from OpenStreetMap's Nominatim Geocoding API.
 */
class OsrmRouteRepository(
    private val apiService: OsrmApiService = Retrofit.Builder()
        .baseUrl("https://router.project-osrm.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OsrmApiService::class.java),
    private val nominatimApiService: NominatimApiService = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "MOS-NairobiSafetyNavigator/1.0 (contact@mos.app)")
                        .build()
                    chain.proceed(request)
                }
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NominatimApiService::class.java),
    private val fallbackRepository: RouteRepository = FakeRouteRepository()
) : RouteRepository {

    override suspend fun getRoutes(from: Place, to: Place, mode: TransportMode, waypoints: List<Place>): List<Route> {
        return try {
            // OSRM takes longitude first, then latitude: {lng1},{lat1};{lng2},{lat2}
            val coords = "${from.lng},${from.lat};${to.lng},${to.lat}"

            // Select profile endpoint based on transport mode
            val url = if (mode == TransportMode.WALK) {
                "https://routing.openstreetmap.de/routed-foot/route/v1/foot/$coords"
            } else {
                "https://router.project-osrm.org/route/v1/driving/$coords"
            }

            val response = apiService.getRoutes(url)

            if (response.routes.isEmpty()) {
                return emptyList()
            }

            // Map OSRM routes to MOS Route objects using GeoJSON [lng, lat] pairs directly
            response.routes.mapIndexed { index, osrmRoute ->
                val stops = osrmRoute.geometry?.coordinates?.map { coord ->
                    val lng = coord.getOrElse(0) { 0.0 }
                    val lat = coord.getOrElse(1) { 0.0 }
                    Stop(name = "Waypoint", lat = lat, lng = lng)
                } ?: emptyList()

                val distanceKm = osrmRoute.distance / 1000.0
                val durationMin = (osrmRoute.duration / 60.0).toInt()

                // Assign varied mock risk scores for alternative routes (safest first)
                val riskScore = 0.08 + (index * 0.12)
                Route(
                    id = "osrm-${mode.name.lowercase()}-$index",
                    name = "OSRM ${mode.name} Route ${index + 1} (${distanceKm.toInt()} km, $durationMin min)",
                    mode = mode,
                    stops = stops,
                    risk = Risk(
                        overall = riskScore,
                        accident = riskScore * 0.9,
                        flooding = riskScore * 0.3,
                        lighting = riskScore * 0.5
                    )
                )
            }.sortedBy { it.risk.overall }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getRoute(routeId: String): Route? {
        return fallbackRepository.getRoute(routeId)
    }

    override suspend fun getPlaces(): List<Place> {
        return fallbackRepository.getPlaces()
    }

    override suspend fun searchPlaces(query: String): List<Place> {
        if (query.isBlank()) return getPlaces()
        return try {
            val results = nominatimApiService.searchPlaces(query)
            if (results.isNotEmpty()) {
                results.map { it.toPlace() }
            } else {
                fallbackRepository.searchPlaces(query)
            }
        } catch (_: Exception) {
            fallbackRepository.searchPlaces(query)
        }
    }
}
