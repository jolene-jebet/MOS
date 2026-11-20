// OWNER: B — GoogleRouteRepository: Google Routes API (routes) + Places API (New) Text Search (place search). Uses ONE key: MAPS_API_KEY.
package com.example.mos.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.mos.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

// ---------- Shared ----------
data class GoogleLatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

// ---------- Routes API (computeRoutes) ----------
data class GoogleLocation(val latLng: GoogleLatLng)

data class GoogleWaypoint(val location: GoogleLocation)

data class ComputeRoutesRequest(
    val origin: GoogleWaypoint,
    val destination: GoogleWaypoint,
    val travelMode: String, // "DRIVE" or "WALK"
    val computeAlternativeRoutes: Boolean = true
)

data class ComputeRoutesResponse(
    val routes: List<GoogleApiRoute> = emptyList()
)

data class GoogleApiRoute(
    val description: String? = null,
    val distanceMeters: Int = 0,
    val duration: String? = null, // seconds as text, e.g. "1234s"
    val polyline: GooglePolyline? = null
)

data class GooglePolyline(
    val encodedPolyline: String = ""
)

// ---------- Places API (New) Text Search ----------
data class GoogleCircle(
    val center: GoogleLatLng = GoogleLatLng(-1.2864, 36.8172), // Nairobi CBD
    val radius: Double = 50000.0
)

data class GoogleLocationBias(
    val circle: GoogleCircle = GoogleCircle()
)

data class TextSearchRequest(
    val textQuery: String,
    val regionCode: String = "KE",
    val pageSize: Int = 8,
    val locationBias: GoogleLocationBias = GoogleLocationBias()
)

data class TextSearchResponse(
    val places: List<GooglePlaceResult> = emptyList()
)

data class GooglePlaceResult(
    val id: String = "",
    val displayName: GoogleText? = null,
    val formattedAddress: String = "",
    val location: GoogleLatLng? = null
)

data class GoogleText(
    val text: String = ""
)

// ---------- Retrofit ----------
interface GoogleMapsApiService {
    @POST("https://routes.googleapis.com/directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body body: ComputeRoutesRequest
    ): ComputeRoutesResponse

    @POST("https://places.googleapis.com/v1/places:searchText")
    suspend fun searchText(
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body body: TextSearchRequest
    ): TextSearchResponse
}

private const val TAG = "MOS-Google"
private const val ROUTES_FIELD_MASK =
    "routes.description,routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline"
private const val PLACES_FIELD_MASK =
    "places.id,places.displayName,places.formattedAddress,places.location"

/**
 * SHA-1 of the key that signed this app (hex, no colons).
 * Google needs it, with the package name, when the API key is restricted to "Android apps".
 */
@Suppress("DEPRECATION")
private fun signingSha1(context: Context): String? = try {
    val pm = context.packageManager
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            .signingInfo?.apkContentsSigners
    } else {
        pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
    }
    signatures?.firstOrNull()?.let { sig ->
        MessageDigest.getInstance("SHA-1").digest(sig.toByteArray())
            .joinToString("") { "%02X".format(it) }
    }
} catch (_: Exception) {
    null
}

private fun createGoogleMapsApiService(context: Context): GoogleMapsApiService {
    val appContext = context.applicationContext
    val packageName = appContext.packageName
    val sha1 = signingSha1(appContext)

    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                .header("X-Android-Package", packageName)
            if (sha1 != null) builder.header("X-Android-Cert", sha1)
            chain.proceed(builder.build())
        }
        .build()

    return Retrofit.Builder()
        .baseUrl("https://routes.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GoogleMapsApiService::class.java)
}

