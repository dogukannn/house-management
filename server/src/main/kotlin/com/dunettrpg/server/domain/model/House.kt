package com.dunettrpg.server.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class House(
    val id: String,
    val name: String,
    val planetaryFief: String,
    val economyState: EconomyState,
    val politicalStanding: Int,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class EconomyState(
    val spiceReserves: Double,
    val solariBalance: Double,
    val incomePerCycle: Double,
    val expensesPerCycle: Double,
    val tradeModifiers: Map<String, Double> = emptyMap()
)

@Serializable
data class Character(
    val id: String,
    val houseId: String,
    val name: String,
    val role: String,
    val stats: CharacterStats,
    val status: CharacterStatus,
    val notes: String?,
    val portraitUrl: String?
)

@Serializable
data class CharacterStats(
    val combat: Int,
    val intrigue: Int,
    val diplomacy: Int,
    val prescience: Int
)

@Serializable
enum class CharacterStatus {
    ACTIVE, INJURED, CAPTURED, DECEASED
}

@Serializable
data class Army(
    val id: String,
    val houseId: String,
    val name: String,
    val units: ArmyUnits,
    val location: String,
    val status: ArmyStatus,
    val maintenanceCost: Double,
    val commanderId: String?
)

@Serializable
data class ArmyUnits(
    val infantry: Int,
    val sardaukar: Int,
    val fremen: Int,
    val ornithopters: Int
)

@Serializable
enum class ArmyStatus {
    STATIONED, DEPLOYED, IN_COMBAT, RETREATING
}

// Vote models
@Serializable
data class Vote(
    val id: String,
    val type: VoteType,
    val title: String,
    val description: String,
    val initiatorHouseId: String,
    val requiredParticipants: List<String>,
    val votes: Map<String, VoteDecision>,
    val consensusRequired: Boolean,
    val deadline: String?,
    val status: VoteStatus,
    val result: String?,
    val createdAt: String,
    val resolvedAt: String?
)

@Serializable
enum class VoteType {
    TRADE_DEAL, ALLIANCE, WAR_DECLARATION, LANDSRAAD_MOTION, CUSTOM
}

@Serializable
data class VoteDecision(
    val decision: Decision,
    val timestamp: String
)

@Serializable
enum class Decision {
    APPROVE, REJECT, ABSTAIN
}

@Serializable
enum class VoteStatus {
    PENDING, PASSED, FAILED, EXPIRED, CANCELLED
}

// Trade Deal models
@Serializable
data class TradeDeal(
    val id: String,
    val voteId: String?,
    val fromHouseId: String,
    val toHouseId: String,
    val offering: TradeResources,
    val requesting: TradeResources,
    val duration: Int?,
    val status: TradeStatus,
    val createdAt: String,
    val expiresAt: String?
)

@Serializable
data class TradeResources(
    val spice: Double = 0.0,
    val solaris: Double = 0.0,
    val other: List<String> = emptyList()
)

@Serializable
enum class TradeStatus {
    PROPOSED, ACTIVE, COMPLETED, CANCELLED, REJECTED
}
