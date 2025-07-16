package com.example.models.task

import kotlinx.serialization.Serializable

@Serializable
enum class ContentType {
    AUDIO, IMAGE, TEXT, VIDEO
}