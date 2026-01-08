package com.dunettrpg.data.remote.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterDeviceRequest(
    val fcmToken: String
)
