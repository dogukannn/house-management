package com.dunettrpg.server.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object HousesTable : Table("houses") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 100)
    val planetaryFief = varchar("planetary_fief", 100)
    val economyState = text("economy_state") // JSON
    val politicalStanding = integer("political_standing").default(0)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}
