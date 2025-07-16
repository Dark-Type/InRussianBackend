package com.example.models.media

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class MediaFile(
    val id: String,
    val fileName: String,
    val fileType: FileType,
    val mimeType: String,
    val fileSize: Long,
    val uploadedBy: String? = null,
    val uploadedAt: String = LocalDateTime.now().toString(),
    val isActive: Boolean = true
)

