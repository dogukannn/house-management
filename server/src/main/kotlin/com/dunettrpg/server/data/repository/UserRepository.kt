package com.dunettrpg.server.data.repository

import com.dunettrpg.server.config.DatabaseConfig.dbQuery
import com.dunettrpg.server.data.tables.UsersTable
import com.dunettrpg.server.domain.model.User
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import java.util.UUID

class UserRepository {
    
    suspend fun findByUsername(username: String): User? = dbQuery {
        UsersTable.select { UsersTable.username eq username }
            .mapNotNull { toUser(it) }
            .singleOrNull()
    }
    
    suspend fun findById(id: String): User? = dbQuery {
        UsersTable.select { UsersTable.id eq UUID.fromString(id) }
            .mapNotNull { toUser(it) }
            .singleOrNull()
    }
    
    suspend fun getPasswordHash(username: String): String? = dbQuery {
        UsersTable.select { UsersTable.username eq username }
            .map { it[UsersTable.passwordHash] }
            .singleOrNull()
    }
    
    suspend fun create(username: String, passwordHash: String, role: String, houseId: String? = null): User? = dbQuery {
        val id = UsersTable.insert {
            it[UsersTable.username] = username
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role] = role
            it[UsersTable.houseId] = houseId?.let { UUID.fromString(it) }
            it[createdAt] = Clock.System.now()
            it[lastActiveAt] = Clock.System.now()
        } get UsersTable.id
        
        findById(id.toString())
    }
    
    suspend fun updateFcmToken(userId: String, token: String?) = dbQuery {
        UsersTable.update({ UsersTable.id eq UUID.fromString(userId) }) {
            it[fcmToken] = token
        }
    }
    
    suspend fun updateLastActive(userId: String) = dbQuery {
        UsersTable.update({ UsersTable.id eq UUID.fromString(userId) }) {
            it[lastActiveAt] = Clock.System.now()
        }
    }
    
    private fun toUser(row: ResultRow): User {
        return User(
            id = row[UsersTable.id].toString(),
            username = row[UsersTable.username],
            role = row[UsersTable.role],
            houseId = row[UsersTable.houseId]?.toString()
        )
    }
}
