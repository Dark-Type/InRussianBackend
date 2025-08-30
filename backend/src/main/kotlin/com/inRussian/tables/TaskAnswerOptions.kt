package com.inRussian.tables

import org.jetbrains.exposed.dao.id.UUIDTable


object TaskAnswerOptions : UUIDTable("task_answer_options") {
    val taskId = reference("task_id", Tasks)
    val optionText = text("option_text").nullable()
    val optionAudioId = varchar("option_audio_id", 255).nullable()
    val isCorrect = bool("is_correct").default(false)
    val orderNum = integer("order_num")
}