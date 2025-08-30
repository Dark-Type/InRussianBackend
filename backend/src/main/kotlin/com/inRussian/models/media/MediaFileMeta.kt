package com.inRussian.models.media
import kotlinx.serialization.Serializable

@Serializable
data class MediaFileMeta(
    val mediaId: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val fileType: FileType,
    val uploadedBy: String? = null,
    val uploadedAt: String
)