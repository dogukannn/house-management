package com.dunettrpg.server.plugins

import com.dunettrpg.server.dto.response.ApiResponse
import com.dunettrpg.server.dto.response.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.datetime.Clock

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(
                    success = false,
                    error = ErrorResponse(
                        code = "INTERNAL_ERROR",
                        message = cause.message ?: "An unexpected error occurred"
                    ),
                    timestamp = Clock.System.now().toString()
                )
            )
        }
    }
}
