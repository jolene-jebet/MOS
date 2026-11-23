// OWNER: B — ExploreViewModel implementation with Google Geocoding place search & route error handling
package com.example.mos.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mos.data.AuthRepository
import com.example.mos.data.Place
import com.example.mos.data.RemoteRiskRepository
import com.example.mos.data.RiskRepository
import com.example.mos.data.Route
import com.example.mos.data.RouteRepository
import com.example.mos.data.TransportMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WaypointState(
    val place: Place? = null,
    val query: String = "",
    val searchResults: List<Place> = emptyList(),
    val isSearching: Boolean = false
)

data class ExploreUiState(
    val routes: List<Route> = emptyList(),
    val places: List<Place> = emptyList(),
    val originQuery: String = "",
    val destinationQuery: String = "",
    val originSearchResults: List<Place> = emptyList(),
    val destinationSearchResults: List<Place> = emptyList(),
    val waypointStates: List<WaypointState> = emptyList(),
    val selectedMode: TransportMode = TransportMode.WALK,
    val origin: Place? = null,
    val destination: Place? = null,
    val isLoading: Boolean = false,
    val isSearchingOrigin: Boolean = false,
    val isSearchingDestination: Boolean = false,
    val errorMessage: String? = null,
    val isCloudRiskActive: Boolean = false
) {
    val waypoints: List<Place> get() = waypointStates.mapNotNull { it.place }
}

class ExploreViewModel(
    private val routeRepository: RouteRepository,
    private val riskRepository: RiskRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ExploreUiState(
            isCloudRiskActive = riskRepository is RemoteRiskRepository
        )
    )
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var originSearchJob: Job? = null
    private var destinationSearchJob: Job? = null
    private var routesJob: Job? = null

    init {
        val isCloud = riskRepository is RemoteRiskRepository
        _uiState.update { it.copy(isCloudRiskActive = isCloud) }
        loadPresetPlaces()
        loadDefaultMode()
    }

    // Starts Explore on the transport mode saved in the user's profile (if it loads).
    private fun loadDefaultMode() {
        viewModelScope.launch {
            authRepository.getProfile().onSuccess { setTransportMode(it.defaultMode) }
        }
    }

    private fun loadPresetPlaces() {
        viewModelScope.launch {
            try {
                val presets = routeRepository.getPlaces()
                _uiState.update {
                    it.copy(
                        places = presets,
                        originSearchResults = presets,
                        destinationSearchResults = presets
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun searchOrigin(query: String) {
        _uiState.update { it.copy(originQuery = query, isSearchingOrigin = true) }
        originSearchJob?.cancel()
        originSearchJob = viewModelScope.launch {
            delay(400)
            try {
                val results = routeRepository.searchPlaces(query)
                _uiState.update { it.copy(originSearchResults = results, isSearchingOrigin = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                val fallback = _uiState.value.places.filter { it.name.contains(query, ignoreCase = true) }
                _uiState.update { it.copy(originSearchResults = fallback, isSearchingOrigin = false) }
            }
        }
    }

    fun searchDestination(query: String) {
        _uiState.update { it.copy(destinationQuery = query, isSearchingDestination = true) }
        destinationSearchJob?.cancel()
        destinationSearchJob = viewModelScope.launch {
            delay(400)
            try {
                val results = routeRepository.searchPlaces(query)
                _uiState.update { it.copy(destinationSearchResults = results, isSearchingDestination = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                val fallback = _uiState.value.places.filter { it.name.contains(query, ignoreCase = true) }
                _uiState.update { it.copy(destinationSearchResults = fallback, isSearchingDestination = false) }
            }
        }
    }

    fun addWaypointSlot() {
        val presets = _uiState.value.places
        val newStates = _uiState.value.waypointStates + WaypointState(searchResults = presets)
        _uiState.update { it.copy(waypointStates = newStates) }
    }

    fun removeWaypointSlot(index: Int) {
        val current = _uiState.value.waypointStates.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.update { it.copy(waypointStates = current) }
            if (_uiState.value.origin != null && _uiState.value.destination != null) {
                loadRoutes()
            }
        }
    }

    fun searchWaypoint(index: Int, query: String) {
        val current = _uiState.value.waypointStates.toMutableList()
        if (index !in current.indices) return
        current[index] = current[index].copy(query = query, isSearching = true)
        _uiState.update { it.copy(waypointStates = current) }

        viewModelScope.launch {
            delay(400)
            try {
                val results = routeRepository.searchPlaces(query)
                val updated = _uiState.value.waypointStates.toMutableList()
                if (index in updated.indices) {
                    updated[index] = updated[index].copy(searchResults = results, isSearching = false)
                    _uiState.update { it.copy(waypointStates = updated) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                val fallback = _uiState.value.places.filter { it.name.contains(query, ignoreCase = true) }
                val updated = _uiState.value.waypointStates.toMutableList()
                if (index in updated.indices) {
                    updated[index] = updated[index].copy(searchResults = fallback, isSearching = false)
                    _uiState.update { it.copy(waypointStates = updated) }
                }
            }
        }
    }

    fun selectWaypoint(index: Int, place: Place) {
        val current = _uiState.value.waypointStates.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(place = place, query = place.name, searchResults = emptyList())
            _uiState.update { it.copy(waypointStates = current) }
            if (_uiState.value.origin != null && _uiState.value.destination != null) {
                loadRoutes()
            }
        }
    }

    fun setTransportMode(mode: TransportMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        if (_uiState.value.origin != null && _uiState.value.destination != null) {
            loadRoutes()
        }
    }

    fun selectOrigin(place: Place) {
        _uiState.update { it.copy(origin = place, originQuery = place.name) }
        if (_uiState.value.destination != null) {
            loadRoutes()
        }
    }

    fun selectDestination(place: Place) {
        _uiState.update { it.copy(destination = place, destinationQuery = place.name) }
        if (_uiState.value.origin != null) {
            loadRoutes()
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadRoutes() {
        val origin = _uiState.value.origin ?: return
        val destination = _uiState.value.destination ?: return
        val waypoints = _uiState.value.waypoints

        routesJob?.cancel()
        routesJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val currentMode = _uiState.value.selectedMode
                val fetchedRoutes = routeRepository.getRoutes(origin, destination, currentMode, waypoints)

                if (fetchedRoutes.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            routes = emptyList(),
                            isLoading = false,
                            errorMessage = "A route could not be found between ${origin.name} and ${destination.name}. Please try selecting different locations or transport mode."
                        )
                    }
                } else {
                    val scoredRoutes = coroutineScope {
                        fetchedRoutes
                            .map { route -> async { route.copy(risk = riskRepository.getRisk(route, route.mode)) } }
                            .awaitAll()
                    }.sortedBy { it.risk.overall }

                    _uiState.update {
                        it.copy(routes = scoredRoutes, isLoading = false, errorMessage = null)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        routes = emptyList(),
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "A route could not be found. Please check your network or try again."
                    )
                }
            }
        }
    }
}
