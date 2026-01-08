package com.dunettrpg.server.routes

import com.dunettrpg.server.data.repository.CharacterRepository
import com.dunettrpg.server.domain.model.Character
import com.dunettrpg.server.domain.model.CharacterStats
import com.dunettrpg.server.domain.model.CharacterStatus
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
data class CreateCharacterRequest(
    val name: String,
    val role: String,
    val stats: CharacterStats,
    val status: CharacterStatus = CharacterStatus.ACTIVE,
    val notes: String? = null,
    val portraitUrl: String? = null
)

@Serializable
data class UpdateCharacterRequest(
    val name: String? = null,
    val role: String? = null,
    val stats: CharacterStats? = null,
    val status: CharacterStatus? = null,
    val notes: String? = null,
    val portraitUrl: String? = null
)

fun Route.characterRoutes() {
    val characterRepository = CharacterRepository()
    
    authenticate {
        route("/api/houses/{houseId}/characters") {
            // List house characters
            get {
                val houseId = call.parameters["houseId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error<Character>(
                        code = "INVALID_ID",
                        message = "House ID is required",
                        timestamp = Clock.System.now().toString()
                    )
                )
                
                try {
                    // Check authorization
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.payload?.getClaim("role")?.asString()
                    val userHouseId = principal?.payload?.getClaim("houseId")?.asString()
                    
                    if (userRole == "ADMIN" || userHouseId == houseId) {
                        val characters = characterRepository.getCharactersByHouseId(houseId)
                        call.respond(
                            ApiResponse.success(
                                data = characters,
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ApiResponse.error<Character>(
                                code = "FORBIDDEN",
                                message = "You don't have permission to view these characters",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Character>(
                            code = "INTERNAL_ERROR",
                            message = "Failed to fetch characters: ${e.message}",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Create character (Admin only)
            post {
                val houseId = call.parameters["houseId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error<Character>(
                        code = "INVALID_ID",
                        message = "House ID is required",
                        timestamp = Clock.System.now().toString()
                    )
                )
                
                // Check if user is admin
                val principal = call.principal<JWTPrincipal>()
                val userRole = principal?.payload?.getClaim("role")?.asString()
                
                if (userRole != "ADMIN") {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse.error<Character>(
                            code = "FORBIDDEN",
                            message = "Only admins can create characters",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
                
                try {
                    val request = call.receive<CreateCharacterRequest>()
                    val character = characterRepository.createCharacter(
                        houseId = houseId,
                        name = request.name,
                        role = request.role,
                        stats = request.stats,
                        status = request.status,
                        notes = request.notes,
                        portraitUrl = request.portraitUrl
                    )
                    
                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse.success(
                            data = character,
                            timestamp = Clock.System.now().toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Character>(
                            code = "INTERNAL_ERROR",
                            message = "Failed to create character: ${e.message}",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
        }
        
        route("/api/characters/{id}") {
            // Get character details
            get {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error<Character>(
                        code = "INVALID_ID",
                        message = "Character ID is required",
                        timestamp = Clock.System.now().toString()
                    )
                )
                
                try {
                    val character = characterRepository.getCharacterById(id)
                    if (character != null) {
                        // Check authorization
                        val principal = call.principal<JWTPrincipal>()
                        val userRole = principal?.payload?.getClaim("role")?.asString()
                        val userHouseId = principal?.payload?.getClaim("houseId")?.asString()
                        
                        if (userRole == "ADMIN" || userHouseId == character.houseId) {
                            call.respond(
                                ApiResponse.success(
                                    data = character,
                                    timestamp = Clock.System.now().toString()
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                ApiResponse.error<Character>(
                                    code = "FORBIDDEN",
                                    message = "You don't have permission to view this character",
                                    timestamp = Clock.System.now().toString()
                                )
                            )
                        }
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse.error<Character>(
                                code = "CHARACTER_NOT_FOUND",
                                message = "Character not found",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Character>(
                            code = "INTERNAL_ERROR",
                            message = "Failed to fetch character: ${e.message}",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Update character (Admin only)
            put {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error<Character>(
                        code = "INVALID_ID",
                        message = "Character ID is required",
                        timestamp = Clock.System.now().toString()
                    )
                )
                
                // Check if user is admin
                val principal = call.principal<JWTPrincipal>()
                val userRole = principal?.payload?.getClaim("role")?.asString()
                
                if (userRole != "ADMIN") {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse.error<Character>(
                            code = "FORBIDDEN",
                            message = "Only admins can update characters",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
                
                try {
                    val request = call.receive<UpdateCharacterRequest>()
                    val updatedCharacter = characterRepository.updateCharacter(
                        id = id,
                        name = request.name,
                        role = request.role,
                        stats = request.stats,
                        status = request.status,
                        notes = request.notes,
                        portraitUrl = request.portraitUrl
                    )
                    
                    if (updatedCharacter != null) {
                        call.respond(
                            ApiResponse.success(
                                data = updatedCharacter,
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse.error<Character>(
                                code = "CHARACTER_NOT_FOUND",
                                message = "Character not found",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Character>(
                            code = "INTERNAL_ERROR",
                            message = "Failed to update character: ${e.message}",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
            
            // Delete character (Admin only)
            delete {
                val id = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error<Character>(
                        code = "INVALID_ID",
                        message = "Character ID is required",
                        timestamp = Clock.System.now().toString()
                    )
                )
                
                // Check if user is admin
                val principal = call.principal<JWTPrincipal>()
                val userRole = principal?.payload?.getClaim("role")?.asString()
                
                if (userRole != "ADMIN") {
                    return@delete call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse.error<Character>(
                            code = "FORBIDDEN",
                            message = "Only admins can delete characters",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
                
                try {
                    val deleted = characterRepository.deleteCharacter(id)
                    if (deleted) {
                        call.respond(
                            ApiResponse.success(
                                data = mapOf("deleted" to true),
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse.error<Character>(
                                code = "CHARACTER_NOT_FOUND",
                                message = "Character not found",
                                timestamp = Clock.System.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.error<Character>(
                            code = "INTERNAL_ERROR",
                            message = "Failed to delete character: ${e.message}",
                            timestamp = Clock.System.now().toString()
                        )
                    )
                }
            }
        }
    }
}
