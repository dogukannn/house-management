package com.dunettrpg.data.repository

import com.dunettrpg.data.local.TokenManager
import com.dunettrpg.data.remote.api.AuthApi
import com.dunettrpg.data.remote.dto.request.LoginRequest
import com.dunettrpg.data.remote.dto.request.RegisterDeviceRequest
import com.dunettrpg.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {
    
    suspend fun login(username: String, password: String): Result<User> {
        return try {
            val response = authApi.login(LoginRequest(username, password))
            
            if (response.isSuccessful && response.body()?.success == true) {
                val loginData = response.body()!!.data!!
                tokenManager.saveToken(loginData.token)
                
                Result.success(
                    User(
                        id = loginData.userId,
                        username = loginData.username,
                        role = loginData.role
                    )
                )
            } else {
                val errorMessage = response.body()?.error?.message ?: "Login failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun registerDevice(fcmToken: String): Result<Unit> {
        return try {
            val response = authApi.registerDevice(RegisterDeviceRequest(fcmToken))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to register device"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun logout(): Result<Unit> {
        return try {
            authApi.logout()
            tokenManager.clearToken()
            Result.success(Unit)
        } catch (e: Exception) {
            tokenManager.clearToken()
            Result.success(Unit)
        }
    }
    
    fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null
    }
}
