package com.inRussian.models.media

import kotlinx.serialization.Serializable

@Serializable
enum class FileType {
    IMAGE, AUDIO, VIDEO
}