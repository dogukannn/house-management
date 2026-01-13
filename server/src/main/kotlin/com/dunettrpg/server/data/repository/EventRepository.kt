package com.dunettrpg.server.data.repository

import com.dunettrpg.server.config.DatabaseConfig.dbQuery
import com.dunettrpg.server.data.tables.GameEventsTable
import com.dunettrpg.server.domain.model.GameEvent
import com.dunettrpg.server.domain.model.GameEventType
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import java.util.UUID

class EventRepository {
    
    suspend fun getAll(limit: Int = 100, offset: Long = 0): List<GameEvent> = dbQuery {
        GameEventsTable.selectAll()
            .orderBy(GameEventsTable.createdAt, SortOrder.DESC)
            .limit(limit, offset)
            .map { toGameEvent(it) }
    }
    
    suspend fun getById(id: String): GameEvent? = dbQuery {
        GameEventsTable.selectAll().where { GameEventsTable.id eq UUID.fromString(id) }
            .mapNotNull { toGameEvent(it) }
            .singleOrNull()
    }
    
    suspend fun getForHouse(houseId: String, limit: Int = 100): List<GameEvent> = dbQuery {
        GameEventsTable.selectAll().where {
            (GameEventsTable.targetHouseIds eq "[]") or  // All houses
            (GameEventsTable.targetHouseIds like "%$houseId%")
        }
        .orderBy(GameEventsTable.createdAt, SortOrder.DESC)
        .limit(limit)
        .map { toGameEvent(it) }
    }
    
    suspend fun getByType(type: GameEventType): List<GameEvent> = dbQuery {
        GameEventsTable.selectAll().where { GameEventsTable.type eq type.name }
            .orderBy(GameEventsTable.createdAt, SortOrder.DESC)
            .map { toGameEvent(it) }
    }
    
    suspend fun create(
        type: GameEventType,
        targetHouseIds: List<String>,
        payload: Map<String, String>,
        requiresNotification: Boolean,
        createdBy: String
    ): GameEvent? = dbQuery {
        val id = GameEventsTable.insert {
            it[GameEventsTable.type] = type.name
            it[GameEventsTable.targetHouseIds] = Json.encodeToString(targetHouseIds)
            it[GameEventsTable.payload] = Json.encodeToString(payload)
            it[GameEventsTable.requiresNotification] = requiresNotification
            it[notificationSent] = false
            it[createdAt] = Clock.System.now()
            it[GameEventsTable.createdBy] = UUID.fromString(createdBy)
        } get GameEventsTable.id
        
        getById(id.toString())
    }
    
    suspend fun markNotificationSent(eventId: String): Boolean = dbQuery {
        GameEventsTable.update({ GameEventsTable.id eq UUID.fromString(eventId) }) {
            it[notificationSent] = true
        } > 0
    }
    
    private fun toGameEvent(row: ResultRow): GameEvent {
        return GameEvent(
            id = row[GameEventsTable.id].toString(),
            type = GameEventType.valueOf(row[GameEventsTable.type]),
            targetHouseIds = Json.decodeFromString(row[GameEventsTable.targetHouseIds]),
            payload = Json.decodeFromString(row[GameEventsTable.payload]),
            requiresNotification = row[GameEventsTable.requiresNotification],
            notificationSent = row[GameEventsTable.notificationSent],
            createdAt = row[GameEventsTable.createdAt].toString(),
            createdBy = row[GameEventsTable.createdBy].toString()
        )
    }
}
