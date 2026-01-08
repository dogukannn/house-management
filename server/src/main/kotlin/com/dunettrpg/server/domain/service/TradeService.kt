package com.dunettrpg.server.domain.service

import com.dunettrpg.server.data.repository.EventRepository
import com.dunettrpg.server.data.repository.HouseRepository
import com.dunettrpg.server.data.repository.TradeRepository
import com.dunettrpg.server.domain.model.GameEventType
import com.dunettrpg.server.domain.model.TradeOffering
import com.dunettrpg.server.domain.model.TradeDeal
import com.dunettrpg.server.domain.model.TradeStatus

class TradeService(
    private val tradeRepository: TradeRepository,
    private val houseRepository: HouseRepository,
    private val eventRepository: EventRepository
) {
    
    suspend fun proposeTrade(
        fromHouseId: String,
        toHouseId: String,
        offering: TradeOffering,
        requesting: TradeOffering,
        duration: Int? = null
    ): TradeDeal? {
        // Validate houses exist
        val fromHouse = houseRepository.getHouseById(fromHouseId) ?: return null
        val toHouse = houseRepository.getHouseById(toHouseId) ?: return null
        
        // Validate proposer has sufficient resources
        if (offering.spice > fromHouse.economyState.spiceReserves ||
            offering.solaris > fromHouse.economyState.solariBalance) {
            return null
        }
        
        val trade = tradeRepository.create(
            fromHouseId, toHouseId, offering, requesting, duration
        )
        
        if (trade != null) {
            // Create event
            eventRepository.create(
                type = GameEventType.TRADE_PROPOSED,
                targetHouseIds = listOf(toHouseId),
                payload = mapOf(
                    "tradeId" to trade.id,
                    "fromHouse" to fromHouse.name,
                    "toHouse" to toHouse.name,
                    "offeringSpice" to offering.spice.toString(),
                    "offeringSolaris" to offering.solaris.toString()
                ),
                requiresNotification = true,
                createdBy = fromHouseId
            )
        }
        
        return trade
    }
    
    suspend fun acceptTrade(tradeId: String, acceptingHouseId: String): Boolean {
        val trade = tradeRepository.getById(tradeId) ?: return false
        
        // Verify it's the correct house accepting
        if (trade.toHouseId != acceptingHouseId) {
            return false
        }
        
        if (trade.status != TradeStatus.PROPOSED) {
            return false
        }
        
        // Execute resource transfer
        val success = executeTradeTransfer(trade)
        if (!success) {
            return false
        }
        
        tradeRepository.updateStatus(tradeId, TradeStatus.ACTIVE)
        
        // Create event
        eventRepository.create(
            type = GameEventType.TRADE_STATUS_CHANGED,
            targetHouseIds = listOf(trade.fromHouseId, trade.toHouseId),
            payload = mapOf(
                "tradeId" to tradeId,
                "newStatus" to TradeStatus.ACTIVE.name,
                "message" to "Trade accepted and executed"
            ),
            requiresNotification = true,
            createdBy = acceptingHouseId
        )
        
        return true
    }
    
    suspend fun rejectTrade(tradeId: String, rejectingHouseId: String): Boolean {
        val trade = tradeRepository.getById(tradeId) ?: return false
        
        if (trade.toHouseId != rejectingHouseId) {
            return false
        }
        
        if (trade.status != TradeStatus.PROPOSED) {
            return false
        }
        
        tradeRepository.updateStatus(tradeId, TradeStatus.REJECTED)
        
        // Create event
        eventRepository.create(
            type = GameEventType.TRADE_STATUS_CHANGED,
            targetHouseIds = listOf(trade.fromHouseId, trade.toHouseId),
            payload = mapOf(
                "tradeId" to tradeId,
                "newStatus" to TradeStatus.REJECTED.name,
                "message" to "Trade rejected"
            ),
            requiresNotification = true,
            createdBy = rejectingHouseId
        )
        
        return true
    }
    
    private suspend fun executeTradeTransfer(trade: TradeDeal): Boolean {
        val fromHouse = houseRepository.getHouseById(trade.fromHouseId) ?: return false
        val toHouse = houseRepository.getHouseById(trade.toHouseId) ?: return false
        
        // Verify both houses have sufficient resources
        if (trade.offering.spice > fromHouse.economyState.spiceReserves ||
            trade.offering.solaris > fromHouse.economyState.solariBalance) {
            return false
        }
        
        if (trade.requesting.spice > toHouse.economyState.spiceReserves ||
            trade.requesting.solaris > toHouse.economyState.solariBalance) {
            return false
        }
        
        // Transfer from fromHouse to toHouse
        val newFromEconomy = fromHouse.economyState.copy(
            spiceReserves = fromHouse.economyState.spiceReserves - trade.offering.spice + trade.requesting.spice,
            solariBalance = fromHouse.economyState.solariBalance - trade.offering.solaris + trade.requesting.solaris
        )
        
        val newToEconomy = toHouse.economyState.copy(
            spiceReserves = toHouse.economyState.spiceReserves + trade.offering.spice - trade.requesting.spice,
            solariBalance = toHouse.economyState.solariBalance + trade.offering.solaris - trade.requesting.solaris
        )
        
        houseRepository.updateEconomy(trade.fromHouseId, newFromEconomy)
        houseRepository.updateEconomy(trade.toHouseId, newToEconomy)
        
        return true
    }
}
