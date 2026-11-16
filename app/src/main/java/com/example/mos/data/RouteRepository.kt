// OWNER: B — TODO
package com.example.mos.data

interface RouteRepository {
    suspend fun getRoutes(from: Place, to: Place, mode: TransportMode): List<Route>
    suspend fun getRoute(routeId: String): Route?
}

class FakeRouteRepository : RouteRepository {
    override suspend fun getRoutes(from: Place, to: Place, mode: TransportMode): List<Route> {
        // TODO: Directions API
        return emptyList()
    }

    override suspend fun getRoute(routeId: String): Route? {
        // TODO: Directions API
        return null
    }
}
