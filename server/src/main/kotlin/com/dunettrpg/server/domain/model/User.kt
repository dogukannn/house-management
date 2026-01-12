package com.dunettrpg.server.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val username: String,
    val role: String,
    val houseId: String? = null,
    val fcmToken: String? = null,
    val createdAt: String? = null,
    val lastActiveAt: String? = null
)

@Serializable
enum class UserRole {
    ADMIN, PLAYER
}
