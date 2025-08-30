package com.inRussian.models.content

import com.inRussian.models.tasks.*
import kotlinx.serialization.Serializable

@Serializable
data class Section(
    val id: String,
    val courseId: String,
    val name: String,
    val description: String?,
    val orderNum: Int,
    val createdAt: String
)

@Serializable
data class Course(
    val id: String,
    val name: String,
    val posterId: String?,
    val description: String?,
    val authorId: String,
    val authorUrl: String?,
    val language: String,
    val isPublished: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CountStats(
    val coursesCount: Long,
    val sectionsCount: Long,
    val themesCount: Long,
    val tasksCount: Long
)

@Serializable
data class TaskWithDetails(
    val id: String,
    val themeId: String,
    val name: String,
    val taskType: TaskType,
    val question: String,
    val instructions: String?,
    val isTraining: Boolean,
    val orderNum: Int,
    val createdAt: String,
    val content: List<TaskContentItem> = emptyList(),
    val answer: TaskAnswerItem? = null,
    val answerOptions: List<TaskAnswerOptionItem> = emptyList()
)