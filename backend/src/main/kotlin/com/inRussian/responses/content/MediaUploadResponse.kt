package com.inRussian.responses.content

import com.inRussian.models.media.FileType
import kotlinx.serialization.Serializable

@Serializable
data class MediaUploadResponse(
    val id: String,
    val fileName: String,
    val fileType: FileType,
    val fileSize: Long,
    val uploadUrl: String? = null
)