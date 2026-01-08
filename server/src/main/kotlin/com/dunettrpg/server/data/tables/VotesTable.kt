package com.dunettrpg.server.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object VotesTable : Table("votes") {
    val id = uuid("id").autoGenerate()
    val type = varchar("type", 50) // TRADE_DEAL, ALLIANCE, WAR_DECLARATION, etc.
    val title = varchar("title", 200)
    val description = text("description")
    val initiatorHouseId = uuid("initiator_house_id").references(HousesTable.id)
    val requiredParticipants = text("required_participants") // JSON array of house IDs
    val votes = text("votes") // JSON map of houseId to decision
    val consensusRequired = bool("consensus_required").default(false)
    val deadline = timestamp("deadline").nullable()
    val status = varchar("status", 20) // PENDING, PASSED, FAILED, EXPIRED, CANCELLED
    val result = text("result").nullable() // JSON
    val createdAt = timestamp("created_at")
    val resolvedAt = timestamp("resolved_at").nullable()
    
    override val primaryKey = PrimaryKey(id)
}
