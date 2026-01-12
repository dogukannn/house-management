package com.dunettrpg.server.data.repository

import com.dunettrpg.server.data.tables.TradeDealsTable
import com.dunettrpg.server.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object TradeRepository {
    
    fun getAllTrades(): List<TradeDeal> = transaction {
        TradeDealsTable.selectAll().map { rowToTradeDeal(it) }
    }
    
    fun getTradeById(id: String): TradeDeal? = transaction {
        TradeDealsTable.selectAll().where { TradeDealsTable.id eq UUID.fromString(id) }
            .map { rowToTradeDeal(it) }
            .singleOrNull()
    }
    
    fun getTradesByHouseId(houseId: String): List<TradeDeal> = transaction {
        val uuid = UUID.fromString(houseId)
        TradeDealsTable.selectAll().where { 
            (TradeDealsTable.fromHouseId eq uuid) or (TradeDealsTable.toHouseId eq uuid)
        }.map { rowToTradeDeal(it) }
    }
    
    fun getTradesByStatus(status: TradeStatus): List<TradeDeal> = transaction {
        TradeDealsTable.selectAll().where { TradeDealsTable.status eq status.name }
            .map { rowToTradeDeal(it) }
    }
    
    fun createTrade(
        voteId: String?,
        fromHouseId: String,
        toHouseId: String,
        offering: TradeResources,
        requesting: TradeResources,
        duration: Int?
    ): TradeDeal = transaction {
        val id = UUID.randomUUID()
        TradeDealsTable.insert {
            it[TradeDealsTable.id] = id
            it[TradeDealsTable.voteId] = voteId?.let { UUID.fromString(it) }
            it[TradeDealsTable.fromHouseId] = UUID.fromString(fromHouseId)
            it[TradeDealsTable.toHouseId] = UUID.fromString(toHouseId)
            it[TradeDealsTable.offering] = Json.encodeToString(offering)
            it[TradeDealsTable.requesting] = Json.encodeToString(requesting)
            it[TradeDealsTable.duration] = duration
            it[TradeDealsTable.status] = TradeStatus.PROPOSED.name
            it[TradeDealsTable.createdAt] = kotlinx.datetime.Clock.System.now()
            it[TradeDealsTable.expiresAt] = null
        }
        getTradeById(id.toString())!!
    }
    
    fun updateTradeStatus(tradeId: String, status: TradeStatus): TradeDeal? = transaction {
        // Get current trade to check duration
        val trade = getTradeById(tradeId)
        
        TradeDealsTable.update({ TradeDealsTable.id eq UUID.fromString(tradeId) }) {
            it[TradeDealsTable.status] = status.name
            if (status == TradeStatus.ACTIVE && trade?.duration != null) {
                // Set expiration if duration specified
                it[expiresAt] = kotlinx.datetime.Clock.System.now()
            }
        }
        getTradeById(tradeId)
    }
    
    fun acceptTrade(tradeId: String): TradeDeal? = transaction {
        val trade = getTradeById(tradeId) ?: return@transaction null
        
        if (trade.status != TradeStatus.PROPOSED) {
            return@transaction null // Can only accept proposed trades
        }
        
        // Execute resource transfer
        executeTradeTransfer(trade)
        
        // Update status to ACTIVE or COMPLETED
        val newStatus = if (trade.duration != null) TradeStatus.ACTIVE else TradeStatus.COMPLETED
        updateTradeStatus(tradeId, newStatus)
    }
    
    fun rejectTrade(tradeId: String): TradeDeal? = transaction {
        updateTradeStatus(tradeId, TradeStatus.REJECTED)
    }
    
    fun cancelTrade(tradeId: String): TradeDeal? = transaction {
        updateTradeStatus(tradeId, TradeStatus.CANCELLED)
    }
    
    private fun executeTradeTransfer(trade: TradeDeal) {
        // Get houses
        val fromHouse = HouseRepository.getHouseById(trade.fromHouseId) ?: return
        val toHouse = HouseRepository.getHouseById(trade.toHouseId) ?: return
        
        // Update from house (subtract offering, add requesting)
        val fromEconomy = fromHouse.economyState.copy(
            spiceReserves = fromHouse.economyState.spiceReserves - trade.offering.spice + trade.requesting.spice,
            solariBalance = fromHouse.economyState.solariBalance - trade.offering.solaris + trade.requesting.solaris
        )
        HouseRepository.updateHouse(trade.fromHouseId, null, null, fromEconomy, null)
        
        // Update to house (add offering, subtract requesting)
        val toEconomy = toHouse.economyState.copy(
            spiceReserves = toHouse.economyState.spiceReserves + trade.offering.spice - trade.requesting.spice,
            solariBalance = toHouse.economyState.solariBalance + trade.offering.solaris - trade.requesting.solaris
        )
        HouseRepository.updateHouse(trade.toHouseId, null, null, toEconomy, null)
    }
    
    private fun rowToTradeDeal(row: ResultRow): TradeDeal {
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
