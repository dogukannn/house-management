package com.dunettrpg.server.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

object JwtConfig {
    val secret = System.getenv("JWT_SECRET") ?: "default-secret-change-in-production"
    val issuer = System.getenv("JWT_ISSUER") ?: "dune-ttrpg"
    val audience = System.getenv("JWT_AUDIENCE") ?: "dune-ttrpg-clients"
    val realm = "Dune TTRPG Server"
    private const val validityInMs = 3600000L // 1 hour
    
    private val algorithm = Algorithm.HMAC256(secret)
    
    val verifier = JWT
        .require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()
    
    fun makeToken(userId: String, username: String, role: String): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("userId", userId)
        .withClaim("username", username)
        .withClaim("role", role)
        .withExpiresAt(java.util.Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)
}
