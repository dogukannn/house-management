package com.dunettrpg.server.routes

import com.dunettrpg.server.data.repository.UserRepository
import com.dunettrpg.server.domain.service.AuthService
import com.dunettrpg.server.dto.request.LoginRequest
import com.dunettrpg.server.dto.request.RegisterDeviceRequest
import com.dunettrpg.server.dto.response.ApiResponse
import com.dunettrpg.server.dto.response.ErrorResponse
import com.dunettrpg.server.dto.response.LoginResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock

fun Route.authRoutes() {
    val authService = AuthService(UserRepository())
    val userRepository = UserRepository()
    
    route("/api/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            
            val result = authService.login(request.username, request.password)
            if (result != null) {
                val (token, userId) = result
                val user = userRepository.findById(userId)
                
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = LoginResponse(
                            token = token,
                            userId = userId,
                            username = user!!.username,
                            role = user.role
                        ),
                        timestamp = Clock.System.now().toString()
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(
                        success = false,
                        error = ErrorResponse(
                            code = "INVALID_CREDENTIALS",
                            message = "Invalid username or password"
                        ),
                        timestamp = Clock.System.now().toString()
                    )
                )
            }
        }
        
        authenticate("auth-jwt") {
            post("/register-device") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)
                
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }
                
                val request = call.receive<RegisterDeviceRequest>()
                authService.registerDevice(userId, request.fcmToken)
                
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = mapOf("message" to "Device registered successfully"),
                        timestamp = Clock.System.now().toString()
                    )
                )
            }
            
            delete("/logout") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)
                
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@delete
                }
                
                authService.logout(userId)
                
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = mapOf("message" to "Logged out successfully"),
                        timestamp = Clock.System.now().toString()
                    )
                )
            }
        }
    }
}
