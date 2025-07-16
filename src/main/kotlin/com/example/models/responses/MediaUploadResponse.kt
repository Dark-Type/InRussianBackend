package com.example.models.responses

import com.example.models.media.FileType
import kotlinx.serialization.Serializable

@Serializable
data class MediaUploadResponse(
    val id: String,
    val fileName: String,
    val fileType: FileType,
    val fileSize: Long,
    val uploadUrl: String? = null
)