package com.dunettrpg.server.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UsersTable : Table("users") {
    val id = uuid("id").autoGenerate()
    val username = varchar("username", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 20) // ADMIN, PLAYER
    val fcmToken = varchar("fcm_token", 255).nullable()
    val houseId = uuid("house_id").nullable()
    val createdAt = timestamp("created_at")
    val lastActiveAt = timestamp("last_active_at")
    
    override val primaryKey = PrimaryKey(id)
}
