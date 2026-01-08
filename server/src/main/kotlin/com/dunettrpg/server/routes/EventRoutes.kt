package com.dunettrpg.server.routes

import com.dunettrpg.server.data.repository.EventRepository
import com.dunettrpg.server.domain.model.GameEvent
import com.dunettrpg.server.dto.response.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock

fun Route.eventRoutes() {
    val eventRepository = EventRepository()
    
    authenticate("auth-jwt") {
        route("/api/events") {
            // Get event feed
            get {
                try {
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                    val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0
                    val houseId = call.request.queryParameters["houseId"]
                    
                    val events = if (houseId != null) {
                        eventRepository.getForHouse(houseId, limit)
                    } else {
                        eventRepository.getAll(limit, offset)
                    }
                    
                    call.respond(
                        ApiResponse.success(
                            data = events,
                            timestamp = Clock.System.now().toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<GameEvent>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Get unread events count (placeholder - requires tracking read status)
            get("/unread") {
                try {
                    // Placeholder implementation
                    call.respond(
                        ApiResponse.success(
                            data = mapOf("count" to 0),
                            timestamp = Clock.System.now().toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<GameEvent>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Mark events as read (placeholder)
            post("/mark-read") {
                try {
                    // Placeholder implementation
                    call.respond(
                        ApiResponse.success(
                            data = mapOf("message" to "Events marked as read"),
                            timestamp = Clock.System.now().toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<GameEvent>(
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
