package com.inRussian.routes

import com.inRussian.config.getUserId
import com.inRussian.config.getUserRole
import com.inRussian.models.media.FileType
import com.inRussian.services.MediaService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.utils.io.toByteArray
import com.inRussian.models.users.UserRole
import java.util.UUID
import kotlin.text.get

fun Route.mediaRoutes(mediaService: MediaService) {
    authenticate("auth-jwt") {
        route("/media") {
            post("/upload") {
                val principal = call.principal<JWTPrincipal>()!!
                val tokenUserId = principal.getUserId()?.let(UUID::fromString)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                val userRole = principal.getUserRole()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")

                val paramUserId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val targetUserId = if (userRole == UserRole.ADMIN && paramUserId != null) paramUserId else tokenUserId

                val params = call.receiveParameters()
                val fileName =
                    params["fileName"] ?: return@post call.respond(HttpStatusCode.BadRequest, "fileName required")
                val mimeType =
                    params["mimeType"] ?: return@post call.respond(HttpStatusCode.BadRequest, "mimeType required")
                val fileType = params["fileType"]?.let { FileType.valueOf(it) }
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "fileType required")
                val fileBytes = call.receiveChannel().toByteArray()

                val result = mediaService.saveMediaFile(fileName, mimeType, fileType, fileBytes, targetUserId, userRole)
                result.fold(
                    onSuccess = { call.respond(HttpStatusCode.Created, it) },
                    onFailure = { call.respond(HttpStatusCode.BadRequest, it.message ?: "Upload failed") }
                )
            }

            get("/{mediaId}") {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "userId required")
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
                val params = call.receiveParameters()
                val fileName =
                    params["fileName"] ?: return@put call.respond(HttpStatusCode.BadRequest, "fileName required")
                val mimeType =
                    params["mimeType"] ?: return@put call.respond(HttpStatusCode.BadRequest, "mimeType required")
                val fileType = params["fileType"]?.let { FileType.valueOf(it) }
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "fileType required")
                val fileBytes = call.receiveChannel().toByteArray()


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
    }
}