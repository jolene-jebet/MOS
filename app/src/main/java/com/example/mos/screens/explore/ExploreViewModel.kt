// OWNER: B — ExploreViewModel implementation with places and dropdown support
package com.example.mos.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mos.data.Place
import com.example.mos.data.RiskRepository
import com.example.mos.data.Route
import com.example.mos.data.RouteRepository
import com.example.mos.data.TransportMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreUiState(
    val routes: List<Route> = emptyList(),
    val places: List<Place> = emptyList(),
    val selectedMode: TransportMode = TransportMode.WALK,
    val origin: Place = Place("p1", "CBD Centre", -1.2921, 36.8219),
    val destination: Place = Place("p2", "Westlands", -1.2670, 36.8110),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ExploreViewModel(
    private val routeRepository: RouteRepository,
    private val riskRepository: RiskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

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
                        origin = defaultOrigin,
                        destination = defaultDest
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

    fun setTransportMode(mode: TransportMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        loadRoutes()
    }

    fun setOrigin(place: Place) {
        _uiState.update { it.copy(origin = place) }
        loadRoutes()
    }

    fun setDestination(place: Place) {
        _uiState.update { it.copy(destination = place) }
        loadRoutes()
    }

    fun loadRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val currentMode = _uiState.value.selectedMode
                val origin = _uiState.value.origin
                val destination = _uiState.value.destination

                val fetchedRoutes = routeRepository.getRoutes(origin, destination, currentMode)

                _uiState.update {
                    it.copy(
                        routes = fetchedRoutes,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load routes"
                    )
                }
            }
        }
    }
}
