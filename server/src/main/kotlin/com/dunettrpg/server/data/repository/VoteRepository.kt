package com.dunettrpg.server.data.repository

import com.dunettrpg.server.data.tables.VotesTable
import com.dunettrpg.server.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.kotlinx.datetime.Clock.System.now()
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object VoteRepository {
    
    fun getAllVotes(): List<Vote> = transaction {
        VotesTable.selectAll().map { rowToVote(it) }
    }
    
    fun getVoteById(id: String): Vote? = transaction {
        VotesTable.selectAll().where { VotesTable.id eq UUID.fromString(id) }
            .map { rowToVote(it) }
            .singleOrNull()
    }
    
    fun getVotesByStatus(status: VoteStatus): List<Vote> = transaction {
        VotesTable.selectAll().where { VotesTable.status eq status.name }
            .map { rowToVote(it) }
    }
    
    fun getPendingVotesForHouse(houseId: String): List<Vote> = transaction {
        VotesTable.selectAll().where { VotesTable.status eq VoteStatus.PENDING.name }
            .map { rowToVote(it) }
            .filter { it.requiredParticipants.contains(houseId) }
    }
    
    fun createVote(
        type: VoteType,
        title: String,
        description: String,
        initiatorHouseId: String,
        requiredParticipants: List<String>,
        consensusRequired: Boolean,
        deadline: kotlinx.datetime.Instant?
    ): Vote = transaction {
        val id = UUID.randomUUID()
        VotesTable.insert {
            it[VotesTable.id] = id
            it[VotesTable.type] = type.name
            it[VotesTable.title] = title
            it[VotesTable.description] = description
            it[VotesTable.initiatorHouseId] = UUID.fromString(initiatorHouseId)
            it[VotesTable.requiredParticipants] = Json.encodeToString(requiredParticipants)
            it[VotesTable.votes] = Json.encodeToString(emptyMap<String, VoteDecision>())
            it[VotesTable.consensusRequired] = consensusRequired
            it[VotesTable.deadline] = deadline
            it[VotesTable.status] = VoteStatus.PENDING.name
            it[VotesTable.result] = null
            it[VotesTable.createdAt] = kotlinx.datetime.Clock.System.now()
            it[VotesTable.resolvedAt] = null
        }
        getVoteById(id.toString())!!
    }
    
    fun castVote(voteId: String, houseId: String, decision: Decision): Vote? = transaction {
        val vote = getVoteById(voteId) ?: return@transaction null
        
        if (vote.status != VoteStatus.PENDING) {
            return@transaction null // Cannot vote on non-pending votes
        }
        
        if (!vote.requiredParticipants.contains(houseId)) {
            return@transaction null // House not a participant
        }
        
        val updatedVotes = vote.votes.toMutableMap()
        updatedVotes[houseId] = VoteDecision(
            decision = decision,
            timestamp = kotlinx.datetime.Clock.System.now().toString()
        )
        
        VotesTable.update({ VotesTable.id eq UUID.fromString(voteId) }) {
            it[votes] = Json.encodeToString(updatedVotes)
        }
        
        // Check if voting is complete and resolve if needed
        val updatedVote = getVoteById(voteId)!!
        resolveVoteIfComplete(updatedVote)
    }
    
    fun cancelVote(voteId: String): Vote? = transaction {
        VotesTable.update({ VotesTable.id eq UUID.fromString(voteId) }) {
            it[status] = VoteStatus.CANCELLED.name
            it[resolvedAt] = kotlinx.datetime.Clock.System.now()
        }
        getVoteById(voteId)
    }
    
    private fun resolveVoteIfComplete(vote: Vote): Vote? {
        if (vote.status != VoteStatus.PENDING) return vote
        
        // Check if all participants have voted
        val allVoted = vote.requiredParticipants.all { vote.votes.containsKey(it) }
        if (!allVoted) return vote
        
        // Determine result
        val decisions = vote.votes.values.map { it.decision }
        val newStatus = if (vote.consensusRequired) {
            // Consensus: all must approve
            if (decisions.all { it == Decision.APPROVE }) VoteStatus.PASSED
            else VoteStatus.FAILED
        } else {
            // Majority: more approvals than rejections
            val approvals = decisions.count { it == Decision.APPROVE }
            val rejections = decisions.count { it == Decision.REJECT }
            if (approvals > rejections) VoteStatus.PASSED
            else VoteStatus.FAILED
        }
        
        transaction {
            VotesTable.update({ VotesTable.id eq UUID.fromString(vote.id) }) {
                it[status] = newStatus.name
                it[resolvedAt] = kotlinx.datetime.Clock.System.now()
                it[result] = Json.encodeToString(mapOf(
                    "outcome" to newStatus.name,
                    "votes" to vote.votes
                ))
            }
        }
        
        return getVoteById(vote.id)
    }
    
    private fun rowToVote(row: ResultRow): Vote {
        return Vote(
            id = row[VotesTable.id].toString(),
            type = VoteType.valueOf(row[VotesTable.type]),
            title = row[VotesTable.title],
            description = row[VotesTable.description],
            initiatorHouseId = row[VotesTable.initiatorHouseId].toString(),
            requiredParticipants = Json.decodeFromString(row[VotesTable.requiredParticipants]),
            votes = Json.decodeFromString(row[VotesTable.votes]),
            consensusRequired = row[VotesTable.consensusRequired],
            deadline = row[VotesTable.deadline]?.toString(),
            status = VoteStatus.valueOf(row[VotesTable.status]),
            result = row[VotesTable.result],
            createdAt = row[VotesTable.createdAt].toString(),
            resolvedAt = row[VotesTable.resolvedAt]?.toString()
        )
    }
}
