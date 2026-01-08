package com.dunettrpg.server.data.repository

import com.dunettrpg.server.config.DatabaseConfig.dbQuery
import com.dunettrpg.server.data.tables.TradeDealsTable
import com.dunettrpg.server.domain.model.TradeDeal
import com.dunettrpg.server.domain.model.TradeOffering
import com.dunettrpg.server.domain.model.TradeStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import java.util.UUID

class TradeRepository {
    
    suspend fun getAll(): List<TradeDeal> = dbQuery {
        TradeDealsTable.selectAll().map { toTradeDeal(it) }
    }
    
    suspend fun getById(id: String): TradeDeal? = dbQuery {
        TradeDealsTable.selectAll().where { TradeDealsTable.id eq UUID.fromString(id) }
            .mapNotNull { toTradeDeal(it) }
            .singleOrNull()
    }
    
    suspend fun getByHouse(houseId: String): List<TradeDeal> = dbQuery {
        TradeDealsTable.selectAll().where { 
            (TradeDealsTable.fromHouseId eq UUID.fromString(houseId)) or
            (TradeDealsTable.toHouseId eq UUID.fromString(houseId))
        }.map { toTradeDeal(it) }
    }
    
    suspend fun getByStatus(status: TradeStatus): List<TradeDeal> = dbQuery {
        TradeDealsTable.selectAll().where { TradeDealsTable.status eq status.name }
            .map { toTradeDeal(it) }
    }
    
    suspend fun create(
        fromHouseId: String,
        toHouseId: String,
        offering: TradeOffering,
        requesting: TradeOffering,
        duration: Int? = null,
        voteId: String? = null
    ): TradeDeal? = dbQuery {
        val id = TradeDealsTable.insert {
            it[TradeDealsTable.voteId] = voteId?.let { UUID.fromString(it) }
            it[TradeDealsTable.fromHouseId] = UUID.fromString(fromHouseId)
            it[TradeDealsTable.toHouseId] = UUID.fromString(toHouseId)
            it[TradeDealsTable.offering] = Json.encodeToString(offering)
            it[TradeDealsTable.requesting] = Json.encodeToString(requesting)
            it[TradeDealsTable.duration] = duration
            it[TradeDealsTable.status] = TradeStatus.PROPOSED.name
            it[createdAt] = Clock.System.now()
        } get TradeDealsTable.id
        
        getById(id.toString())
    }
    
    suspend fun updateStatus(tradeId: String, status: TradeStatus): Boolean = dbQuery {
        TradeDealsTable.update({ TradeDealsTable.id eq UUID.fromString(tradeId) }) {
            it[TradeDealsTable.status] = status.name
            if (status == TradeStatus.ACTIVE) {
                it[expiresAt] = null // Clear expiration for active trades
            }
        } > 0
    }
    
    suspend fun setExpiration(tradeId: String, expiresAt: Instant): Boolean = dbQuery {
        TradeDealsTable.update({ TradeDealsTable.id eq UUID.fromString(tradeId) }) {
            it[TradeDealsTable.expiresAt] = expiresAt
        } > 0
    }
    
    private fun toTradeDeal(row: ResultRow): TradeDeal {
        return TradeDeal(
            id = row[TradeDealsTable.id].toString(),
            voteId = row[TradeDealsTable.voteId]?.toString(),
            fromHouseId = row[TradeDealsTable.fromHouseId].toString(),
            toHouseId = row[TradeDealsTable.toHouseId].toString(),
            offering = Json.decodeFromString(row[TradeDealsTable.offering]),
            requesting = Json.decodeFromString(row[TradeDealsTable.requesting]),
            duration = row[TradeDealsTable.duration],
            status = TradeStatus.valueOf(row[TradeDealsTable.status]),
            createdAt = row[TradeDealsTable.createdAt].toString(),
            expiresAt = row[TradeDealsTable.expiresAt]?.toString()
        )
    }
}
