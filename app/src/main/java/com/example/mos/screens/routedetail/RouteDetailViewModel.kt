// OWNER: C — RouteDetailViewModel: loads the route + its risk by id
package com.example.mos.screens.routedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mos.data.Risk
import com.example.mos.data.RiskRepository
import com.example.mos.data.Route
import com.example.mos.data.RouteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RouteDetailUiState {
    data object Loading : RouteDetailUiState
    data class Success(val route: Route, val risk: Risk) : RouteDetailUiState
    data class Error(val message: String) : RouteDetailUiState
}

class RouteDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
    private val riskRepository: RiskRepository
) : ViewModel() {

    private val routeId: String = savedStateHandle.get<String>("routeId").orEmpty()

    private val _uiState = MutableStateFlow<RouteDetailUiState>(RouteDetailUiState.Loading)
    val uiState: StateFlow<RouteDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = RouteDetailUiState.Loading
            val route = routeRepository.getRoute(routeId)
            if (route == null) {
                _uiState.value = RouteDetailUiState.Error("Route not found.")
                return@launch
            }
            val risk = riskRepository.getRisk(route, route.mode)
            _uiState.value = RouteDetailUiState.Success(route, risk)
        }
    }
}