class GoogleRouteRepository(
    context: Context,
    private val fallbackRepository: RouteRepository = FakeRouteRepository(),
    private val apiService: GoogleMapsApiService = createGoogleMapsApiService(context)
) : RouteRepository {

    private val nairobiPresetPlaces = listOf(
        Place("p1", "CBD Centre", -1.2921, 36.8219),
        Place("p2", "Westlands", -1.2670, 36.8110),
        Place("p3", "Kilimani", -1.2950, 36.7920),
        Place("p4", "Upper Hill", -1.2980, 36.8100),
        Place("p5", "Junction Mall", -1.2985, 36.7750),
        Place("p6", "Sarit Centre", -1.2630, 36.8020),
        Place("p7", "Karen", -1.3200, 36.7090),
        Place("p8", "Lavington", -1.2780, 36.7650),
        Place("p9", "Eastleigh", -1.2730, 36.8500),
        Place("p10", "Kasarani", -1.2210, 36.8900)
    )

    // Last routes fetched, so Route Detail can find them by id.
    private val routeCache = ConcurrentHashMap<String, Route>()

    override suspend fun getRoutes(from: Place, to: Place, mode: TransportMode): List<Route> {
        val request = ComputeRoutesRequest(
            origin = from.toWaypoint(),
            destination = to.toWaypoint(),
            // Guide: Boda, Matatu and Private use the driving profile.
            travelMode = if (mode == TransportMode.WALK) "WALK" else "DRIVE"
        )

        val response = callGoogle("Routes API") {
            apiService.computeRoutes(ROUTES_FIELD_MASK, request)
        }

        val routes = response.routes.mapIndexedNotNull { index, googleRoute ->
            val points = decodePolyline(googleRoute.polyline?.encodedPolyline.orEmpty())
            // No real line = no route. We never draw a fake straight line.
            if (points.size < 2) return@mapIndexedNotNull null

            val distanceKm = googleRoute.distanceMeters / 1000.0
            val durationMin = ((googleRoute.duration?.removeSuffix("s")?.toDoubleOrNull() ?: 0.0) / 60.0).roundToInt()
            val label = googleRoute.description?.takeIf { it.isNotBlank() }?.let { "Via $it" } ?: "Route ${index + 1}"

            val riskScore = 0.08 + (index * 0.12)
            Route(
                id = "google-${mode.name.lowercase()}-$index",
                name = "$label (${"%.1f".format(distanceKm)} km, $durationMin min)",
                mode = mode,
                stops = points,
                risk = Risk(
                    overall = riskScore,
                    accident = riskScore * 0.9,
                    flooding = riskScore * 0.3,
                    lighting = riskScore * 0.5
                )
            )
        }.sortedBy { it.risk.overall }

        routeCache.clear()
        routes.forEach { routeCache[it.id] = it }
        return routes
    }

    override suspend fun getRoute(routeId: String): Route? {
        return routeCache[routeId] ?: fallbackRepository.getRoute(routeId)
    }

    override suspend fun getPlaces(): List<Place> {
        return nairobiPresetPlaces
    }

    override suspend fun searchPlaces(query: String): List<Place> {
        val q = query.trim()
        if (q.isEmpty()) return nairobiPresetPlaces
        if (q.length < 2) return nairobiPresetPlaces.filter { it.name.contains(q, ignoreCase = true) }

        val response = callGoogle("Places API (New)") {
            apiService.searchText(PLACES_FIELD_MASK, TextSearchRequest(textQuery = q))
        }
        return response.places.mapNotNull { it.toPlace() }.distinctBy { it.id }
    }

    // ---------- helpers ----------

    private fun Place.toWaypoint() = GoogleWaypoint(GoogleLocation(GoogleLatLng(lat, lng)))

    /** Name is "Title, address" so the list can show Google-Maps-style two lines. */
    private fun GooglePlaceResult.toPlace(): Place? {
        val loc = location ?: return null
        val title = displayName?.text.orEmpty().trim()
        val address = formattedAddress.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("Kenya", ignoreCase = true) }
            .joinToString(", ")
        val label = when {
            title.isEmpty() -> address
            address.isEmpty() -> title
            address.startsWith(title, ignoreCase = true) -> address
            else -> "$title, $address"
        }
        if (label.isBlank()) return null
        return Place(
            id = id.ifBlank { "${loc.latitude},${loc.longitude}" },
            name = label,
            lat = loc.latitude,
            lng = loc.longitude
        )
    }

    /** Runs a Google call. On failure, logs the real reason (Logcat tag MOS-Google) and throws a readable message. */
    private suspend fun <T> callGoogle(api: String, block: suspend () -> T): T {
        if (BuildConfig.MAPS_API_KEY.isBlank()) {
            throw IllegalStateException("No Google API key found. Add MAPS_API_KEY to local.properties.")
        }
        try {
            return block()
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string().orEmpty()
            Log.e(TAG, "$api failed: HTTP ${e.code()} $body")
            val reason = when (e.code()) {
                400 -> "Google could not read the request (HTTP 400)."
                401, 403 -> "Google blocked the request (HTTP ${e.code()}). Enable $api and allow it on your API key."
                429 -> "Too many requests. Wait a moment and try again."
                else -> "Google $api error (HTTP ${e.code()})."
            }
            throw IllegalStateException(reason, e)
        } catch (e: IOException) {
            Log.e(TAG, "$api network error: ${e.message}")
            throw IllegalStateException("No internet connection. Check your network and try again.", e)
        }
    }

    private fun decodePolyline(encoded: String): List<Stop> {
        val poly = ArrayList<Stop>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(Stop(name = "Waypoint", lat = lat / 1E5, lng = lng / 1E5))
        }
        return poly
    }
}