package com.dunettrpg.server.routes

import com.dunettrpg.server.data.repository.HouseRepository
import com.dunettrpg.server.data.repository.UserRepository
import com.dunettrpg.server.domain.model.UserRole
import com.dunettrpg.server.domain.service.EconomyService
import com.dunettrpg.server.dto.response.ApiResponses
import com.dunettrpg.server.util.PasswordHasher
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.adminRoutes() {
    route("/api/admin") {
        authenticate {
            // Middleware to check admin role
            intercept(ApplicationCallPipeline.Call) {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                
                if (role != "ADMIN") {
                    call.respond(HttpStatusCode.Forbidden, ApiResponses.error<Unit>("FORBIDDEN", "Admin access required"))
                    finish()
                }
            }
            
            // GET /api/admin/game-state - Get full game state snapshot
            get("/game-state") {
                val houses = HouseRepository.getAllHouses()
                val users = UserRepository.getAllUsers()
                
                val gameState = mapOf(
                    "houses" to houses,
                    "users" to users.map { user ->
                        mapOf(
                            "id" to user.id,
                            "username" to user.username,
                            "role" to user.role,
                            "houseId" to user.houseId,
                            "lastActiveAt" to user.lastActiveAt
                        )
                    },
                    "timestamp" to kotlinx.datetime.Clock.System.now().toString()
                )
                
                call.respond(HttpStatusCode.OK, ApiResponses.success(gameState))
            }
            
            // POST /api/admin/economy/cycle - Trigger economy cycle manually
            post("/economy/cycle") {
                val results = EconomyService.executeEconomyCycle()
                
                call.respond(HttpStatusCode.OK, ApiResponses.success(mapOf(
                    "message" to "Economy cycle completed",
                    "results" to results
                )))
            }
            
            // POST /api/admin/economy/adjust - Manual economy adjustment
            post("/economy/adjust") {
                val request = try {
                    call.receive<AdjustEconomyRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error<Unit>("INVALID_REQUEST", "Invalid request format"))
                    return@post
                }
                
                val house = EconomyService.adjustHouseEconomy(
                    houseId = request.houseId,
                    spiceChange = request.spiceChange,
                    solarisChange = request.solarisChange,
                    reason = request.reason
                )
                
                if (house == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponses.error<Unit>("NOT_FOUND", "House not found"))
                    return@post
                }
                
                call.respond(HttpStatusCode.OK, ApiResponses.success(house))
            }
            
            // GET /api/admin/users - List all users
            get("/users") {
                val users = UserRepository.getAllUsers()
                val safeUsers = users.map { user ->
                    mapOf(
                        "id" to user.id,
                        "username" to user.username,
                        "role" to user.role,
                        "houseId" to user.houseId,
                        "fcmToken" to if (user.fcmToken != null) "[SET]" else null,
                        "createdAt" to user.createdAt,
                        "lastActiveAt" to user.lastActiveAt
                    )
                }
                
                call.respond(HttpStatusCode.OK, ApiResponses.success(safeUsers))
            }
            
            // POST /api/admin/users - Create new user
            post("/users") {
                val request = try {
                    call.receive<CreateUserRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error<Unit>("INVALID_REQUEST", "Invalid request format"))
                    return@post
                }
                
                // Check if username already exists
                val existingUser = UserRepository.findByUsername(request.username)
                if (existingUser != null) {
                    call.respond(HttpStatusCode.Conflict, ApiResponses.error<Unit>("USERNAME_EXISTS", "Username already exists"))
                    return@post
                }
                
                // Validate house exists if houseId provided
                if (request.houseId != null) {
                    val house = HouseRepository.getHouseById(request.houseId)
                    if (house == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponses.error<Unit>("INVALID_HOUSE", "House not found"))
                        return@post
                    }
                }
                
                // Create user
                val passwordHash = PasswordHasher.hash(request.password)
                val user = UserRepository.createUser(
                    username = request.username,
                    passwordHash = passwordHash,
                    role = request.role,
                    houseId = request.houseId
                )
                
                val safeUser = mapOf(
                    "id" to user.id,
                    "username" to user.username,
                    "role" to user.role,
                    "houseId" to user.houseId,
                    "createdAt" to user.createdAt
                )
                
                call.respond(HttpStatusCode.Created, ApiResponses.success(safeUser))
            }
            
            // POST /api/admin/announce - Send announcement to all users
            post("/announce") {
                val request = try {
                    call.receive<AnnouncementRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error<Unit>("INVALID_REQUEST", "Invalid request format"))
                    return@post
                }
                
                // TODO: Implement actual notification broadcasting via FCM
                // For now, just return acknowledgment that request was received
                
                val announcement = mapOf(
                    "title" to request.title,
                    "message" to request.message,
                    "priority" to request.priority,
                    "receivedAt" to kotlinx.datetime.Clock.System.now().toString(),
                    "status" to "QUEUED",
                    "note" to "Notification system not yet implemented. This announcement was not sent."
                )
                
                call.respond(HttpStatusCode.Accepted, ApiResponses.success(announcement))
            }
            
            // DELETE /api/admin/users/{id} - Delete user (optional)
            delete("/users/{id}") {
                val userId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error<Unit>("MISSING_ID", "User ID is required"))
                    return@delete
                }
                
                val deleted = UserRepository.deleteUser(userId)
                if (!deleted) {
                    call.respond(HttpStatusCode.NotFound, ApiResponses.error<Unit>("NOT_FOUND", "User not found"))
                    return@delete
                }
                
                call.respond(HttpStatusCode.OK, ApiResponses.success(mapOf("message" to "User deleted successfully")))
            }
            
            // POST /api/admin/reset - Reset game state (DANGEROUS)
            post("/reset") {
                val request = try {
                    call.receive<ResetGameRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error<Unit>("INVALID_REQUEST", "Invalid request format"))
                    return@post
                }
                
                if (request.confirmationCode != "RESET_DUNE_GAME") {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error<Unit>("INVALID_CONFIRMATION", "Invalid confirmation code"))
                    return@post
                }
                
                // TODO: Implement actual game state reset
                // This should clear all votes, trades, and reset house economies
                // For now, just return a warning
                
                call.respond(HttpStatusCode.OK, ApiResponses.success(mapOf(
                    "message" to "Game reset not fully implemented - placeholder response",
                    "warning" to "This would reset all game state in production"
                )))
            }
        }
    }
}

@Serializable
data class AdjustEconomyRequest(
    val houseId: String,
    val spiceChange: Double? = null,
    val solarisChange: Double? = null,
    val reason: String
)

@Serializable
data class CreateUserRequest(
    val username: String,
    val password: String,
    val role: UserRole,
    val houseId: String? = null
)

@Serializable
data class AnnouncementRequest(
    val title: String,
    val message: String,
    val priority: String = "NORMAL" // LOW, NORMAL, HIGH, URGENT
)

@Serializable
data class ResetGameRequest(
    val confirmationCode: String
)
