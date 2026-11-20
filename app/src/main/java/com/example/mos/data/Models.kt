// OWNER: C — Models: Route, Risk, Incident, Stop, Place, User, TransportMode, RiskLevel
package com.example.mos.data

enum class TransportMode { WALK, BODA, MATATU, PRIVATE }

enum class RiskLevel { LOW, MODERATE, HIGH }

data class Risk(
    val overall: Double = 0.0,
    val accident: Double = 0.0,
    val flooding: Double = 0.0,
    val lighting: Double = 0.0
)

data class Stop(
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val time: String = "",
    val offsetMin: Int = 0,
    val status: RiskLevel = RiskLevel.LOW,
)

data class Incident(
    val id: String = "",
    val type: String = "",
    val description: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val title: String = "",
    val place: String = "",
    val minutesAgo: Int = 0,
    val severity: RiskLevel = RiskLevel.LOW
)

data class Route(
    val id: String = "",
    val name: String = "",
    val via: String = "",
    val durationMin: Int = 0,
    val distanceKm: Double = 0.0,
    val mode: TransportMode = TransportMode.WALK,
    val polyline: String = "",
    val risk: Risk = Risk(),
    val stops: List<Stop> = emptyList(),
    val incidents: List<Incident> = emptyList()
)

data class Place(
    val id: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = ""
)
