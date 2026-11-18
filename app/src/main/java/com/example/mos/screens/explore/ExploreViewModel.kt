// OWNER: B — ExploreViewModel implementation with OSM Nominatim place search & route error handling
package com.example.mos.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mos.data.Place
import com.example.mos.data.RiskRepository
import com.example.mos.data.Route
import com.example.mos.data.RouteRepository
import com.example.mos.data.TransportMode
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
    val originQuery: String = "CBD Centre",
    val destinationQuery: String = "Westlands",
    val originSearchResults: List<Place> = emptyList(),
    val destinationSearchResults: List<Place> = emptyList(),
    val selectedMode: TransportMode = TransportMode.WALK,
    val origin: Place = Place("p1", "CBD Centre", -1.2921, 36.8219),
    val destination: Place = Place("p2", "Westlands", -1.2670, 36.8110),
    val isLoading: Boolean = false,
    val isSearchingOrigin: Boolean = false,
    val isSearchingDestination: Boolean = false,
    val errorMessage: String? = null
)

class ExploreViewModel(
    private val routeRepository: RouteRepository,
    private val riskRepository: RiskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var originSearchJob: Job? = null
    private var destinationSearchJob: Job? = null

    init {
        loadPlacesAndRoutes()
    }

    fun loadPlacesAndRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val places = routeRepository.getPlaces()
                val defaultOrigin = places.getOrNull(0) ?: _uiState.value.origin
                val defaultDest = places.getOrNull(1) ?: _uiState.value.destination

                _uiState.update {
                    it.copy(
                        places = places,
                        originSearchResults = places,
                        destinationSearchResults = places,
                        origin = defaultOrigin,
                        destination = defaultDest,
                        originQuery = defaultOrigin.name,
                        destinationQuery = defaultDest.name
                    )
                }
                loadRoutes()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load places"
                    )
                }
            }
        }
    }

    fun searchOrigin(query: String) {
        _uiState.update { it.copy(originQuery = query, isSearchingOrigin = true) }
        originSearchJob?.cancel()
        originSearchJob = viewModelScope.launch {
            delay(300) // debounce
            try {
                val results = routeRepository.searchPlaces(query)
                _uiState.update { it.copy(originSearchResults = results, isSearchingOrigin = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSearchingOrigin = false) }
            }
        }
    }

    fun searchDestination(query: String) {
        _uiState.update { it.copy(destinationQuery = query, isSearchingDestination = true) }
        destinationSearchJob?.cancel()
        destinationSearchJob = viewModelScope.launch {
            delay(300) // debounce
            try {
                val results = routeRepository.searchPlaces(query)
                _uiState.update { it.copy(destinationSearchResults = results, isSearchingDestination = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSearchingDestination = false) }
            }
        }
    }

    fun setTransportMode(mode: TransportMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        loadRoutes()
    }

    fun selectOrigin(place: Place) {
        _uiState.update { it.copy(origin = place, originQuery = place.name) }
        loadRoutes()
    }

    fun selectDestination(place: Place) {
        _uiState.update { it.copy(destination = place, destinationQuery = place.name) }
        loadRoutes()
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val currentMode = _uiState.value.selectedMode
                val origin = _uiState.value.origin
                val destination = _uiState.value.destination

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
