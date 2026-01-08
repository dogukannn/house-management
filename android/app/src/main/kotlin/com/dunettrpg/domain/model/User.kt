package com.dunettrpg.domain.model

data class User(
    val id: String,
    val username: String,
    val role: String,
    val houseId: String? = null
)
