package com.dunettrpg.server.routes

import com.dunettrpg.server.data.repository.EventRepository
import com.dunettrpg.server.data.repository.HouseRepository
import com.dunettrpg.server.data.repository.TradeRepository
import com.dunettrpg.server.domain.model.TradeDeal
import com.dunettrpg.server.domain.model.TradeOffering
import com.dunettrpg.server.domain.model.TradeStatus
import com.dunettrpg.server.domain.service.TradeService
import com.dunettrpg.server.dto.response.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class ProposeTradeRequest(
    val fromHouseId: String,
    val toHouseId: String,
    val offering: TradeOffering,
    val requesting: TradeOffering,
    val duration: Int? = null
)

fun Route.tradeRoutes() {
    val tradeRepository = TradeRepository()
    val houseRepository = HouseRepository()
    val eventRepository = EventRepository()
    val tradeService = TradeService(tradeRepository, houseRepository, eventRepository)
    
    authenticate("auth-jwt") {
        route("/api/trades") {
            // List all trades
            get {
                try {
                    val houseId = call.request.queryParameters["houseId"]
                    val trades = if (houseId != null) {
                        tradeRepository.getByHouse(houseId)
                    } else {
                        tradeRepository.getAll()
                    }
                    
                    call.respond(
                        ApiResponse.success(
                            data = trades,
                            timestamp = Clock.System.now().toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<TradeDeal>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Propose new trade
            post("/propose") {
                try {
                    val request = call.receive<ProposeTradeRequest>()
                    
                    val trade = tradeService.proposeTrade(
                        fromHouseId = request.fromHouseId,
                        toHouseId = request.toHouseId,
                        offering = request.offering,
                        requesting = request.requesting,
                        duration = request.duration
                    )
                    
                    if (trade != null) {
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse.success(
                                data = trade,
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse.error<TradeDeal>(
                                code = "TRADE_PROPOSAL_FAILED",
                                message = "Failed to propose trade - check house IDs and resources",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<TradeDeal>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Get trade by ID
            get("/{id}") {
                try {
                    val id = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<TradeDeal>(
                            code = "INVALID_ID",
                            message = "Trade ID is required",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                    
                    val trade = tradeRepository.getById(id)
                    if (trade != null) {
                        call.respond(
                            ApiResponse.success(
                                data = trade,
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse.error<TradeDeal>(
                                code = "NOT_FOUND",
                                message = "Trade not found",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<TradeDeal>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Accept trade
            post("/{id}/accept") {
                try {
                    val id = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<TradeDeal>(
                            code = "INVALID_ID",
                            message = "Trade ID is required",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                    
                    val houseId = call.request.queryParameters["houseId"]
                    if (houseId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse.error<TradeDeal>(
                                code = "MISSING_HOUSE_ID",
                                message = "House ID is required",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                        return@post
                    }
                    
                    val success = tradeService.acceptTrade(id, houseId)
                    if (success) {
                        val trade = tradeRepository.getById(id)
                        call.respond(
                            ApiResponse.success(
                                data = trade,
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse.error<TradeDeal>(
                                code = "ACCEPT_FAILED",
                                message = "Failed to accept trade - check authorization and resources",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<TradeDeal>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Reject trade
            post("/{id}/reject") {
                try {
                    val id = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<TradeDeal>(
                            code = "INVALID_ID",
                            message = "Trade ID is required",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                    
                    val houseId = call.request.queryParameters["houseId"]
                    if (houseId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse.error<TradeDeal>(
                                code = "MISSING_HOUSE_ID",
                                message = "House ID is required",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                        return@post
                    }
                    
                    val success = tradeService.rejectTrade(id, houseId)
                    if (success) {
                        call.respond(
                            ApiResponse.success(
                                data = mapOf("message" to "Trade rejected successfully"),
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse.error<TradeDeal>(
                                code = "REJECT_FAILED",
                                message = "Failed to reject trade - not authorized or invalid trade",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<TradeDeal>(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Unknown error",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Cancel trade
            post("/{id}/cancel") {
                try {
                    val id = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error<TradeDeal>(
                            code = "INVALID_ID",
                            message = "Trade ID is required",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                    
                    val success = tradeRepository.updateStatus(id, TradeStatus.CANCELLED)
                    if (success) {
                        call.respond(
                            ApiResponse.success(
                                data = mapOf("message" to "Trade cancelled successfully"),
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse.error<TradeDeal>(
                                code = "NOT_FOUND",
                                message = "Trade not found",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<TradeDeal>(
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
