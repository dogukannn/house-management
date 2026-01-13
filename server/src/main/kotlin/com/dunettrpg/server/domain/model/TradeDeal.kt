package com.dunettrpg.server.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeDeal(
    val id: String,
    val voteId: String? = null,
    val fromHouseId: String,
    val toHouseId: String,
    val offering: TradeOffering,
    val requesting: TradeOffering,
    val duration: Int? = null, // cycles, null for one-time
    val status: TradeStatus,
    val createdAt: String,
    val expiresAt: String? = null
)

@Serializable
data class TradeOffering(
    val spice: Double = 0.0,
    val solaris: Double = 0.0,
    val other: List<String> = emptyList()
)

@Serializable
enum class TradeStatus {
    PROPOSED,
    ACTIVE,
    COMPLETED,
    CANCELLED,
    REJECTED
}
