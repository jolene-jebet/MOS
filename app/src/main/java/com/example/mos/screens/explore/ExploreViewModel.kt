// OWNER: B — ExploreViewModel implementation with Google Geocoding place search & route error handling
package com.example.mos.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mos.data.AuthRepository
import com.example.mos.data.Place
import com.example.mos.data.RiskRepository
import com.example.mos.data.Route
import com.example.mos.data.RouteRepository
import com.example.mos.data.TransportMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreUiState(
    val routes: List<Route> = emptyList(),
    val places: List<Place> = emptyList(),
    val originQuery: String = "",
    val destinationQuery: String = "",
    val originSearchResults: List<Place> = emptyList(),
    val destinationSearchResults: List<Place> = emptyList(),
    val selectedMode: TransportMode = TransportMode.WALK,
    val origin: Place? = null,
    val destination: Place? = null,
    val isLoading: Boolean = false,
    val isSearchingOrigin: Boolean = false,
    val isSearchingDestination: Boolean = false,
    val errorMessage: String? = null
)

class ExploreViewModel(
    private val routeRepository: RouteRepository,
    private val riskRepository: RiskRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var originSearchJob: Job? = null
    private var destinationSearchJob: Job? = null
    private var routesJob: Job? = null

    init {
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

        routesJob?.cancel()
        routesJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val currentMode = _uiState.value.selectedMode
                val fetchedRoutes = routeRepository.getRoutes(origin, destination, currentMode)

                if (fetchedRoutes.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            routes = emptyList(),
                            isLoading = false,
                            errorMessage = "A route could not be found between ${origin.name} and ${destination.name}. Please try selecting different locations or transport mode."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            routes = fetchedRoutes,
                            isLoading = false,
                            errorMessage = null
                        )
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