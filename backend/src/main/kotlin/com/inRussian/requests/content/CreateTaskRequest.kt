package com.inRussian.requests.content

import com.inRussian.models.tasks.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreateTaskRequest(
    val themeId: String,
    val name: String,
    val taskType: TaskType,
    val question: String,
    val instructions: String?,
    val isTraining: Boolean = false,
    val orderNum: Int
)

@Serializable
data class UpdateTaskRequest(
    val name: String?,
    val question: String?,
    val instructions: String?,
    val isTraining: Boolean?,
    val orderNum: Int?
)

@Serializable
data class CreateTaskContentRequest(
    val contentType: ContentType,
    val contentId: String?,
    val description: String?,
    val transcription: String?,
    val translation: String?,
    val orderNum: Int
)

@Serializable
data class UpdateTaskContentRequest(
    val contentType: ContentType?,
    val contentId: String?,
    val description: String?,
    val transcription: String?,
    val translation: String?,
    val orderNum: Int?
)

@Serializable
data class CreateTaskAnswerRequest(
    val answerType: AnswerType,
    val correctAnswer: JsonElement
)

@Serializable
data class UpdateTaskAnswerRequest(
    val answerType: AnswerType?,
    val correctAnswer: JsonElement?
)

@Serializable
data class CreateTaskAnswerOptionRequest(
    val optionText: String?,
    val optionAudioId: String?,
    val isCorrect: Boolean = false,
    val orderNum: Int
)

@Serializable
data class UpdateTaskAnswerOptionRequest(
    val optionText: String?,
    val optionAudioId: String?,
    val isCorrect: Boolean?,
    val orderNum: Int?
)

@Serializable
data class CreateThemeRequest(
    val sectionId: String,
    val name: String,
    val description: String?,
    val orderNum: Int
)

@Serializable
data class UpdateThemeRequest(
    val name: String?,
    val description: String?,
    val orderNum: Int?
)

@Serializable
data class CreateSectionRequest(
    val courseId: String,
    val name: String,
    val description: String?,
    val orderNum: Int
)

@Serializable
data class UpdateSectionRequest(
    val name: String?,
    val description: String?,
    val orderNum: Int?
)

@Serializable
data class CreateCourseRequest(
    val name: String,
    val description: String?,
    val coursePoster: String?,
    val authorUrl: String?,
    val language: String,
    val isPublished: Boolean = false
)

@Serializable
data class UpdateCourseRequest(
    val name: String?,
    val description: String?,
    val authorUrl: String?,
    val language: String?,
    val coursePoster: String?,
    val isPublished: Boolean?
)

@Serializable
data class CreateReportRequest(
    val description: String,
    val taskId: String
)