package com.dunettrpg.server.routes

import com.dunettrpg.server.data.repository.VoteRepository
import com.dunettrpg.server.domain.model.*
import com.dunettrpg.server.dto.response.ApiResponses
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.voteRoutes() {
    route("/api/votes") {
        authenticate {
            // GET /api/votes - List all votes or filter by status
            get {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@get
                }
                
                val status = call.request.queryParameters["status"]
                val votes = if (status != null) {
                    try {
                        VoteRepository.getVotesByStatus(VoteStatus.valueOf(status.uppercase()))
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponses.error("INVALID_STATUS", "Invalid vote status"))
                        return@get
                    }
                } else {
                    VoteRepository.getAllVotes()
                }
                
                call.respond(HttpStatusCode.OK, ApiResponses.success(votes))
            }
            
            // GET /api/votes/pending - Get pending votes for the authenticated user's house
            get("/pending") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@get
                }
                
                val houseId = principal.payload.getClaim("houseId").asString()
                if (houseId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("NO_HOUSE", "User is not associated with a house"))
                    return@get
                }
                
                val pendingVotes = VoteRepository.getPendingVotesForHouse(houseId)
                call.respond(HttpStatusCode.OK, ApiResponses.success(pendingVotes))
            }
            
            // POST /api/votes - Create a new vote (admin or house owner)
            post {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@post
                }
                
                val role = principal.payload.getClaim("role").asString()
                val userId = principal.payload.getClaim("userId").asString()
                val userHouseId = principal.payload.getClaim("houseId").asString()
                
                val request = try {
                    call.receive<CreateVoteRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("INVALID_REQUEST", "Invalid request format"))
                    return@post
                }
                
                // Validate that user can create vote (must be admin or owner of initiator house)
                if (role != "ADMIN" && userHouseId != request.initiatorHouseId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponses.error("FORBIDDEN", "You can only create votes for your own house"))
                    return@post
                }
                
                val deadline = request.deadline?.let { kotlinx.datetime.Instant.parse(it) }
                
                val vote = VoteRepository.createVote(
                    type = request.type,
                    title = request.title,
                    description = request.description,
                    initiatorHouseId = request.initiatorHouseId,
                    requiredParticipants = request.requiredParticipants,
                    consensusRequired = request.consensusRequired,
                    deadline = deadline
                )
                
                call.respond(HttpStatusCode.Created, ApiResponses.success(vote))
            }
            
            // GET /api/votes/{id} - Get vote details
            get("/{id}") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@get
                }
                
                val voteId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("MISSING_ID", "Vote ID is required"))
                    return@get
                }
                
                val vote = VoteRepository.getVoteById(voteId)
                if (vote == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponses.error("NOT_FOUND", "Vote not found"))
                    return@get
                }
                
                // Check if user is a participant or admin
                val role = principal.payload.getClaim("role").asString()
                val houseId = principal.payload.getClaim("houseId").asString()
                if (role != "ADMIN" && !vote.requiredParticipants.contains(houseId)) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponses.error("FORBIDDEN", "You are not a participant in this vote"))
                    return@get
                }
                
                call.respond(HttpStatusCode.OK, ApiResponses.success(vote))
            }
            
            // POST /api/votes/{id}/cast - Cast a vote
            post("/{id}/cast") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@post
                }
                
                val voteId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("MISSING_ID", "Vote ID is required"))
                    return@post
                }
                
                val houseId = principal.payload.getClaim("houseId").asString()
                if (houseId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("NO_HOUSE", "User is not associated with a house"))
                    return@post
                }
                
                val request = try {
                    call.receive<CastVoteRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("INVALID_REQUEST", "Invalid request format"))
                    return@post
                }
                
                val vote = VoteRepository.castVote(voteId, houseId, request.decision)
                if (vote == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("CANNOT_VOTE", "Cannot cast vote - vote may be closed or you are not a participant"))
                    return@post
                }
                
                call.respond(HttpStatusCode.OK, ApiResponses.success(vote))
            }
            
            // POST /api/votes/{id}/cancel - Cancel a vote (admin or initiator)
            post("/{id}/cancel") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@post
                }
                
                val voteId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("MISSING_ID", "Vote ID is required"))
                    return@post
                }
                
                val vote = VoteRepository.getVoteById(voteId)
                if (vote == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponses.error("NOT_FOUND", "Vote not found"))
                    return@post
                }
                
                val role = principal.payload.getClaim("role").asString()
                val houseId = principal.payload.getClaim("houseId").asString()
                
                // Only admin or initiator can cancel
                if (role != "ADMIN" && vote.initiatorHouseId != houseId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponses.error("FORBIDDEN", "Only admin or initiator can cancel a vote"))
                    return@post
                }
                
                val cancelledVote = VoteRepository.cancelVote(voteId)
                call.respond(HttpStatusCode.OK, ApiResponses.success(cancelledVote))
            }
        }
    }
}

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
    val decision: Decision
)
