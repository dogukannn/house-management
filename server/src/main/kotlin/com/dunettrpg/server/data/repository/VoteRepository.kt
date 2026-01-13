package com.dunettrpg.server.data.repository

import com.dunettrpg.server.config.DatabaseConfig.dbQuery
import com.dunettrpg.server.data.tables.VotesTable
import com.dunettrpg.server.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import java.util.UUID

class VoteRepository {
    
    suspend fun getAll(): List<Vote> = dbQuery {
        VotesTable.selectAll().map { toVote(it) }
    }
    
    suspend fun getById(id: String): Vote? = dbQuery {
        VotesTable.selectAll().where { VotesTable.id eq UUID.fromString(id) }
            .mapNotNull { toVote(it) }
            .singleOrNull()
    }
    
    suspend fun getByStatus(status: VoteStatus): List<Vote> = dbQuery {
        VotesTable.selectAll().where { VotesTable.status eq status.name }
            .map { toVote(it) }
    }
    
    suspend fun getPendingForHouse(houseId: String): List<Vote> = dbQuery {
        VotesTable.selectAll().where { 
            (VotesTable.status eq VoteStatus.PENDING.name) and
            (VotesTable.requiredParticipants like "%$houseId%")
        }.map { toVote(it) }
    }
    
    suspend fun create(
        type: VoteType,
        title: String,
        description: String,
        initiatorHouseId: String,
        requiredParticipants: List<String>,
        consensusRequired: Boolean,
        deadline: Instant? = null
    ): Vote? = dbQuery {
        val id = VotesTable.insert {
            it[VotesTable.type] = type.name
            it[VotesTable.title] = title
            it[VotesTable.description] = description
            it[VotesTable.initiatorHouseId] = UUID.fromString(initiatorHouseId)
            it[VotesTable.requiredParticipants] = Json.encodeToString(requiredParticipants)
            it[VotesTable.votes] = "{}"
            it[VotesTable.consensusRequired] = consensusRequired
            it[VotesTable.deadline] = deadline
            it[VotesTable.status] = VoteStatus.PENDING.name
            it[VotesTable.createdAt] = Clock.System.now()
        } get VotesTable.id
        
        getById(id.toString())
    }
    
    suspend fun castVote(voteId: String, houseId: String, decision: Decision): Boolean = dbQuery {
        val vote = getById(voteId) ?: return@dbQuery false
        
        if (vote.status != VoteStatus.PENDING) {
            return@dbQuery false
        }
        
        if (!vote.requiredParticipants.contains(houseId)) {
            return@dbQuery false
        }
        
        val updatedVotes = vote.votes.toMutableMap()
        updatedVotes[houseId] = VoteDecision(decision, Clock.System.now().toString())
        
        VotesTable.update({ VotesTable.id eq UUID.fromString(voteId) }) {
            it[votes] = Json.encodeToString(updatedVotes)
        }
        
        true
    }
    
    suspend fun resolveVote(voteId: String, result: VoteResult, status: VoteStatus): Boolean = dbQuery {
        VotesTable.update({ VotesTable.id eq UUID.fromString(voteId) }) {
            it[VotesTable.status] = status.name
            it[VotesTable.result] = Json.encodeToString(result)
            it[resolvedAt] = Clock.System.now()
        } > 0
    }
    
    suspend fun cancelVote(voteId: String): Boolean = dbQuery {
        VotesTable.update({ VotesTable.id eq UUID.fromString(voteId) }) {
            it[status] = VoteStatus.CANCELLED.name
            it[resolvedAt] = Clock.System.now()
        } > 0
    }
    
    private fun toVote(row: ResultRow): Vote {
        val votesMap = try {
            Json.decodeFromString<Map<String, VoteDecision>>(row[VotesTable.votes])
        } catch (e: Exception) {
            emptyMap()
        }
        
        val resultData = row[VotesTable.result]?.let {
            try {
                Json.decodeFromString<VoteResult>(it)
            } catch (e: Exception) {
                null
            }
        }
        
        return Vote(
            id = row[VotesTable.id].toString(),
            type = VoteType.valueOf(row[VotesTable.type]),
            title = row[VotesTable.title],
            description = row[VotesTable.description],
            initiatorHouseId = row[VotesTable.initiatorHouseId].toString(),
            requiredParticipants = Json.decodeFromString(row[VotesTable.requiredParticipants]),
            votes = votesMap,
            consensusRequired = row[VotesTable.consensusRequired],
            deadline = row[VotesTable.deadline]?.toString(),
            status = VoteStatus.valueOf(row[VotesTable.status]),
            result = resultData,
            createdAt = row[VotesTable.createdAt].toString(),
            resolvedAt = row[VotesTable.resolvedAt]?.toString()
        )
    }
}
