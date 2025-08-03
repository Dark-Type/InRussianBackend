package com.inRussian.repositories

import com.inRussian.models.media.FileType
import com.inRussian.models.media.MediaFileMeta
import com.inRussian.tables.MediaFiles
import com.inRussian.tables.MediaFiles.isActive
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.io.File
import java.util.*

class MediaRepository(baseDir: String = "media") {

    private val avatarsDir = "$baseDir/Avatars"
    private val contentDir = "$baseDir/Content"

    init {
        File(avatarsDir).mkdirs()
        File(contentDir).mkdirs()
    }

    fun saveMedia(
        fileName: String,
        mimeType: String,
        fileType: FileType,
        uploadedBy: UUID?,
        fileBytes: ByteArray
    ): MediaFileMeta {
        val mediaId = UUID.randomUUID()
        val ext = File(fileName).extension.ifEmpty {
            when (mimeType) {
                "image/jpeg", "image/jpg" -> "jpg"
                "image/png" -> "png"
                "image/gif" -> "gif"
                "image/webp" -> "webp"
                "video/mp4" -> "mp4"
                "video/avi" -> "avi"
                "audio/mp3" -> "mp3"
                "audio/wav" -> "wav"
                "application/pdf" -> "pdf"
                else -> "bin"
            }
        }
        val safeFileName = "$mediaId.$ext"
        val dir = if (fileType == FileType.AVATAR) avatarsDir else contentDir
        val filePath = "$dir/$safeFileName"
        File(filePath).writeBytes(fileBytes)

        val uploadedAt = transaction {
            MediaFiles.insert {
                it[id] = mediaId
                it[MediaFiles.fileName] = fileName
                it[MediaFiles.mimeType] = mimeType
                it[MediaFiles.fileType] = fileType.toString()
                it[MediaFiles.fileSize] = fileBytes.size.toLong()
                it[MediaFiles.uploadedBy] = uploadedBy
                it[MediaFiles.isActive] = true
            }.resultedValues!![0][MediaFiles.uploadedAt]
        }

        return MediaFileMeta(
            mediaId = mediaId.toString(),
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileBytes.size.toLong(),
            fileType = fileType,
            uploadedBy = uploadedBy?.toString(),
            uploadedAt = uploadedAt.toString()
        )
    }

    fun getMedia(mediaId: UUID): Pair<MediaFileMeta, File>? {
        val meta = transaction {
            MediaFiles.selectAll().where {
                (MediaFiles.id eq mediaId) and (isActive eq true)
            }.singleOrNull()?.let {
                MediaFileMeta(
                    mediaId = it[MediaFiles.id].toString(),
                    fileName = it[MediaFiles.fileName],
                    mimeType = it[MediaFiles.mimeType],
                    fileSize = it[MediaFiles.fileSize],
                    fileType = FileType.valueOf(it[MediaFiles.fileType]),
                    uploadedBy = it[MediaFiles.uploadedBy]?.toString(),
                    uploadedAt = it[MediaFiles.uploadedAt].toString()
                )
            }
        } ?: return null

        val dir = if (meta.fileType == FileType.AVATAR) avatarsDir else contentDir
        val file = findFileByMediaId(dir, mediaId)
        return if (file?.exists() == true) meta to file else null
    }

    private fun findFileByMediaId(dir: String, mediaId: UUID): File? {
        val directory = File(dir)
        if (!directory.exists()) return null

        return directory.listFiles()?.find { file ->
            file.name.matches(Regex("^$mediaId\\.[a-zA-Z0-9]+$"))
        }
    }

    fun findUserAvatar(userId: UUID): MediaFileMeta? = transaction {
        MediaFiles.selectAll().where {
            (MediaFiles.fileType eq FileType.AVATAR.toString()) and
                    (MediaFiles.uploadedBy eq userId) and
                    (isActive eq true)
        }.singleOrNull()?.let {
            MediaFileMeta(
                mediaId = it[MediaFiles.id].toString(),
                fileName = it[MediaFiles.fileName],
                mimeType = it[MediaFiles.mimeType],
                fileSize = it[MediaFiles.fileSize],
                fileType = FileType.valueOf(it[MediaFiles.fileType]),
                uploadedBy = it[MediaFiles.uploadedBy]?.toString(),
                uploadedAt = it[MediaFiles.uploadedAt].toString()
            )
        }
    }

    fun deleteMedia(mediaId: UUID): Boolean {
        val meta = transaction {
            MediaFiles.selectAll().where {
                (MediaFiles.id eq mediaId) and (isActive eq true)
            }.singleOrNull()
        } ?: return false

        val fileType = FileType.valueOf(meta[MediaFiles.fileType])
        val dir = if (fileType == FileType.AVATAR) avatarsDir else contentDir
        val file = findFileByMediaId(dir, mediaId)
        val deleted = file?.delete() ?: false

        transaction {
            MediaFiles.update({ MediaFiles.id eq mediaId }) {
                it[isActive] = false
            }
        }
        return deleted
    }

    fun updateMedia(
        mediaId: UUID,
        fileName: String,
        mimeType: String,
        fileType: FileType,
        fileBytes: ByteArray
    ): MediaFileMeta {
        val oldMeta = getMeta(mediaId) ?: throw Exception("Файл не найден")
        val dir = if (oldMeta.fileType == FileType.AVATAR) avatarsDir else contentDir
        val oldFile = findFileByMediaId(dir, mediaId)
        oldFile?.delete()

        val ext = File(fileName).extension.ifEmpty {
            when (mimeType) {
                "image/jpeg", "image/jpg" -> "jpg"
                "image/png" -> "png"
                "image/gif" -> "gif"
                "image/webp" -> "webp"
                "video/mp4" -> "mp4"
                "video/avi" -> "avi"
                "audio/mp3" -> "mp3"
                "audio/wav" -> "wav"
                "application/pdf" -> "pdf"
                else -> "bin"
            }
        }
        val newFile = File("$dir/$mediaId.$ext")
        newFile.writeBytes(fileBytes)

        transaction {
            MediaFiles.update({ MediaFiles.id eq mediaId }) {
                it[MediaFiles.fileName] = fileName
                it[MediaFiles.mimeType] = mimeType
                it[MediaFiles.fileType] = fileType.toString()
                it[MediaFiles.fileSize] = fileBytes.size.toLong()
            }
        }

        return getMeta(mediaId)!!
    }

    fun getMeta(mediaId: UUID): MediaFileMeta? = transaction {
        MediaFiles.selectAll().where {
            (MediaFiles.id eq mediaId) and (isActive eq true)
        }.singleOrNull()?.let {
            MediaFileMeta(
                mediaId = it[MediaFiles.id].toString(),
                fileName = it[MediaFiles.fileName],
                mimeType = it[MediaFiles.mimeType],
                fileSize = it[MediaFiles.fileSize],
                fileType = FileType.valueOf(it[MediaFiles.fileType]),
                uploadedBy = it[MediaFiles.uploadedBy]?.toString(),
                uploadedAt = it[MediaFiles.uploadedAt].toString()
            )
        }
    }
}