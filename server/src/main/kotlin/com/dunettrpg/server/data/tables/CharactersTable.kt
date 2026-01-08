package com.dunettrpg.server.data.tables

import org.jetbrains.exposed.sql.Table

object CharactersTable : Table("characters") {
    val id = uuid("id").autoGenerate()
    val houseId = uuid("house_id").references(HousesTable.id)
    val name = varchar("name", 100)
    val role = varchar("role", 100)
    val stats = text("stats") // JSON
    val status = varchar("status", 20) // ACTIVE, INJURED, CAPTURED, DECEASED
    val notes = text("notes").nullable()
    val portraitUrl = varchar("portrait_url", 500).nullable()
    
    override val primaryKey = PrimaryKey(id)
}
