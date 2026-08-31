package com.fitbuddy.app.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// --- 요청/응답 데이터 클래스 ---

data class SignupRequest(
    val email: String,
    val password: String,
    val name: String
)

data class SignupResponse(
    val success: Boolean,
    val message: String
)


data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String
)

data class UserInfoRequest(
    val email: String,
    val height_cm: Int,
    val weight_kg: Double,
    val gender: String,
    val workout_goal: String
)

data class UserInfoResponse(
    val success: Boolean,
    val message: String
)

interface ApiService {

    @GET("/")
    suspend fun ping(): Map<String, String>

    @POST("/signup")
    suspend fun signup(@Body body: SignupRequest): SignupResponse

    @POST("/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("/user/info")
    suspend fun updateUserInfo(
        @Body request: UserInfoRequest
    ): UserInfoResponse

    @POST("/pose/analyze")
    suspend fun analyzePose(
        @Body req: PoseImageRequest
    ): PoseImageResponse

    @POST("/facility/nearby")
    suspend fun getNearbyFacilities(
        @Body req: FacilityRequest
    ): List<FacilityDto>
}
