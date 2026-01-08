package com.dunettrpg.server.routes

import com.dunettrpg.server.data.repository.EventRepository
import com.dunettrpg.server.data.repository.VoteRepository
import com.dunettrpg.server.domain.model.Decision
import com.dunettrpg.server.domain.model.Vote
import com.dunettrpg.server.domain.model.VoteType
import com.dunettrpg.server.domain.service.VoteService
import com.dunettrpg.server.dto.response.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CreateVoteRequest(
    val type: VoteType,
    val title: String,
    val description: String,
    val initiatorHouseId: String,
    val requiredParticipants: List<String>,
    val consensusRequired: Boolean = false,
    val deadline: String? = null
)

@Serializable
data class CastVoteRequest(
    val houseId: String,
    val decision: Decision
)

fun Route.voteRoutes() {
    val voteRepository = VoteRepository()
    val eventRepository = EventRepository()
    val voteService = VoteService(voteRepository, eventRepository)
    
    authenticate("auth-jwt") {
        route("/api/votes") {
            // List all votes
            get {
                try {
                    val status = call.request.queryParameters["status"]
                    val votes = if (status != null) {
                        voteRepository.getByStatus(com.dunettrpg.server.domain.model.VoteStatus.valueOf(status))
                    } else {
                        voteRepository.getAll()
                    }
                    
                    call.respond(
                        ApiResponse.success(
                            data = votes,
                            timestamp = Clock.System.now().toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Vote>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Get pending votes for user's house
            get("/pending") {
                try {
                    val houseId = call.request.queryParameters["houseId"]
                    if (houseId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse.error<Vote>(
                                code = "MISSING_HOUSE_ID",
                                message = "House ID is required",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                        return@get
                    }
                    
                    val votes = voteRepository.getPendingForHouse(houseId)
                    call.respond(
                        ApiResponse.success(
                            data = votes,
                            timestamp = Clock.System.now().toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Vote>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Create new vote
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class) ?: ""
                    
                    val request = call.receive<CreateVoteRequest>()
                    val deadline = request.deadline?.let { Instant.parse(it) }
                    
                    val vote = voteService.createVote(
                        type = request.type,
                        title = request.title,
                        description = request.description,
                        initiatorHouseId = request.initiatorHouseId,
                        requiredParticipants = request.requiredParticipants,
                        consensusRequired = request.consensusRequired,
                        deadline = deadline,
                        createdBy = userId
                    )
                    
                    if (vote != null) {
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse.success(
                                data = vote,
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse.error<Vote>(
                                code = "VOTE_CREATION_FAILED",
                                message = "Failed to create vote",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Vote>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Get vote by ID
            get("/{id}") {
                try {
                    val id = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<Vote>(
                            code = "INVALID_ID",
                            message = "Vote ID is required",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                    
                    val vote = voteRepository.getById(id)
                    if (vote != null) {
                        call.respond(
                            ApiResponse.success(
                                data = vote,
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse.error<Vote>(
                                code = "NOT_FOUND",
                                message = "Vote not found",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Vote>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Cast vote
            post("/{id}/cast") {
                try {
                    val id = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<Vote>(
                            code = "INVALID_ID",
                            message = "Vote ID is required",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                    
                    val request = call.receive<CastVoteRequest>()
                    val success = voteService.castVote(id, request.houseId, request.decision)
                    
                    if (success) {
                        val vote = voteRepository.getById(id)
                        call.respond(
                            ApiResponse.success(
                                data = vote,
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse.error<Vote>(
                                code = "VOTE_CAST_FAILED",
                                message = "Failed to cast vote",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Vote>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Cancel vote
            post("/{id}/cancel") {
                try {
                    val id = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<Vote>(
                            code = "INVALID_ID",
                            message = "Vote ID is required",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                    
                    val houseId = call.request.queryParameters["houseId"]
                    if (houseId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse.error<Vote>(
                                code = "MISSING_HOUSE_ID",
                                message = "House ID is required",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                        return@post
                    }
                    
                    val success = voteService.cancelVote(id, houseId)
                    if (success) {
                        call.respond(
                            ApiResponse.success(
                                data = mapOf("message" to "Vote cancelled successfully"),
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ApiResponse.error<Vote>(
                                code = "CANCEL_FAILED",
                                message = "Failed to cancel vote - not authorized or vote not found",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Vote>(
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
