package com.inRussian.services

import com.inRussian.models.media.FileType
import com.inRussian.models.media.MediaFileMeta
import com.inRussian.models.users.UserRole
import com.inRussian.repositories.MediaRepository
import java.io.File
import java.util.UUID

class MediaService(private val mediaRepository: MediaRepository) {

    suspend fun saveMediaFile(
        fileName: String,
        mimeType: String,
        fileType: FileType,
        fileBytes: ByteArray,
        userId: UUID,
        userRole: UserRole = UserRole.STUDENT
    ): Result<MediaFileMeta> {
        return when (fileType) {
            FileType.AVATAR -> {
                val allowedMimeTypes = listOf("image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp")
                if (mimeType !in allowedMimeTypes) {
                    return Result.failure(Exception("Разрешены только изображения (png, jpeg, jpg, gif, webp)"))
                }
                if (fileBytes.size > 10 * 1024 * 1024) {
                    return Result.failure(Exception("Размер изображения не должен превышать 10 МБ"))
                }

                val oldAvatar = mediaRepository.findUserAvatar(userId)
                oldAvatar?.let {
                    mediaRepository.deleteMedia(UUID.fromString(it.mediaId))
                }

                Result.success(
                    mediaRepository.saveMedia(fileName, mimeType, FileType.AVATAR, userId, fileBytes)
                )
            }

            else -> {
                if (userRole != UserRole.CONTENT_MODERATOR && userRole != UserRole.ADMIN) {
                    return Result.failure(Exception("Нет прав для загрузки контента"))
                }
                Result.success(
                    mediaRepository.saveMedia(fileName, mimeType, fileType, userId, fileBytes)
                )
            }
        }
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
        val meta = mediaRepository.getMeta(mediaId)
            ?: return Result.failure(Exception("Файл не найден"))

        if (fileType == FileType.AVATAR) {
            if (meta.uploadedBy != userId.toString()) {
                return Result.failure(Exception("Можно обновлять только свой аватар"))
            }

            if (fileBytes.size > 10 * 1024 * 1024) {
                return Result.failure(Exception("Размер изображения не должен превышать 10 МБ"))
            }

            val allowedMimeTypes = listOf("image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp")
            if (mimeType !in allowedMimeTypes) {
                return Result.failure(Exception("Разрешены только изображения (png, jpeg, jpg, gif, webp)"))
            }
        } else {
            if (userRole != UserRole.CONTENT_MODERATOR && userRole != UserRole.ADMIN) {
                return Result.failure(Exception("Нет прав для обновления контента"))
            }
        }

        val updatedMeta = mediaRepository.updateMedia(mediaId, fileName, mimeType, fileType, fileBytes)
        return Result.success(updatedMeta)
    }

    suspend fun deleteMediaFile(
        mediaId: UUID,
        userId: UUID,
        userRole: UserRole = UserRole.STUDENT
    ): Boolean {
        val meta = mediaRepository.getMeta(mediaId) ?: return false

        return when (meta.fileType) {
            FileType.AVATAR -> {
                if (meta.uploadedBy == userId.toString()) {
                    mediaRepository.deleteMedia(mediaId)
                } else {
                    false
                }
            }
            else -> {
                if (userRole == UserRole.CONTENT_MODERATOR || userRole == UserRole.ADMIN) {
                    mediaRepository.deleteMedia(mediaId)
                } else {
                    false
                }
            }
        }
    }

    suspend fun getMediaFile(mediaId: UUID): Pair<MediaFileMeta, File>? {
        return mediaRepository.getMedia(mediaId)
    }

    suspend fun getMediaMeta(mediaId: UUID): MediaFileMeta? {
        return mediaRepository.getMeta(mediaId)
    }

    suspend fun getUserAvatar(userId: UUID): MediaFileMeta? {
        return mediaRepository.findUserAvatar(userId)
    }
}