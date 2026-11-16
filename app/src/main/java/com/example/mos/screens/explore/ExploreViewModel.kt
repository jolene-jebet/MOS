// OWNER: B — TODO
package com.example.mos.screens.explore

import androidx.lifecycle.ViewModel
import com.example.mos.data.RiskRepository
import com.example.mos.data.RouteRepository

class ExploreViewModel(
    private val routeRepository: RouteRepository,
    private val riskRepository: RiskRepository
) : ViewModel() {
    // TODO: UI state + load routes and rank them safest first
}
