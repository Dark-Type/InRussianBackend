package com.inRussian.tables

import com.inRussian.models.tasks.AnswerType
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.json.jsonb


object TaskAnswers : UUIDTable("task_answers") {
    val taskId = reference("task_id", Tasks).uniqueIndex()
    val answerType = enumerationByName("answer_type", 30, AnswerType::class)
    val correctAnswer = jsonb<JsonElement>("correct_answer", json)
}