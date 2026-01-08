package com.dunettrpg.server.data.repository

import com.dunettrpg.server.data.tables.ArmiesTable
import com.dunettrpg.server.domain.model.Army
import com.dunettrpg.server.domain.model.ArmyStatus
import com.dunettrpg.server.domain.model.ArmyUnits
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ArmyRepository {
    
    fun getArmiesByHouseId(houseId: String): List<Army> = transaction {
        ArmiesTable.selectAll().where { ArmiesTable.houseId eq UUID.fromString(houseId) }
            .map { rowToArmy(it) }
    }
    
    fun getArmyById(id: String): Army? = transaction {
        ArmiesTable.selectAll().where { ArmiesTable.id eq UUID.fromString(id) }
            .map { rowToArmy(it) }
            .singleOrNull()
    }
    
    fun createArmy(houseId: String, name: String, units: ArmyUnits, location: String, status: ArmyStatus, maintenanceCost: Double, commanderId: String?): Army = transaction {
        val id = ArmiesTable.insert {
            it[ArmiesTable.houseId] = UUID.fromString(houseId)
            it[ArmiesTable.name] = name
            it[ArmiesTable.units] = Json.encodeToString(ArmyUnits.serializer(), units)
            it[ArmiesTable.location] = location
            it[ArmiesTable.status] = status.name
            it[ArmiesTable.maintenanceCost] = maintenanceCost.toBigDecimal()
            it[ArmiesTable.commanderId] = commanderId?.let { cid -> UUID.fromString(cid) }
        } get ArmiesTable.id
        
        getArmyById(id.toString())!!
    }
    
    fun updateArmy(id: String, name: String?, units: ArmyUnits?, location: String?, status: ArmyStatus?, maintenanceCost: Double?, commanderId: String?): Army? = transaction {
        val armyId = UUID.fromString(id)
        val updates = mutableMapOf<Column<*>, Any?>()
        
        name?.let { updates[ArmiesTable.name] = it }
        units?.let { updates[ArmiesTable.units] = Json.encodeToString(ArmyUnits.serializer(), it) }
        location?.let { updates[ArmiesTable.location] = it }
        status?.let { updates[ArmiesTable.status] = it.name }
        maintenanceCost?.let { updates[ArmiesTable.maintenanceCost] = it.toBigDecimal() }
        commanderId?.let { updates[ArmiesTable.commanderId] = if (it.isEmpty()) null else UUID.fromString(it) }
        
        ArmiesTable.update({ ArmiesTable.id eq armyId }) {
            updates.forEach { (column, value) ->
                it[column as Column<Any?>] = value
            }
        }
        
        getArmyById(id)
    }
    
    fun deleteArmy(id: String): Boolean = transaction {
        ArmiesTable.deleteWhere { ArmiesTable.id eq UUID.fromString(id) } > 0
    }
    
    private fun rowToArmy(row: ResultRow): Army {
        val unitsJson = row[ArmiesTable.units]
        val units = Json.decodeFromString(ArmyUnits.serializer(), unitsJson)
        
        return Army(
            id = row[ArmiesTable.id].toString(),
            houseId = row[ArmiesTable.houseId].toString(),
            name = row[ArmiesTable.name],
            units = units,
            location = row[ArmiesTable.location],
            status = ArmyStatus.valueOf(row[ArmiesTable.status]),
            maintenanceCost = row[ArmiesTable.maintenanceCost].toDouble(),
            commanderId = row[ArmiesTable.commanderId]?.toString()
        )
    }
}
