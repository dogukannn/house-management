package com.dunettrpg.server.domain.service

import com.dunettrpg.server.data.repository.EventRepository
import com.dunettrpg.server.data.repository.HouseRepository
import com.dunettrpg.server.domain.model.EconomyState
import com.dunettrpg.server.domain.model.GameEventType

class EconomyService(
    private val houseRepository: HouseRepository,
    private val eventRepository: EventRepository
) {
    
    /**
     * Execute economy cycle for all houses
     * Updates resources based on income and expenses
     */
    suspend fun executeCycle(adminUserId: String): Map<String, Any> {
        val houses = houseRepository.getAllHouses()
        val updates = mutableMapOf<String, Any>()
        
        for (house in houses) {
            val previousState = house.economyState
            val newState = calculateNewEconomyState(previousState)
            
            // Update house
            houseRepository.updateEconomy(house.id, newState)
            
            // Create event
            eventRepository.create(
                type = GameEventType.ECONOMY_UPDATE,
                targetHouseIds = listOf(house.id),
                payload = mapOf(
                    "houseId" to house.id,
                    "houseName" to house.name,
                    "previousBalance" to previousState.solariBalance.toString(),
                    "newBalance" to newState.solariBalance.toString(),
                    "change" to (newState.solariBalance - previousState.solariBalance).toString()
                ),
                requiresNotification = true,
                createdBy = adminUserId
            )
            
            updates[house.name] = mapOf(
                "previousBalance" to previousState.solariBalance,
                "newBalance" to newState.solariBalance,
                "change" to (newState.solariBalance - previousState.solariBalance)
            )
        }
        
        return updates
    }
    
    private fun calculateNewEconomyState(current: EconomyState): EconomyState {
        val netIncome = current.incomePerCycle - current.expensesPerCycle
        val newBalance = (current.solariBalance + netIncome).coerceAtLeast(0.0)
        
        return current.copy(
            solariBalance = newBalance
        )
    }
    
    /**
     * Manual adjustment of house economy by admin
     */
    suspend fun adjustEconomy(
        houseId: String,
        spiceAdjustment: Double? = null,
        solarisAdjustment: Double? = null,
        incomeAdjustment: Double? = null,
        expenseAdjustment: Double? = null,
        adminUserId: String
    ): Boolean {
        val house = houseRepository.getHouseById(houseId) ?: return false
        val economy = house.economyState
        
        val newEconomy = economy.copy(
            spiceReserves = spiceAdjustment?.let { economy.spiceReserves + it } ?: economy.spiceReserves,
            solariBalance = solarisAdjustment?.let { economy.solariBalance + it } ?: economy.solariBalance,
            incomePerCycle = incomeAdjustment?.let { economy.incomePerCycle + it } ?: economy.incomePerCycle,
            expensesPerCycle = expenseAdjustment?.let { economy.expensesPerCycle + it } ?: economy.expensesPerCycle
        )
        
        houseRepository.updateEconomy(houseId, newEconomy)
        
        // Create event
        eventRepository.create(
            type = GameEventType.ECONOMY_UPDATE,
            targetHouseIds = listOf(houseId),
            payload = mapOf(
                "houseId" to houseId,
                "houseName" to house.name,
                "adjustmentType" to "manual",
                "spiceAdjustment" to (spiceAdjustment?.toString() ?: "0"),
                "solarisAdjustment" to (solarisAdjustment?.toString() ?: "0")
            ),
            requiresNotification = true,
            createdBy = adminUserId
        )
        
        return true
    }
}
