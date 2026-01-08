package com.dunettrpg.server.data.repository

import com.dunettrpg.server.data.tables.HousesTable
import com.dunettrpg.server.domain.model.EconomyState
import com.dunettrpg.server.domain.model.House
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class HouseRepository {
    
    fun getAllHouses(): List<House> = transaction {
        HousesTable.selectAll().map { rowToHouse(it) }
    }
    
    fun getHouseById(id: String): House? = transaction {
        HousesTable.selectAll().where { HousesTable.id eq UUID.fromString(id) }
            .map { rowToHouse(it) }
            .singleOrNull()
    }
    
    fun updateHouse(id: String, name: String?, planetaryFief: String?, economyState: EconomyState?, politicalStanding: Int?): House? = transaction {
        val updates = mutableMapOf<Column<*>, Any?>()
        name?.let { updates[HousesTable.name] = it }
        planetaryFief?.let { updates[HousesTable.planetaryFief] = it }
        economyState?.let { updates[HousesTable.economyState] = Json.encodeToString(EconomyState.serializer(), it) }
        politicalStanding?.let { updates[HousesTable.politicalStanding] = it }
        updates[HousesTable.updatedAt] = Clock.System.now()
        
        val houseId = UUID.fromString(id)
        HousesTable.update({ HousesTable.id eq houseId }) {
            updates.forEach { (column, value) ->
                it[column as Column<Any?>] = value
            }
        }
        
        getHouseById(id)
    }
    
    fun createHouse(name: String, planetaryFief: String, economyState: EconomyState, politicalStanding: Int): House = transaction {
        val now = Clock.System.now()
        val id = HousesTable.insert {
            it[HousesTable.name] = name
            it[HousesTable.planetaryFief] = planetaryFief
            it[HousesTable.economyState] = Json.encodeToString(EconomyState.serializer(), economyState)
            it[HousesTable.politicalStanding] = politicalStanding
            it[createdAt] = now
            it[updatedAt] = now
        } get HousesTable.id
        
        getHouseById(id.toString())!!
    }
    
    private fun rowToHouse(row: ResultRow): House {
        val economyStateJson = row[HousesTable.economyState]
        val economyState = Json.decodeFromString(EconomyState.serializer(), economyStateJson)
        
        return House(
            id = row[HousesTable.id].toString(),
            name = row[HousesTable.name],
            planetaryFief = row[HousesTable.planetaryFief],
            economyState = economyState,
            politicalStanding = row[HousesTable.politicalStanding],
            createdAt = row[HousesTable.createdAt].toString(),
            updatedAt = row[HousesTable.updatedAt].toString()
        )
    }
}
