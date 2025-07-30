package com.inRussian.models.media
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class MediaFileMeta(
    val mediaId: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val fileType: FileType,
    val uploadedBy: String? = null,
    @Contextual
    val uploadedAt: Instant
)