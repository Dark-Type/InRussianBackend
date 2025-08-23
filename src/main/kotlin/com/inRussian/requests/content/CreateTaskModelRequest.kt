package com.inRussian.requests.content

import com.inRussian.models.tasks.TaskBody
import com.inRussian.models.tasks.TaskType
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import java.util.UUID
import kotlin.time.Instant

@Serializable
data class CreateTaskModelRequest(
    val courseId: String,
    val taskBody: TaskBody,
    val taskTypes: List<TaskType>
)
