package com.dunettrpg.data.remote.api

import com.dunettrpg.data.remote.dto.request.LoginRequest
import com.dunettrpg.data.remote.dto.request.RegisterDeviceRequest
import com.dunettrpg.data.remote.dto.response.ApiResponse
import com.dunettrpg.data.remote.dto.response.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface AuthApi {
    
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>
    
    @POST("/api/auth/register-device")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): Response<ApiResponse<Map<String, String>>>
    
    @DELETE("/api/auth/logout")
    suspend fun logout(): Response<ApiResponse<Map<String, String>>>
}
