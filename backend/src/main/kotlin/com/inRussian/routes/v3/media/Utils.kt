package com.inRussian.routes.v3.media

import com.inRussian.models.media.FileType
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.RoutingCall

import java.util.UUID


fun String.toUuidOrNull(): UUID? = try {
    UUID.fromString(this)
} catch (_: Exception) {
    null
}

data class MediaUploadInput(
    val fileName: String?,
    val mimeType: String?,
    val fileType: FileType?,
    val fileBytes: ByteArray?
)

suspend fun parseMediaMultipart(call: RoutingCall): MediaUploadInput {
    var fileName: String? = null
    var mimeType: String? = null
    var fileType: FileType? = null
    var fileBytes: ByteArray? = null

    val multipart = call.receiveMultipart()
    multipart.forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                when (part.name) {
                    "fileType" -> fileType = try {
                        FileType.valueOf(part.value.uppercase())
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            is PartData.FileItem -> {
                fileName = part.originalFileName
                mimeType = part.contentType?.toString()
                fileBytes = part.streamProvider().readBytes()
            }

            else -> {}
        }
        part.dispose()
    }

    return MediaUploadInput(fileName, mimeType, fileType, fileBytes)
}

fun validateMediaInput(input: MediaUploadInput): List<Map<String, String>> {
    val errors = mutableListOf<Map<String, String>>()

    if (input.fileName.isNullOrBlank()) {
        errors.add(mapOf("field" to "file", "code" to "REQUIRED", "message" to "File is required"))
    }
    if (input.mimeType.isNullOrBlank()) {
        errors.add(mapOf("field" to "mimeType", "code" to "REQUIRED", "message" to "MIME type is required"))
    }
    if (input.fileType == null) {
        errors.add(
            mapOf(
                "field" to "fileType",
                "code" to "REQUIRED",
                "message" to "File type is required (AVATAR or CONTENT)"
            )
        )
    }
    if (input.fileBytes == null || input.fileBytes.isEmpty()) {
        errors.add(mapOf("field" to "file", "code" to "EMPTY", "message" to "File content is empty"))
    }

    return errors
}