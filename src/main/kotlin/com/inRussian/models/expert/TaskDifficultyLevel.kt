package com.inRussian.models.expert

import kotlinx.serialization.Serializable

@Serializable
enum class TaskDifficultyLevel {
    VERY_EASY, EASY, MEDIUM, HARD, VERY_HARD
}