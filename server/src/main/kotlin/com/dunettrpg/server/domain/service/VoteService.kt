package com.dunettrpg.server.domain.service

import com.dunettrpg.server.data.repository.EventRepository
import com.dunettrpg.server.data.repository.VoteRepository
import com.dunettrpg.server.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class VoteService(
    private val voteRepository: VoteRepository,
    private val eventRepository: EventRepository
) {
    
    suspend fun createVote(
        type: VoteType,
        title: String,
        description: String,
        initiatorHouseId: String,
        requiredParticipants: List<String>,
        consensusRequired: Boolean,
        deadline: Instant? = null,
        createdBy: String
    ): Vote? {
        val vote = voteRepository.create(
            type, title, description, initiatorHouseId,
            requiredParticipants, consensusRequired, deadline
        )
        
        if (vote != null) {
            // Create event for vote creation
            eventRepository.create(
                type = GameEventType.VOTE_STARTED,
                targetHouseIds = requiredParticipants,
                payload = mapOf(
                    "voteId" to vote.id,
                    "title" to title,
                    "type" to type.name,
                    "initiator" to initiatorHouseId
                ),
                requiresNotification = true,
                createdBy = createdBy
            )
        }
        
        return vote
    }
    
    suspend fun castVote(voteId: String, houseId: String, decision: Decision): Boolean {
        val success = voteRepository.castVote(voteId, houseId, decision)
        
        if (success) {
            // Check if vote should be resolved
            val vote = voteRepository.getById(voteId)
            if (vote != null && shouldResolveVote(vote)) {
                resolveVote(vote)
            }
        }
        
        return success
    }
    
    private fun shouldResolveVote(vote: Vote): Boolean {
        // Check if all required participants have voted
        val votedHouses = vote.votes.keys
        return vote.requiredParticipants.all { it in votedHouses }
    }
    
    private suspend fun resolveVote(vote: Vote) {
        val approvals = vote.votes.values.count { it.decision == Decision.APPROVE }
        val rejections = vote.votes.values.count { it.decision == Decision.REJECT }
        val abstains = vote.votes.values.count { it.decision == Decision.ABSTAIN }
        
        val passed = if (vote.consensusRequired) {
            // Consensus: all must approve (abstentions don't count)
            rejections == 0 && approvals > 0
        } else {
            // Majority: more approvals than rejections
            approvals > rejections
        }
        
        val status = if (passed) VoteStatus.PASSED else VoteStatus.FAILED
        val result = VoteResult(
            outcome = if (passed) "PASSED" else "FAILED",
            approvalCount = approvals,
            rejectionCount = rejections,
            abstainCount = abstains
        )
        
        voteRepository.resolveVote(vote.id, result, status)
        
        // Create event for vote resolution
        eventRepository.create(
            type = GameEventType.VOTE_RESOLVED,
            targetHouseIds = vote.requiredParticipants,
            payload = mapOf(
                "voteId" to vote.id,
                "title" to vote.title,
                "outcome" to result.outcome,
                "approvals" to approvals.toString(),
                "rejections" to rejections.toString()
            ),
            requiresNotification = true,
            createdBy = vote.initiatorHouseId
        )
    }
    
    suspend fun cancelVote(voteId: String, requestingHouseId: String): Boolean {
        val vote = voteRepository.getById(voteId) ?: return false
        
        // Only initiator or admin can cancel
        if (vote.initiatorHouseId != requestingHouseId) {
            return false
        }
        
        return voteRepository.cancelVote(voteId)
    }
}
