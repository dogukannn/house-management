package com.dunettrpg.server.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    val id: String,
    val type: VoteType,
    val title: String,
    val description: String,
    val initiatorHouseId: String,
    val requiredParticipants: List<String>, // List of house IDs
    val votes: Map<String, VoteDecision>, // Map of houseId to decision
    val consensusRequired: Boolean,
    val deadline: String? = null,
    val status: VoteStatus,
    val result: VoteResult? = null,
    val createdAt: String,
    val resolvedAt: String? = null
)

@Serializable
enum class VoteType {
    TRADE_DEAL,
    ALLIANCE,
    WAR_DECLARATION,
    LANDSRAAD_MOTION,
    CUSTOM
}

@Serializable
enum class VoteStatus {
    PENDING,
    PASSED,
    FAILED,
    EXPIRED,
    CANCELLED
}

@Serializable
data class VoteDecision(
    val decision: Decision,
    val timestamp: String
)

@Serializable
enum class Decision {
    APPROVE,
    REJECT,
    ABSTAIN
}

@Serializable
data class VoteResult(
    val outcome: String,
    val approvalCount: Int,
    val rejectionCount: Int,
    val abstainCount: Int,
    val effects: List<String> = emptyList()
)
