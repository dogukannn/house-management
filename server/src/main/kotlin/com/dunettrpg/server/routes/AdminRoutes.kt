package com.dunettrpg.server.routes

import com.dunettrpg.server.data.repository.EventRepository
import com.dunettrpg.server.data.repository.HouseRepository
import com.dunettrpg.server.domain.model.GameEventType
import com.dunettrpg.server.domain.service.EconomyService
import com.dunettrpg.server.dto.response.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class AdjustEconomyRequest(
    val houseId: String,
    val spiceAdjustment: Double? = null,
    val solarisAdjustment: Double? = null,
    val incomeAdjustment: Double? = null,
    val expenseAdjustment: Double? = null
)

@Serializable
data class AnnouncementRequest(
    val title: String,
    val message: String,
    val targetHouseIds: List<String> = emptyList(), // empty = all houses
    val priority: String = "NORMAL"
)

fun Route.adminRoutes() {
    val houseRepository = HouseRepository()
    val eventRepository = EventRepository()
    val economyService = EconomyService(houseRepository, eventRepository)
    
    authenticate("auth-jwt") {
        route("/api/admin") {
            // Note: In production, add role check to ensure user is admin
            
            // Trigger economy cycle
            post("/economy/cycle") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class) ?: ""
                    
                    val updates = economyService.executeCycle(userId)
                    
                    call.respond(
                        ApiResponse.success(
                            data = mapOf(
                                "message" to "Economy cycle completed",
                                "updates" to updates
                            ),
                            timestamp = Clock.System.now().toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Any>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Manual economy adjustment
            post("/economy/adjust") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class) ?: ""
                    
                    val request = call.receive<AdjustEconomyRequest>()
                    
                    val success = economyService.adjustEconomy(
                        houseId = request.houseId,
                        spiceAdjustment = request.spiceAdjustment,
                        solarisAdjustment = request.solarisAdjustment,
                        incomeAdjustment = request.incomeAdjustment,
                        expenseAdjustment = request.expenseAdjustment,
                        adminUserId = userId
                    )
                    
                    if (success) {
                        call.respond(
                            ApiResponse.success(
                                data = mapOf("message" to "Economy adjusted successfully"),
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse.error<Any>(
                                code = "HOUSE_NOT_FOUND",
                                message = "House not found",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Any>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Send announcement
            post("/announce") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class) ?: ""
                    
                    val request = call.receive<AnnouncementRequest>()
                    
                    val event = eventRepository.create(
                        type = GameEventType.ADMIN_ANNOUNCEMENT,
                        targetHouseIds = request.targetHouseIds,
                        payload = mapOf(
                            "title" to request.title,
                            "message" to request.message,
                            "priority" to request.priority
                        ),
                        requiresNotification = true,
                        createdBy = userId
                    )
                    
                    if (event != null) {
                        call.respond(
                            ApiResponse.success(
                                data = mapOf("message" to "Announcement sent successfully"),
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse.error<Any>(
                                code = "ANNOUNCEMENT_FAILED",
                                message = "Failed to send announcement",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Any>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Get game state snapshot
            get("/game-state") {
                try {
                    val houses = houseRepository.getAllHouses()
                    
                    call.respond(
                        ApiResponse.success(
                            data = mapOf(
                                "houses" to houses,
                                "timestamp" to Clock.System.now().toString()
                            ),
                            timestamp = Clock.System.now().toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Any>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
        }
    }
}
