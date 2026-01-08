package com.dunettrpg.server.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object GameEventsTable : Table("game_events") {
    val id = uuid("id").autoGenerate()
    val type = varchar("type", 50)
    val targetHouseIds = text("target_house_ids") // JSON array
    val payload = text("payload") // JSON
    val requiresNotification = bool("requires_notification").default(false)
    val notificationSent = bool("notification_sent").default(false)
    val createdAt = timestamp("created_at")
    val createdBy = uuid("created_by")
    
    override val primaryKey = PrimaryKey(id)
}
