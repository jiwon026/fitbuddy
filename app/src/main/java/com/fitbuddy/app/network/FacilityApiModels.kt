package com.fitbuddy.app.network

data class FacilityRequest(
    val user_lat: Double,
    val user_lon: Double,
    val radius_km: Double
)

data class FacilityDto(
    val name: String,
    val address: String,
    val distance_km: Double,
    val lat: Double,
    val lon: Double
)
