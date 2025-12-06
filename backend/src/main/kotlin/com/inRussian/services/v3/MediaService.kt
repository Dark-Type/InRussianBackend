package com.inRussian.services.v3

import com.inRussian.models.media.FileType
import com.inRussian.models.media.MediaFileMeta
import com.inRussian.models.users.UserRole
import com.inRussian.repositories.MediaRepository
import com.inRussian.utils.validation.FieldError
import com.inRussian.utils.validation.ValidationException
import java.nio.file.Path
import java.util.UUID

class MediaService(private val mediaRepository: MediaRepository) {

    companion object {
        private const val MAX_AVATAR_SIZE = 10 * 1024 * 1024L // 10 MB
        private const val MAX_CONTENT_SIZE = 100 * 1024 * 1024L // 100 MB
        private val ALLOWED_AVATAR_TYPES = listOf("image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp")
        private val ALLOWED_CONTENT_TYPES = listOf(
            "image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp",
            "video/mp4", "video/avi", "audio/mp3", "audio/wav", "application/pdf"
        )
    }

    suspend fun saveMediaFile(
        fileName: String,
        mimeType: String,
        fileType: FileType,
        fileBytes: ByteArray,
        userId: UUID,
        userRole: UserRole = UserRole.STUDENT
    ): Result<MediaFileMeta> {
        val errors = mutableListOf<FieldError>()

        validateFileName(fileName, errors)

        if (mediaRepository.existsByFileName(userId, fileName, fileType)) {
            errors.add(FieldError("fileName", "duplicate_name", "file_name_already_exists"))
        }

        when (fileType) {
            FileType.AVATAR -> {
                validateMimeType(mimeType, ALLOWED_AVATAR_TYPES, "avatar", errors)
                validateFileSize(fileBytes.size.toLong(), MAX_AVATAR_SIZE, "avatar", errors)
            }

            else -> {
                if (userRole != UserRole.CONTENT_MODERATOR && userRole != UserRole.ADMIN) {
                    errors.add(FieldError("permission", "no_permission", "no_permission_to_upload_content"))
                }
                validateMimeType(mimeType, ALLOWED_CONTENT_TYPES, "content", errors)
                validateFileSize(fileBytes.size.toLong(), MAX_CONTENT_SIZE, "content", errors)
            }
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)

        if (fileType == FileType.AVATAR) {
            mediaRepository.findUserAvatar(userId)?.let {
                mediaRepository.deleteMedia(UUID.fromString(it.mediaId))
            }
        }

        return Result.success(
            mediaRepository.saveMedia(fileName, mimeType, fileType, userId, fileBytes)
        )
    }

    suspend fun updateMediaFile(
        mediaId: UUID,
        fileName: String,
        mimeType: String,
        fileType: FileType,
        fileBytes: ByteArray,
        userId: UUID,
        userRole: UserRole = UserRole.STUDENT
    ): Result<MediaFileMeta> {
        val errors = mutableListOf<FieldError>()

        val meta = mediaRepository.getMeta(mediaId)
            ?: return Result.failure(Exception("file_not_found"))

        validateFileName(fileName, errors)

        // если имя меняется — проверить дубликат
        if (fileName != meta.fileName && mediaRepository.existsByFileName(
                UUID.fromString(meta.uploadedBy ?: ""),
                fileName,
                fileType
            )
        ) {
            errors.add(FieldError("fileName", "duplicate_name", "file_name_already_exists"))
        }

        if (fileType == FileType.AVATAR) {
            if (meta.uploadedBy != userId.toString()) {
                errors.add(FieldError("permission", "no_permission", "can_only_update_own_avatar"))
            }
            validateMimeType(mimeType, ALLOWED_AVATAR_TYPES, "avatar", errors)
            validateFileSize(fileBytes.size.toLong(), MAX_AVATAR_SIZE, "avatar", errors)
        } else {
            if (userRole != UserRole.CONTENT_MODERATOR && userRole != UserRole.ADMIN) {
                errors.add(FieldError("permission", "no_permission", "no_permission_to_update_content"))
            }
            validateMimeType(mimeType, ALLOWED_CONTENT_TYPES, "content", errors)
            validateFileSize(fileBytes.size.toLong(), MAX_CONTENT_SIZE, "content", errors)
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)

        return Result.success(mediaRepository.updateMedia(mediaId, fileName, mimeType, fileType, fileBytes))
    }

    suspend fun deleteMediaFile(mediaId: UUID, userId: UUID, userRole: UserRole = UserRole.STUDENT): Boolean {
        val meta = mediaRepository.getMeta(mediaId) ?: return false

        return when (meta.fileType) {
            FileType.AVATAR -> {
                if (meta.uploadedBy == userId.toString()) mediaRepository.deleteMedia(mediaId) else false
            }

            else -> {
                if (userRole == UserRole.CONTENT_MODERATOR || userRole == UserRole.ADMIN) {
                    mediaRepository.deleteMedia(mediaId)
                } else false
            }
        }
    }

    suspend fun getMediaFile(mediaId: UUID): Pair<MediaFileMeta, Path>? {
        return mediaRepository.getMedia(mediaId)
    }

    suspend fun getMediaMeta(mediaId: UUID): MediaFileMeta? = mediaRepository.getMeta(mediaId)

    suspend fun getUserAvatar(userId: UUID): MediaFileMeta? = mediaRepository.findUserAvatar(userId)

    private fun validateFileName(name: String, errors: MutableList<FieldError>) {
        if (name.isBlank()) errors.add(FieldError("fileName", "required", "file_name_required"))
        if (name.length > 255) errors.add(FieldError("fileName", "too_long", "file_name_too_long"))
    }

    private fun validateMimeType(mime: String, allowed: List<String>, field: String, errors: MutableList<FieldError>) {
        if (mime !in allowed) errors.add(FieldError(field, "invalid_mime", "invalid_mime_type"))
    }

    private fun validateFileSize(size: Long, max: Long, field: String, errors: MutableList<FieldError>) {
        if (size > max) errors.add(FieldError(field, "too_large", "file_too_large"))
    }
}