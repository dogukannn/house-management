package com.dunettrpg.server.data.repository

import com.dunettrpg.server.data.tables.CharactersTable
import com.dunettrpg.server.domain.model.Character
import com.dunettrpg.server.domain.model.CharacterStats
import com.dunettrpg.server.domain.model.CharacterStatus
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class CharacterRepository {
    
    fun getCharactersByHouseId(houseId: String): List<Character> = transaction {
        CharactersTable.selectAll().where { CharactersTable.houseId eq UUID.fromString(houseId) }
            .map { rowToCharacter(it) }
    }
    
    fun getCharacterById(id: String): Character? = transaction {
        CharactersTable.selectAll().where { CharactersTable.id eq UUID.fromString(id) }
            .map { rowToCharacter(it) }
            .singleOrNull()
    }
    
    fun createCharacter(houseId: String, name: String, role: String, stats: CharacterStats, status: CharacterStatus, notes: String?, portraitUrl: String?): Character = transaction {
        val id = CharactersTable.insert {
            it[CharactersTable.houseId] = UUID.fromString(houseId)
            it[CharactersTable.name] = name
            it[CharactersTable.role] = role
            it[CharactersTable.stats] = Json.encodeToString(CharacterStats.serializer(), stats)
            it[CharactersTable.status] = status.name
            it[CharactersTable.notes] = notes
            it[CharactersTable.portraitUrl] = portraitUrl
        } get CharactersTable.id
        
        getCharacterById(id.toString())!!
    }
    
    fun updateCharacter(id: String, name: String?, role: String?, stats: CharacterStats?, status: CharacterStatus?, notes: String?, portraitUrl: String?): Character? = transaction {
        val characterId = UUID.fromString(id)
        val updates = mutableMapOf<Column<*>, Any?>()
        
        name?.let { updates[CharactersTable.name] = it }
        role?.let { updates[CharactersTable.role] = it }
        stats?.let { updates[CharactersTable.stats] = Json.encodeToString(CharacterStats.serializer(), it) }
        status?.let { updates[CharactersTable.status] = it.name }
        notes?.let { updates[CharactersTable.notes] = it }
        portraitUrl?.let { updates[CharactersTable.portraitUrl] = it }
        
        CharactersTable.update({ CharactersTable.id eq characterId }) {
            updates.forEach { (column, value) ->
                it[column as Column<Any?>] = value
            }
        }
        
        getCharacterById(id)
    }
    
    fun deleteCharacter(id: String): Boolean = transaction {
        CharactersTable.deleteWhere { CharactersTable.id eq UUID.fromString(id) } > 0
    }
    
    private fun rowToCharacter(row: ResultRow): Character {
        val statsJson = row[CharactersTable.stats]
        val stats = Json.decodeFromString(CharacterStats.serializer(), statsJson)
        
        return Character(
            id = row[CharactersTable.id].toString(),
            houseId = row[CharactersTable.houseId].toString(),
            name = row[CharactersTable.name],
            role = row[CharactersTable.role],
            stats = stats,
            status = CharacterStatus.valueOf(row[CharactersTable.status]),
            notes = row[CharactersTable.notes],
            portraitUrl = row[CharactersTable.portraitUrl]
        )
    }
}
