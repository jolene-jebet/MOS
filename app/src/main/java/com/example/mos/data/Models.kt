// OWNER: C — TODO
package com.example.mos.data

// TODO: adjust the fields to match fake_data.json and the real APIs.

enum class TransportMode { WALK, BODA, MATATU, PRIVATE }

data class Risk(
    val overall: Double = 0.0,
    val accident: Double = 0.0,
    val flooding: Double = 0.0,
    val lighting: Double = 0.0
)

data class Stop(
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class Route(
    val id: String = "",
    val name: String = "",
    val mode: TransportMode = TransportMode.WALK,
    val stops: List<Stop> = emptyList(),
    val risk: Risk = Risk()
)

data class Incident(
    val id: String = "",
    val type: String = "",
    val description: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
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
    val email: String = ""
)
