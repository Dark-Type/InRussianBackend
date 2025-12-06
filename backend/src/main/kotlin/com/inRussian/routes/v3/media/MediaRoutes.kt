package com.inRussian.routes.v3.media

import com.inRussian.config.getUserId
import com.inRussian.config.getUserRole
import com.inRussian.models.media.FileType
import com.inRussian.models.users.UserRole
import com.inRussian.services.v3.MediaService
import io.ktor.http.*
import io.ktor.server.routing.put
import io.ktor.server.routing.post
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.utils.io.readRemaining
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.put
import io.ktor.server.resources.post
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import kotlinx.io.readByteArray
import java.util.UUID
import kotlin.io.path.exists

fun Route.mediaRoutes(mediaService: MediaService) {
    authenticate("auth-jwt") {

        post("media/upload") {
            val principal = call.principal<JWTPrincipal>()!!
            val tokenUserId = principal.getUserId()?.let(UUID::fromString)
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")
            val userRole = principal.getUserRole()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")

            val paramUserId = call.request.queryParameters["userId"]?.let(UUID::fromString)
            val targetUserId =
                if (userRole == UserRole.ADMIN && paramUserId != null) paramUserId else tokenUserId

            val multipart = call.receiveMultipart()
            var fileName: String? = null
            var mimeType: String? = null
            var fileType: FileType? = null
            var fileBytes: ByteArray? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "fileName" -> fileName = part.value
                            "mimeType" -> mimeType = part.value
                            "fileType" -> fileType = FileType.valueOf(part.value)
                        }
                    }

                    is PartData.FileItem -> {
                        fileBytes = part.provider().readRemaining().readByteArray()
                    }

                    else -> {}
                }
            }

            if (fileName == null) return@post call.respond(HttpStatusCode.BadRequest, "fileName required")
            if (mimeType == null) return@post call.respond(HttpStatusCode.BadRequest, "mimeType required")
            if (fileType == null) return@post call.respond(HttpStatusCode.BadRequest, "fileType required")
            if (fileBytes == null) return@post call.respond(HttpStatusCode.BadRequest, "file required")

            val result = mediaService.saveMediaFile(
                fileName,
                mimeType,
                fileType,
                fileBytes,
                targetUserId,
                userRole
            )
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.Created, it) },
                onFailure = { call.respond(HttpStatusCode.BadRequest, it.message ?: "Upload failed") }
            )
        }
        put("media/{mediaId}") {
            val principal = call.principal<JWTPrincipal>()!!
            val tokenUserId = principal.getUserId()?.let(UUID::fromString)
                ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
            val userRole = principal.getUserRole()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid token")
            val paramUserId = call.request.queryParameters["userId"]?.let(UUID::fromString)
            val targetUserId = if (userRole == UserRole.ADMIN && paramUserId != null) paramUserId else tokenUserId

            val mediaId = call.parameters["mediaId"]?.let(UUID::fromString)
                ?: return@put call.respond(HttpStatusCode.BadRequest, "mediaId required")

            val multipart = call.receiveMultipart()
            var fileName: String? = null
            var mimeType: String? = null
            var fileType: FileType? = null
            var fileBytes: ByteArray? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "fileName" -> fileName = part.value
                            "mimeType" -> mimeType = part.value
                            "fileType" -> fileType = FileType.valueOf(part.value)
                        }
                    }

                    is PartData.FileItem -> {
                        fileBytes = part.provider().readRemaining().readByteArray()
                    }

                    else -> {}
                }
            }

            if (fileName == null) return@put call.respond(HttpStatusCode.BadRequest, "fileName required")
            if (mimeType == null) return@put call.respond(HttpStatusCode.BadRequest, "mimeType required")
            if (fileType == null) return@put call.respond(HttpStatusCode.BadRequest, "fileType required")
            if (fileBytes == null) return@put call.respond(HttpStatusCode.BadRequest, "file required")

            val result = mediaService.updateMediaFile(
                mediaId,
                fileName,
                mimeType,
                fileType,
                fileBytes,
                targetUserId,
                userRole
            )
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.OK, it) },
                onFailure = { call.respond(HttpStatusCode.BadRequest, it.message ?: "Update failed") }
            )
        }

        delete<MediaResource.ById> { resource ->
            val principal = call.principal<JWTPrincipal>()!!
            val tokenUserId = principal.getUserId()?.let { UUID.fromString(it) }
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid_token"))
            val userRole = principal.getUserRole()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid_token"))

            val paramUserId = resource.userId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (resource.userId != null && paramUserId == null) {
                return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_userId"))
            }
            if (paramUserId != null && userRole != UserRole.ADMIN) {
                return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "forbidden"))
            }
            val targetUserId = paramUserId ?: tokenUserId

            val mediaId = runCatching { UUID.fromString(resource.mediaId) }.getOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_mediaId"))

            val deleted = mediaService.deleteMediaFile(mediaId, targetUserId, userRole)
            if (deleted) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "deleted"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "not_found_or_access_denied"))
            }
        }
    }

    // GET /media/{mediaId} (публичный просмотр)
    get<MediaResource.ById> { resource ->
        val mediaId = runCatching { UUID.fromString(resource.mediaId) }.getOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_mediaId"))

        val pair = mediaService.getMediaFile(mediaId)
        if (pair != null) {
            val (meta, path) = pair
            if (path.exists()) {
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    "inline; filename=\"${meta.fileName}\""
                )
                call.respondFile(path.toFile())
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "file_not_found"))
            }
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "not_found"))
        }
    }
}
