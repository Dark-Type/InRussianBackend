package com.inRussian.models.media

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
    val isActive: Boolean = true,
    val fileBlob: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaFile) return false

        return id == other.id &&
                fileName == other.fileName &&
                fileType == other.fileType &&
                mimeType == other.mimeType &&
                fileSize == other.fileSize &&
                uploadedBy == other.uploadedBy &&
                uploadedAt == other.uploadedAt &&
                isActive == other.isActive &&
                fileBlob.contentEquals(other.fileBlob)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + fileType.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + (uploadedBy?.hashCode() ?: 0)
        result = 31 * result + uploadedAt.hashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + fileBlob.contentHashCode()
        return result
    }
}
