package com.dunettrpg.server.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null,
    val timestamp: String
) {
    companion object {
        fun <T> success(data: T, timestamp: String): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                timestamp = timestamp
            )
        }
        
        fun <T> error(code: String, message: String, timestamp: String, details: Map<String, String>? = null): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorResponse(code, message, details),
                timestamp = timestamp
            )
        }
    }
}

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

@Serializable
data class LoginResponse(
    val token: String,
    val userId: String,
    val username: String,
    val role: String
)
