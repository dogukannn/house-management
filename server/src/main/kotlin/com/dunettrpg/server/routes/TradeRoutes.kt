package com.dunettrpg.server.routes

import com.dunettrpg.server.data.repository.TradeRepository
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

fun Route.tradeRoutes() {
    route("/api/trades") {
        authenticate {
            // GET /api/trades - List all trades or filter by status
            get {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@get
                }
                
                val status = call.request.queryParameters["status"]
                val trades = if (status != null) {
                    try {
                        TradeRepository.getTradesByStatus(TradeStatus.valueOf(status.uppercase()))
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponses.error("INVALID_STATUS", "Invalid trade status"))
                        return@get
                    }
                } else {
                    TradeRepository.getAllTrades()
                }
                
                call.respond(HttpStatusCode.OK, ApiResponses.success(trades))
            }
            
            // POST /api/trades/propose - Propose a new trade
            post("/propose") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@post
                }
                
                val houseId = principal.payload.getClaim("houseId").asString()
                if (houseId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("NO_HOUSE", "User is not associated with a house"))
                    return@post
                }
                
                val request = try {
                    call.receive<ProposeTradeRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("INVALID_REQUEST", "Invalid request format"))
                    return@post
                }
                
                // Validate that the proposer is from the fromHouseId
                if (request.fromHouseId != houseId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponses.error("FORBIDDEN", "You can only propose trades from your own house"))
                    return@post
                }
                
                val trade = TradeRepository.createTrade(
                    voteId = request.voteId,
                    fromHouseId = request.fromHouseId,
                    toHouseId = request.toHouseId,
                    offering = request.offering,
                    requesting = request.requesting,
                    duration = request.duration
                )
                
                call.respond(HttpStatusCode.Created, ApiResponses.success(trade))
            }
            
            // GET /api/trades/{id} - Get trade details
            get("/{id}") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@get
                }
                
                val tradeId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("MISSING_ID", "Trade ID is required"))
                    return@get
                }
                
                val trade = TradeRepository.getTradeById(tradeId)
                if (trade == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponses.error("NOT_FOUND", "Trade not found"))
                    return@get
                }
                
                // Check if user is a party or admin
                val role = principal.payload.getClaim("role").asString()
                val houseId = principal.payload.getClaim("houseId").asString()
                if (role != "ADMIN" && trade.fromHouseId != houseId && trade.toHouseId != houseId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponses.error("FORBIDDEN", "You are not a party to this trade"))
                    return@get
                }
                
                call.respond(HttpStatusCode.OK, ApiResponses.success(trade))
            }
            
            // POST /api/trades/{id}/accept - Accept a trade
            post("/{id}/accept") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@post
                }
                
                val tradeId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("MISSING_ID", "Trade ID is required"))
                    return@post
                }
                
                val houseId = principal.payload.getClaim("houseId").asString()
                if (houseId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("NO_HOUSE", "User is not associated with a house"))
                    return@post
                }
                
                val trade = TradeRepository.getTradeById(tradeId)
                if (trade == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponses.error("NOT_FOUND", "Trade not found"))
                    return@post
                }
                
                // Only the receiving house can accept
                if (trade.toHouseId != houseId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponses.error("FORBIDDEN", "Only the receiving house can accept a trade"))
                    return@post
                }
                
                val acceptedTrade = TradeRepository.acceptTrade(tradeId)
                if (acceptedTrade == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("CANNOT_ACCEPT", "Cannot accept trade - it may not be in PROPOSED status"))
                    return@post
                }
                
                call.respond(HttpStatusCode.OK, ApiResponses.success(acceptedTrade))
            }
            
            // POST /api/trades/{id}/reject - Reject a trade
            post("/{id}/reject") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@post
                }
                
                val tradeId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("MISSING_ID", "Trade ID is required"))
                    return@post
                }
                
                val houseId = principal.payload.getClaim("houseId").asString()
                if (houseId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("NO_HOUSE", "User is not associated with a house"))
                    return@post
                }
                
                val trade = TradeRepository.getTradeById(tradeId)
                if (trade == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponses.error("NOT_FOUND", "Trade not found"))
                    return@post
                }
                
                // Only the receiving house can reject
                if (trade.toHouseId != houseId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponses.error("FORBIDDEN", "Only the receiving house can reject a trade"))
                    return@post
                }
                
                val rejectedTrade = TradeRepository.rejectTrade(tradeId)
                call.respond(HttpStatusCode.OK, ApiResponses.success(rejectedTrade))
            }
            
            // POST /api/trades/{id}/cancel - Cancel a trade (proposer or admin)
            post("/{id}/cancel") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@post
                }
                
                val tradeId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("MISSING_ID", "Trade ID is required"))
                    return@post
                }
                
                val trade = TradeRepository.getTradeById(tradeId)
                if (trade == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponses.error("NOT_FOUND", "Trade not found"))
                    return@post
                }
                
                val role = principal.payload.getClaim("role").asString()
                val houseId = principal.payload.getClaim("houseId").asString()
                
                // Only proposer or admin can cancel
                if (role != "ADMIN" && trade.fromHouseId != houseId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponses.error("FORBIDDEN", "Only the proposer or admin can cancel a trade"))
                    return@post
                }
                
                val cancelledTrade = TradeRepository.cancelTrade(tradeId)
                call.respond(HttpStatusCode.OK, ApiResponses.success(cancelledTrade))
            }
            
            // POST /api/trades/{id}/counter - Counter-propose (create new trade with modified terms)
            post("/{id}/counter") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponses.error("UNAUTHORIZED", "Authentication required"))
                    return@post
                }
                
                val tradeId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("MISSING_ID", "Trade ID is required"))
                    return@post
                }
                
                val houseId = principal.payload.getClaim("houseId").asString()
                if (houseId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("NO_HOUSE", "User is not associated with a house"))
                    return@post
                }
                
                val originalTrade = TradeRepository.getTradeById(tradeId)
                if (originalTrade == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponses.error("NOT_FOUND", "Original trade not found"))
                    return@post
                }
                
                // Only the receiving house can counter
                if (originalTrade.toHouseId != houseId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponses.error("FORBIDDEN", "Only the receiving house can counter-propose"))
                    return@post
                }
                
                val request = try {
                    call.receive<CounterTradeRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponses.error("INVALID_REQUEST", "Invalid request format"))
                    return@post
                }
                
                // Cancel original trade
                TradeRepository.cancelTrade(tradeId)
                
                // Create new trade with reversed parties and new terms
                val counterTrade = TradeRepository.createTrade(
                    voteId = null,
                    fromHouseId = originalTrade.toHouseId,
                    toHouseId = originalTrade.fromHouseId,
                    offering = request.offering,
                    requesting = request.requesting,
                    duration = request.duration
                )
                
                call.respond(HttpStatusCode.Created, ApiResponses.success(counterTrade))
            }
        }
    }
}

@Serializable
data class ProposeTradeRequest(
    val voteId: String? = null,
    val fromHouseId: String,
    val toHouseId: String,
    val offering: TradeResources,
    val requesting: TradeResources,
    val duration: Int? = null
)

@Serializable
data class CounterTradeRequest(
    val offering: TradeResources,
    val requesting: TradeResources,
    val duration: Int? = null
)
