package com.inRussian.models.tasks

import kotlinx.serialization.Serializable

@Serializable
enum class ContentType {
    AUDIO, IMAGE, TEXT, VIDEO, AVATAR
}