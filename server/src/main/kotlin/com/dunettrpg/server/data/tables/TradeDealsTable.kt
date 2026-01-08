package com.dunettrpg.server.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object TradeDealsTable : Table("trade_deals") {
    val id = uuid("id").autoGenerate()
    val voteId = uuid("vote_id").nullable()
    val fromHouseId = uuid("from_house_id").references(HousesTable.id)
    val toHouseId = uuid("to_house_id").references(HousesTable.id)
    val offering = text("offering") // JSON
    val requesting = text("requesting") // JSON
    val duration = integer("duration").nullable() // cycles
    val status = varchar("status", 20) // PROPOSED, ACTIVE, COMPLETED, CANCELLED, REJECTED
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at").nullable()
    
    override val primaryKey = PrimaryKey(id)
}
