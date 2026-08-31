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

    @POST("/facility/nearby") // <--- 경로를 복수형에서 단수형으로 수정했습니다.
    suspend fun getNearbyFacilities(
        @Body req: FacilityRequest
    ): List<FacilityDto>
}

// ===================================
// 포즈 분석 API 관련 데이터 클래스
// ===================================

data class PosePointDto(
    val id: Int,
    val x: Float,
    val y: Float,
    val score: Float
)

data class PoseImageRequest(
    val image_base64: String
)

data class PoseImageResponse(
    val keypoints: List<PosePointDto>,
    val knee_angle: Float,
    val hip_angle: Float,
    val torso_tilt: Float,
    val feedback: String
)

// ===================================
// 주변 시설 API 관련 데이터 클래스
// ===================================

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
