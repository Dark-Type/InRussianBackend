package com.inRussian.models.badge

import kotlinx.serialization.Serializable

@Serializable
enum class BadgeType {
    COURSE_COMPLETION, THEME_COMPLETION, STREAK, ACHIEVEMENT
}