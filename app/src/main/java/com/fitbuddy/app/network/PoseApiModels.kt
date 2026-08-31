package com.fitbuddy.app.network

data class PoseImageRequest(
    val image_base64: String
)

data class PosePointDto(
    val id: Int,
    val x: Float,
    val y: Float,
    val score: Float
)

data class PoseImageResponse(
    val knee_angle: Float,
    val hip_angle: Float,
    val torso_tilt: Float,
    val feedback: String,
    val keypoints: List<PosePointDto>? = null
)
