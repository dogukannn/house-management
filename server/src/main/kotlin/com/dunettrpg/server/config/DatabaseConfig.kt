package com.dunettrpg.server.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import com.dunettrpg.server.data.tables.*

object DatabaseConfig {
    private lateinit var dataSource: HikariDataSource
    
    fun init() {
        val databaseUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/dune_ttrpg"
        val databaseUser = System.getenv("DATABASE_USER") ?: "dune_app"
        val databasePassword = System.getenv("DATABASE_PASSWORD") ?: "password"
        
        val config = HikariConfig().apply {
            jdbcUrl = databaseUrl
            username = databaseUser
            password = databasePassword
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        
        dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        
        // Create tables
        transaction {
            SchemaUtils.create(
                UsersTable,
                HousesTable,
                CharactersTable,
                ArmiesTable,
                VotesTable,
                TradeDealsTable,
                GameEventsTable
            )
        }
    }
    
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
