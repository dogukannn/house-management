package com.dunettrpg.server.routes

import com.dunettrpg.server.data.repository.ArmyRepository
import com.dunettrpg.server.domain.model.ArmyStatus
import com.dunettrpg.server.domain.model.ArmyUnits
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
data class CreateArmyRequest(
    val name: String,
    val units: ArmyUnits,
    val location: String,
    val status: ArmyStatus = ArmyStatus.STATIONED,
    val maintenanceCost: Double,
    val commanderId: String? = null
)

@Serializable
data class UpdateArmyRequest(
    val name: String? = null,
    val units: ArmyUnits? = null,
    val location: String? = null,
    val status: ArmyStatus? = null,
    val maintenanceCost: Double? = null,
    val commanderId: String? = null
)

fun Route.armyRoutes() {
    val armyRepository = ArmyRepository()
    
    authenticate {
        route("/api/houses/{houseId}/armies") {
            // List house armies
            get {
                val houseId = call.parameters["houseId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error(
                        code = "INVALID_ID",
                        message = "House ID is required",
                        timestamp = Clock.System.now().toString()
                    )
                )
                
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.payload?.getClaim("role")?.asString()
                    val userHouseId = principal?.payload?.getClaim("houseId")?.asString()
                    
                    if (userRole == "ADMIN" || userHouseId == houseId) {
                        val armies = armyRepository.getArmiesByHouseId(houseId)
                        call.respond(
                            ApiResponse.success(
                                data = armies,
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ApiResponse.error(
                                code = "FORBIDDEN",
                                message = "You don't have permission to view these armies",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error(
                            code = "INTERNAL_ERROR",
                            message = "Failed to fetch armies: ${e.message}",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Create army (Admin only)
            post {
                val houseId = call.parameters["houseId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error(
                        code = "INVALID_ID",
                        message = "House ID is required",
                        timestamp = Clock.System.now().toString()
                    )
                )
                
                val principal = call.principal<JWTPrincipal>()
                val userRole = principal?.payload?.getClaim("role")?.asString()
                
                if (userRole != "ADMIN") {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse.error(
                            code = "FORBIDDEN",
                            message = "Only admins can create armies",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
                
                try {
                    val request = call.receive<CreateArmyRequest>()
                    val army = armyRepository.createArmy(
                        houseId = houseId,
                        name = request.name,
                        units = request.units,
                        location = request.location,
                        status = request.status,
                        maintenanceCost = request.maintenanceCost,
                        commanderId = request.commanderId
                    )
                    
                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse.success(
                            data = army,
                            timestamp = Clock.System.now().toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error(
                            code = "INTERNAL_ERROR",
                            message = "Failed to create army: ${e.message}",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
        }
        
        route("/api/armies/{id}") {
            // Update army (Admin only)
            put {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error(
                        code = "INVALID_ID",
                        message = "Army ID is required",
                        timestamp = Clock.System.now().toString()
                    )
                )
                
                val principal = call.principal<JWTPrincipal>()
                val userRole = principal?.payload?.getClaim("role")?.asString()
                
                if (userRole != "ADMIN") {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse.error(
                            code = "FORBIDDEN",
                            message = "Only admins can update armies",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
                
                try {
                    val request = call.receive<UpdateArmyRequest>()
                    val updatedArmy = armyRepository.updateArmy(
                        id = id,
                        name = request.name,
                        units = request.units,
                        location = request.location,
                        status = request.status,
                        maintenanceCost = request.maintenanceCost,
                        commanderId = request.commanderId
                    )
                    
                    if (updatedArmy != null) {
                        call.respond(
                            ApiResponse.success(
                                data = updatedArmy,
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse.error(
                                code = "ARMY_NOT_FOUND",
                                message = "Army not found",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error(
                            code = "INTERNAL_ERROR",
                            message = "Failed to update army: ${e.message}",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
        }
    }
}
