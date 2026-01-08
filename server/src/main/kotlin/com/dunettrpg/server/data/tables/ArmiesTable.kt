package com.dunettrpg.server.data.tables

import org.jetbrains.exposed.sql.Table

object ArmiesTable : Table("armies") {
    val id = uuid("id").autoGenerate()
    val houseId = uuid("house_id").references(HousesTable.id)
    val name = varchar("name", 100)
    val units = text("units") // JSON
    val location = varchar("location", 200)
    val status = varchar("status", 20) // STATIONED, DEPLOYED, IN_COMBAT, RETREATING
    val maintenanceCost = decimal("maintenance_cost", 10, 2)
    val commanderId = uuid("commander_id").nullable()
    
    override val primaryKey = PrimaryKey(id)
}
