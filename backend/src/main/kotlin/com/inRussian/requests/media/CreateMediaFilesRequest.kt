package com.inRussian.requests.media

import com.inRussian.models.media.FileType
import kotlinx.serialization.Serializable

@Serializable
data class CreateMediaFileRequest(
    val fileName: String,
    val fileType: FileType,
    val mimeType: String,
    val fileSize: Long,
    val uploadedBy: String? = null,
    val fileBlob: String
)