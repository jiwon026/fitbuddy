data class FacilityRequest(
    val user_lat: Double,
    val user_lon: Double,
    val radius_km: Double = 3.0,
    val category: String? = null
)

data class FacilityDto(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distance_km: Double
)
