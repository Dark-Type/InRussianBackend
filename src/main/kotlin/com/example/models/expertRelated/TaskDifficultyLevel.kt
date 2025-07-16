package com.example.models.expertRelated

import kotlinx.serialization.Serializable

@Serializable
enum class TaskDifficultyLevel {
    VERY_EASY, EASY, MEDIUM, HARD, VERY_HARD
}