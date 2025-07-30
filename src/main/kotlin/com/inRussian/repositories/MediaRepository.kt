package com.inRussian.repositories

import com.inRussian.models.media.FileType
import com.inRussian.models.media.MediaFileMeta
import com.inRussian.tables.MediaFiles
import com.inRussian.tables.MediaFiles.isActive
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.update
import java.io.File
import java.util.*
import kotlin.and
import kotlin.text.get
import kotlin.text.set
import kotlin.toString


class MediaRepository(private val baseDir: String = "media") {

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
        val ext = File(fileName).extension
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
            }
                .resultedValues!![0][MediaFiles.uploadedAt]
        }

        return MediaFileMeta(
            mediaId = mediaId.toString(),
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileBytes.size.toLong(),
            fileType = fileType,
            uploadedBy = uploadedBy?.toString(),
            uploadedAt = uploadedAt
        )
    }

    fun getMedia(mediaId: UUID): Pair<MediaFileMeta, File>? {
        val meta = transaction {
            MediaFiles.select((MediaFiles.id eq mediaId) and (isActive eq true))
                .singleOrNull()
                ?.let {
                    MediaFileMeta(
                        mediaId = it[MediaFiles.id].toString(),
                        fileName = it[MediaFiles.fileName],
                        mimeType = it[MediaFiles.mimeType],
                        fileSize = it[MediaFiles.fileSize],
                        fileType = FileType.valueOf(it[MediaFiles.fileType]),
                        uploadedBy = it[MediaFiles.uploadedBy]?.toString(),
                        uploadedAt = it[MediaFiles.uploadedAt]
                    )
                }
        } ?: return null

        val ext = File(meta.fileName).extension
        val dir = if (meta.fileType == FileType.AVATAR) avatarsDir else contentDir
        val file = File("$dir/$mediaId.$ext")
        return if (file.exists()) meta to file else null
    }

    fun findUserAvatar(userId: UUID): MediaFileMeta? = transaction {
        MediaFiles.select(
            (MediaFiles.fileType eq FileType.AVATAR.toString()) and
                    (MediaFiles.uploadedBy eq userId) and
                    (MediaFiles.isActive eq true)
        ).singleOrNull()?.let {
            MediaFileMeta(
                mediaId = it[MediaFiles.id].toString(),
                fileName = it[MediaFiles.fileName],
                mimeType = it[MediaFiles.mimeType],
                fileSize = it[MediaFiles.fileSize],
                fileType = FileType.valueOf(it[MediaFiles.fileType]),
                uploadedBy = it[MediaFiles.uploadedBy]?.toString(),
                uploadedAt = it[MediaFiles.uploadedAt]
            )
        }
    }


    fun deleteMedia(mediaId: UUID): Boolean {
        val meta = transaction {
            MediaFiles.select((MediaFiles.id eq mediaId) and (isActive eq true))
                .singleOrNull()
        } ?: return false

        val ext = File(meta[MediaFiles.fileName]).extension
        val fileType = FileType.valueOf(meta[MediaFiles.fileType])
        val dir = if (fileType == FileType.AVATAR) avatarsDir else contentDir
        val file = File("$dir/$mediaId.$ext")
        val deleted = file.delete()

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
        val ext = File(oldMeta.fileName).extension
        val dir = if (oldMeta.fileType == FileType.AVATAR) avatarsDir else contentDir
        val oldFile = File("$dir/$mediaId.$ext")
        oldFile.delete()

        val newExt = File(fileName).extension
        val newFile = File("$dir/$mediaId.$newExt")
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
        MediaFiles.select((MediaFiles.id eq mediaId) and (isActive eq true))
            .singleOrNull()
            ?.let {
                MediaFileMeta(
                    mediaId = it[MediaFiles.id].toString(),
                    fileName = it[MediaFiles.fileName],
                    mimeType = it[MediaFiles.mimeType],
                    fileSize = it[MediaFiles.fileSize],
                    fileType = FileType.valueOf(it[MediaFiles.fileType]),
                    uploadedBy = it[MediaFiles.uploadedBy]?.toString(),
                    uploadedAt = it[MediaFiles.uploadedAt]
                )
            }
    }
}