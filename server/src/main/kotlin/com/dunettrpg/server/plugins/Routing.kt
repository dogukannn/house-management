package com.dunettrpg.server.plugins

import com.dunettrpg.server.routes.authRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Dune TTRPG Server is running")
        }
        
        get("/health") {
            call.respondText("OK")
        }
        
        authRoutes()
    }
}
