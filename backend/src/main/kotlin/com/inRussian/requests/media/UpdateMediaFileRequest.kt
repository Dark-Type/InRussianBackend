package com.inRussian.requests.media

import com.inRussian.models.media.FileType
import kotlinx.serialization.Serializable

@Serializable
data class UpdateMediaFileRequest(
    val fileName: String? = null,
    val fileType: FileType? = null,
    val mimeType: String? = null,
    val fileSize: Long? = null,
    val isActive: Boolean? = null,
    val fileBlob: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UpdateMediaFileRequest) return false

        return fileName == other.fileName &&
                fileType == other.fileType &&
                mimeType == other.mimeType &&
                fileSize == other.fileSize &&
                isActive == other.isActive &&
                fileBlob.contentEquals(other.fileBlob)
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + fileType.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + (isActive?.hashCode() ?: 0)
        result = 31 * result + fileBlob.contentHashCode()
        return result
    }
}