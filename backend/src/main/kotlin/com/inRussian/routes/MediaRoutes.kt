package com.inRussian.routes

import com.inRussian.config.getUserId
import com.inRussian.config.getUserRole
import com.inRussian.models.media.FileType
import com.inRussian.services.MediaService
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import com.inRussian.models.users.UserRole
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import java.util.UUID

fun Route.mediaRoutes(mediaService: MediaService) {

    route("/media") {
        authenticate("auth-jwt") {
            post("/upload") {
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
            put("/{mediaId}") {
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

            delete("/{mediaId}") {
                val principal = call.principal<JWTPrincipal>()!!
                val tokenUserId = principal.getUserId()?.let(UUID::fromString)
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                val userRole = principal.getUserRole()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                val paramUserId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val targetUserId = if (userRole == UserRole.ADMIN && paramUserId != null) paramUserId else tokenUserId

                val mediaId = call.parameters["mediaId"]?.let(UUID::fromString)
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "mediaId required")

                val deleted = mediaService.deleteMediaFile(mediaId, targetUserId, userRole)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, "Deleted")
                } else {
                    call.respond(HttpStatusCode.NotFound, "File not found or access denied")
                }
            }
        }
        get("/{mediaId}") {
            val mediaId = call.parameters["mediaId"]?.let(UUID::fromString)
                ?: return@get call.respond(HttpStatusCode.BadRequest, "mediaId required")

            val pair = mediaService.getMediaFile(mediaId)
            if (pair != null) {
                val (meta, file) = pair
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound, "File not found or access denied")
            }
        }
    }
}