package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.models.media.FileType
import com.inRussian.models.media.MediaFileMeta
import com.inRussian.tables.MediaFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

interface MediaRepository {
    suspend fun saveMedia(
        originalFileName: String,
        mimeType: String,
        fileType: FileType,
        uploadedBy: UUID?,
        fileBytes: ByteArray
    ): MediaFileMeta

    suspend fun getMedia(mediaId: UUID): Pair<MediaFileMeta, Path>?

    suspend fun findUserAvatar(userId: UUID): MediaFileMeta?

    suspend fun deleteMedia(mediaId: UUID): Boolean

    suspend fun updateMedia(
        mediaId: UUID,
        originalFileName: String,
        mimeType: String,
        fileType: FileType,
        fileBytes: ByteArray
    ): MediaFileMeta

    suspend fun getMeta(mediaId: UUID): MediaFileMeta?

    // новый метод для проверки дубликатов имени
    suspend fun existsByFileName(uploadedBy: UUID?, fileName: String, fileType: FileType): Boolean
}

class ExposedMediaRepository(
    baseDir: Path = Path.of("media")
) : MediaRepository {
    private val avatarsDir = baseDir.resolve("Avatars")
    private val contentDir = baseDir.resolve("Content")

    private val mimeToExt: Map<String, String> = mapOf(
        "image/jpeg" to "jpg",
        "image/jpg" to "jpg",
        "image/png" to "png",
        "image/gif" to "gif",
        "image/webp" to "webp",
        "video/mp4" to "mp4",
        "video/avi" to "avi",
        "audio/mp3" to "mp3",
        "audio/wav" to "wav",
        "application/pdf" to "pdf"
    )

    init {
        Files.createDirectories(avatarsDir)
        Files.createDirectories(contentDir)
    }

    override suspend fun saveMedia(
        originalFileName: String,
        mimeType: String,
        fileType: FileType,
        uploadedBy: UUID?,
        fileBytes: ByteArray
    ): MediaFileMeta {
        val mediaId = UUID.randomUUID()
        val ext = pickExtension(originalFileName, mimeType)
        val dir = targetDir(fileType)
        val target = dir.resolve("$mediaId.$ext")

        writeFileAtomically(target, fileBytes)

        return try {
            val meta = dbQuery {
                MediaFiles.insert {
                    it[id] = mediaId
                    it[MediaFiles.fileName] = originalFileName
                    it[MediaFiles.mimeType] = mimeType
                    it[MediaFiles.fileType] = fileType.name
                    it[MediaFiles.fileSize] = fileBytes.size.toLong()
                    it[MediaFiles.uploadedBy] = uploadedBy
                    it[MediaFiles.isActive] = true
                }.resultedValues!!.single().toMeta()
            }
            meta
        } catch (ex: Exception) {
            withContext(Dispatchers.IO) { Files.deleteIfExists(target) }
            throw ex
        }
    }

    override suspend fun getMedia(mediaId: UUID): Pair<MediaFileMeta, Path>? {
        val meta = getMeta(mediaId) ?: return null
        val dir = targetDir(meta.fileType)
        val file = findFileByMediaId(dir, mediaId) ?: return null
        if (!Files.exists(file)) return null
        return meta to file
    }

    override suspend fun findUserAvatar(userId: UUID): MediaFileMeta? = dbQuery {
        MediaFiles.selectAll()
            .where {
                (MediaFiles.fileType eq FileType.AVATAR.name) and
                        (MediaFiles.uploadedBy eq userId) and
                        (MediaFiles.isActive eq true)
            }
            .singleOrNull()
            ?.toMeta()
    }

    override suspend fun deleteMedia(mediaId: UUID): Boolean {
        val updated = dbQuery {
            MediaFiles.update({ MediaFiles.id eq mediaId }) {
                it[isActive] = false
            }
        }
        if (updated == 0) return false

        val meta = getMeta(mediaId)
        val dir = meta?.let { targetDir(it.fileType) } ?: avatarsDir
        val file = findFileByMediaId(dir, mediaId)
        withContext(Dispatchers.IO) {
            file?.let { Files.deleteIfExists(it) }
        }
        return true
    }

    override suspend fun updateMedia(
        mediaId: UUID,
        originalFileName: String,
        mimeType: String,
        fileType: FileType,
        fileBytes: ByteArray
    ): MediaFileMeta {
        val existing = getMeta(mediaId) ?: throw IllegalArgumentException("Media not found")
        val oldDir = targetDir(existing.fileType)
        val oldFile = findFileByMediaId(oldDir, mediaId)

        val ext = pickExtension(originalFileName, mimeType)
        val newDir = targetDir(fileType)
        val newFile = newDir.resolve("$mediaId.$ext")

        writeFileAtomically(newFile, fileBytes)

        return try {
            dbQuery {
                MediaFiles.update({ MediaFiles.id eq mediaId }) {
                    it[MediaFiles.fileName] = originalFileName
                    it[MediaFiles.mimeType] = mimeType
                    it[MediaFiles.fileType] = fileType.name
                    it[MediaFiles.fileSize] = fileBytes.size.toLong()
                }
            }
            withContext(Dispatchers.IO) { oldFile?.let { Files.deleteIfExists(it) } }
            getMeta(mediaId) ?: error("Updated media meta missing")
        } catch (ex: Exception) {
            withContext(Dispatchers.IO) { Files.deleteIfExists(newFile) }
            throw ex
        }
    }

    override suspend fun getMeta(mediaId: UUID): MediaFileMeta? = dbQuery {
        MediaFiles.selectAll()
            .where { (MediaFiles.id eq mediaId) and (MediaFiles.isActive eq true) }
            .singleOrNull()
            ?.toMeta()
    }

    override suspend fun existsByFileName(uploadedBy: UUID?, fileName: String, fileType: FileType): Boolean = dbQuery {
        val uploaderCondition = if (uploadedBy != null) {
            MediaFiles.uploadedBy eq uploadedBy
        } else {
            MediaFiles.uploadedBy.isNull()
        }

        MediaFiles.selectAll()
            .where {
                (MediaFiles.fileName eq fileName) and
                        (MediaFiles.fileType eq fileType.name) and
                        (MediaFiles.isActive eq true) and
                        uploaderCondition
            }
            .count() > 0
    }

    private fun pickExtension(originalName: String, mime: String): String {
        val extFromName = originalName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        if (extFromName.isNotBlank()) return extFromName
        return mimeToExt[mime.lowercase()] ?: "bin"
    }

    private fun targetDir(fileType: FileType): Path =
        if (fileType == FileType.AVATAR) avatarsDir else contentDir

    private fun findFileByMediaId(dir: Path, mediaId: UUID): Path? =
        Files.list(dir).use { stream ->
            stream.filter { p -> p.fileName.toString().startsWith("$mediaId.") }
                .findFirst()
                .orElse(null)
        }

    private suspend fun writeFileAtomically(target: Path, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            Files.createDirectories(target.parent)
            val tmp = Files.createTempFile(target.parent, "upload-", ".tmp")
            Files.write(tmp, bytes)
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
    }

    private fun ResultRow.toMeta() = MediaFileMeta(
        mediaId = this[MediaFiles.id].toString(),
        fileName = this[MediaFiles.fileName],
        mimeType = this[MediaFiles.mimeType],
        fileSize = this[MediaFiles.fileSize],
        fileType = FileType.valueOf(this[MediaFiles.fileType]),
        uploadedBy = this[MediaFiles.uploadedBy]?.toString(),
        uploadedAt = this[MediaFiles.uploadedAt].toString()
    )
}