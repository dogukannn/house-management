package com.dunettrpg.server.domain.service

import com.dunettrpg.server.data.repository.ArmyRepository
import com.dunettrpg.server.data.repository.HouseRepository
import com.dunettrpg.server.data.repository.TradeRepository
import com.dunettrpg.server.domain.model.*
import kotlinx.serialization.Serializable

object EconomyService {
    
    // Army maintenance costs per unit per cycle
    private const val INFANTRY_COST = 10.0
    private const val SARDAUKAR_COST = 50.0
    private const val FREMEN_COST = 25.0
    private const val ORNITHOPTER_COST = 100.0
    
    /**
     * Executes an economy cycle for all houses
     * Returns a list of economy cycle results
     */
    fun executeEconomyCycle(): List<EconomyCycleResult> {
        val results = mutableListOf<EconomyCycleResult>()
        val houses = HouseRepository.getAllHouses()
        
        for (house in houses) {
            val result = processHouseEconomy(house)
            results.add(result)
        }
        
        // Process active trades (recurring trades)
        processActiveTrades()
        
        return results
    }
    
    /**
     * Process economy for a single house
     */
    fun processHouseEconomy(house: House): EconomyCycleResult {
        val previousState = house.economyState
        
        // Calculate base income (from planetary fief and political standing)
        val baseIncome = calculateBaseIncome(house)
        
        // Calculate trade income
        val tradeIncome = calculateTradeIncome(house)
        
        // Calculate army maintenance
        val armyMaintenance = calculateArmyMaintenance(house)
        
        // Calculate total income and expenses
        val totalIncome = baseIncome + tradeIncome
        val totalExpenses = armyMaintenance
        val netChange = totalIncome - totalExpenses
        
        // Apply changes to house economy
        val newSolariBalance = (previousState.solariBalance + netChange).coerceAtLeast(0.0)
        
        val newEconomyState = previousState.copy(
            solariBalance = newSolariBalance,
            incomePerCycle = totalIncome,
            expensesPerCycle = totalExpenses
        )
        
        // Update house
        HouseRepository.updateHouse(house.id, null, null, newEconomyState, null)
        
        return EconomyCycleResult(
            houseId = house.id,
            houseName = house.name,
            previousState = previousState,
            newState = newEconomyState,
            baseIncome = baseIncome,
            tradeIncome = tradeIncome,
            armyMaintenance = armyMaintenance,
            netChange = netChange,
            isBankrupt = newSolariBalance <= 0.0
        )
    }
    
    /**
     * Calculate base income from planetary fief and political standing
     */
    private fun calculateBaseIncome(house: House): Double {
        val baseProduction = 3000.0 // Base planetary production
        
        // Political standing modifier (-100 to +100)
        // At 0 standing: 100% income
        // At +100 standing: 150% income  
        // At -100 standing: 50% income
        val standingModifier = 1.0 + (house.politicalStanding / 200.0)
        
        return baseProduction * standingModifier
    }
    
    /**
     * Calculate income from active trade deals
     */
    private fun calculateTradeIncome(house: House): Double {
        val trades = TradeRepository.getTradesByHouseId(house.id)
        var totalIncome = 0.0
        
        for (trade in trades.filter { it.status == TradeStatus.ACTIVE }) {
            // If this house is receiving, add the value
            if (trade.toHouseId == house.id) {
                val duration = trade.duration ?: 1
                // Protect against division by zero
                if (duration > 0) {
                    totalIncome += (trade.offering.solaris / duration)
                }
            }
        }
        
        return totalIncome
    }
    
    /**
     * Calculate army maintenance costs
     */
    private fun calculateArmyMaintenance(house: House): Double {
        val armies = ArmyRepository.getArmiesByHouseId(house.id)
        var totalCost = 0.0
        
        for (army in armies) {
            totalCost += army.units.infantry * INFANTRY_COST
            totalCost += army.units.sardaukar * SARDAUKAR_COST
            totalCost += army.units.fremen * FREMEN_COST
            totalCost += army.units.ornithopters * ORNITHOPTER_COST
        }
        
        return totalCost
    }
    
    /**
     * Process recurring active trades
     */
    private fun processActiveTrades() {
        val activeTrades = TradeRepository.getTradesByStatus(TradeStatus.ACTIVE)
        
        for (trade in activeTrades) {
            // Check if trade should expire
            if (trade.expiresAt != null) {
                // TODO: Compare with current time and mark as COMPLETED if expired
                // For now, we'll keep it simple
            }
        }
    }
    
    /**
     * Manual economy adjustment (admin only)
     */
    fun adjustHouseEconomy(
        houseId: String,
        spiceChange: Double? = null,
        solarisChange: Double? = null,
        reason: String
    ): House? {
        val house = HouseRepository.getHouseById(houseId) ?: return null
        
        val newEconomyState = house.economyState.copy(
            spiceReserves = house.economyState.spiceReserves + (spiceChange ?: 0.0),
            solariBalance = house.economyState.solariBalance + (solarisChange ?: 0.0)
        )
        
        return HouseRepository.updateHouse(houseId, null, null, newEconomyState, null)
    }
}

@Serializable
data class EconomyCycleResult(
    val houseId: String,
    val houseName: String,
    val previousState: EconomyState,
    val newState: EconomyState,
    val baseIncome: Double,
    val tradeIncome: Double,
    val armyMaintenance: Double,
    val netChange: Double,
    val isBankrupt: Boolean
)
