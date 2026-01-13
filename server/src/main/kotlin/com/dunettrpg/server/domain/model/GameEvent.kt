package com.dunettrpg.server.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GameEvent(
    val id: String,
    val type: GameEventType,
    val targetHouseIds: List<String>, // empty = all houses
    val payload: Map<String, String>,
    val requiresNotification: Boolean,
    val notificationSent: Boolean,
    val createdAt: String,
    val createdBy: String
)

@Serializable
enum class GameEventType {
    ECONOMY_UPDATE,
    VOTE_STARTED,
    VOTE_RESOLVED,
    TRADE_PROPOSED,
    TRADE_STATUS_CHANGED,
    ATTACK_DECLARED,
    CHARACTER_STATUS_CHANGE,
    ADMIN_ANNOUNCEMENT,
    CUSTOM
}
