package com.dunettrpg.server.routes

import com.dunettrpg.server.data.repository.HouseRepository
import com.dunettrpg.server.domain.model.EconomyState
import com.dunettrpg.server.domain.model.House
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
data class UpdateHouseRequest(
    val name: String? = null,
    val planetaryFief: String? = null,
    val economyState: EconomyState? = null,
    val politicalStanding: Int? = null
)

fun Route.houseRoutes() {
    val houseRepository = HouseRepository()
    
    authenticate {
        route("/api/houses") {
            // Get all houses (summary)
            get {
                try {
                    val houses = houseRepository.getAllHouses()
                    call.respond(
                        ApiResponse.success(
                            data = houses,
                            timestamp = Clock.System.now().toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<House>(
                            code = "INTERNAL_ERROR",
                            message = "Failed to fetch houses: ${e.message}",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Get house details
            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error<House>(
                        code = "INVALID_ID",
                        message = "House ID is required",
                        timestamp = Clock.System.now().toString()
                    )
                )
                
                try {
                    val house = houseRepository.getHouseById(id)
                    if (house != null) {
                        // Check authorization: user must own this house or be admin
                        val principal = call.principal<JWTPrincipal>()
                        val userRole = principal?.payload?.getClaim("role")?.asString()
                        val userHouseId = principal?.payload?.getClaim("houseId")?.asString()
                        
                        if (userRole == "ADMIN" || userHouseId == id) {
                            call.respond(
                                ApiResponse.success(
                                    data = house,
                                    timestamp = Clock.System.now().toString()
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                ApiResponse.error<House>(
                                    code = "FORBIDDEN",
                                    message = "You don't have permission to view this house",
                                    timestamp = Clock.System.now().toString()
                                )
                            )
                        }
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse.error<House>(
                                code = "HOUSE_NOT_FOUND",
                                message = "House not found",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<House>(
                            code = "INTERNAL_ERROR",
                            message = "Failed to fetch house: ${e.message}",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Update house (Admin only)
            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error<House>(
                        code = "INVALID_ID",
                        message = "House ID is required",
                        timestamp = Clock.System.now().toString()
                    )
                )
                
                // Check if user is admin
                val principal = call.principal<JWTPrincipal>()
                val userRole = principal?.payload?.getClaim("role")?.asString()
                
                if (userRole != "ADMIN") {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse.error<House>(
                            code = "FORBIDDEN",
                            message = "Only admins can update houses",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
                
                try {
                    val request = call.receive<UpdateHouseRequest>()
                    val updatedHouse = houseRepository.updateHouse(
                        id = id,
                        name = request.name,
                        planetaryFief = request.planetaryFief,
                        economyState = request.economyState,
                        politicalStanding = request.politicalStanding
                    )
                    
                    if (updatedHouse != null) {
                        call.respond(
                            ApiResponse.success(
                                data = updatedHouse,
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse.error<House>(
                                code = "HOUSE_NOT_FOUND",
                                message = "House not found",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<House>(
                            code = "INTERNAL_ERROR",
                            message = "Failed to update house: ${e.message}",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
        }
    }
}
