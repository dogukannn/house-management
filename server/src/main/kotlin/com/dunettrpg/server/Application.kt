package com.dunettrpg.server

import com.dunettrpg.server.config.DatabaseConfig
import com.dunettrpg.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Initialize database
    DatabaseConfig.init()
    
    // Configure plugins
    configureSerialization()
    configureSecurity()
    configureRouting()
    configureWebSockets()
    configureStatusPages()
    configureMonitoring()
}
