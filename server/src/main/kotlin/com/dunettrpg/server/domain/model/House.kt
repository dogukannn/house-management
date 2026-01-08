package com.dunettrpg.server.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

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
