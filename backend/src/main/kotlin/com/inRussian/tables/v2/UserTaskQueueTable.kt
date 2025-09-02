package com.inRussian.tables.v2

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

enum class QueueState { QUEUED, RESERVED, DONE, EXPIRED }

object UserSectionQueueStateTable : Table("user_section_queue_state") {
    val userId = uuid("user_id")
    val sectionId = uuid("section_id")
    val lastSeededOrderNum = integer("last_seeded_order_num").nullable()
    val lastPosition = long("last_position").default(0L)
    val completed = bool("completed").default(false)

    override val primaryKey = PrimaryKey(userId, sectionId, name = "pk_user_section_queue_state")
}


object UserSectionQueueItemTable : UUIDTable("user_section_queue_item") {
    val userId = uuid("user_id").index()
    val sectionId = uuid("section_id").index()
    val taskId = uuid("task_id").index()
    val position = long("position").index()
    val enqueuedAt = timestamp("enqueued_at").defaultExpression(CurrentTimestamp)

    init {
        uniqueIndex("uq_user_section_task", userId, sectionId, taskId)
    }
}