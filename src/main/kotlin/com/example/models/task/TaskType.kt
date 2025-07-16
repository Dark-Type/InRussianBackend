package com.example.models.task

import kotlinx.serialization.Serializable

@Serializable
enum class TaskType {
    LISTEN_AND_CHOOSE,
    READ_AND_CHOOSE,
    LOOK_AND_CHOOSE,
    MATCH_AUDIO_TEXT,
    MATCH_TEXT_TEXT
}
