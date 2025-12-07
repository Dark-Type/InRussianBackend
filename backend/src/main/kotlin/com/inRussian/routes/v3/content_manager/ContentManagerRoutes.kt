package com.inRussian.routes.v3.content_manager

import com.inRussian.config.getUserId
import com.inRussian.requests.content.*
import com.inRussian.responses.common.ErrorResponse
import com.inRussian.services.v3.CloneCourseRequest
import com.inRussian.services.v3.ContentService
import com.inRussian.utils.validation.ValidationException
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import io.ktor.server.resources.post
import io.ktor.server.resources.put

@Resource("/content/themes")
class ThemesResource

@Resource("/content/themes/{themeId}")
class ThemeResource(val themeId: String)

@Resource("/content/courses")
class CoursesResource

@Resource("/content/courses/{courseId}")
class CourseResource(val courseId: String)

@Resource("/content/courses/{courseId}/export")
class CourseExportResource(val courseId: String)

@Resource("/content/courses/import")
class CourseImportResource

@Resource("/content/courses/{courseId}/clone")
class CourseCloneResource(val courseId: String)

@Resource("/content/reports")
class ReportsResource

@Resource("/content/reports/{reportId}")
class ReportResource(val reportId: String)

fun Route.contentManagerRoutes(contentService: ContentService) {
    authenticate("content-jwt") {

        post<ThemesResource> {
            val request = call.safeReceive<CreateThemeRequest>() ?: return@post
            val result = contentService.createTheme(request)
            call.respondServiceResult(result, HttpStatusCode.Created)
        }

        put<ThemeResource> { resource ->
            val request = call.safeReceive<UpdateThemeRequest>() ?: return@put
            val result = contentService.updateTheme(resource.themeId, request)
            call.respondServiceResult(result)
        }

        delete<ThemeResource> { resource ->
            val result = contentService.deleteTheme(resource.themeId)
            call.respondServiceResult(result)
        }

        post<CoursesResource> {
            val authorId = call.requireUserId() ?: return@post
            val request = call.safeReceive<CreateCourseRequest>() ?: return@post
            val result = contentService.createCourse(authorId, request)
            call.respondServiceResult(result, HttpStatusCode.Created)
        }

        put<CourseResource> { resource ->
            val request = call.safeReceive<UpdateCourseRequest>() ?: return@put
            val result = contentService.updateCourse(resource.courseId, request)
            call.respondServiceResult(result)
        }

        delete<CourseResource> { resource ->
            val result = contentService.deleteCourse(resource.courseId)
            call.respondServiceResult(result)
        }

        get<CourseExportResource> { resource ->
            val since = call.request.queryParameters["since"] // optional ISO-8601 UTC
            val result = contentService.exportCourseJson(resource.courseId, since)
            call.respondServiceResult(result) { exportJson ->
                respondText(exportJson, ContentType.Application.Json)
            }
        }

        post<CourseImportResource> {
            val importerId = call.requireUserId() ?: return@post
            val targetCourseId = call.request.queryParameters["targetCourseId"]
            val createIfMissing =
                call.request.queryParameters["createIfMissing"]?.toBooleanStrictOrNull() ?: true
            val languageOverride = call.request.queryParameters["language"]
            val addOnly = call.request.queryParameters["addOnly"]?.toBooleanStrictOrNull() ?: true
            val payload = call.receiveText()

            val result = contentService.importCourseJson(
                json = payload,
                importerId = importerId,
                targetCourseId = targetCourseId,
                createIfMissing = createIfMissing,
                languageOverride = languageOverride,
                addOnly = addOnly
            )
            call.respondServiceResult(result)
        }

        post<CourseCloneResource> { resource ->
            val authorId = call.requireUserId() ?: return@post
            val request = call.safeReceive<CloneCourseRequest>() ?: return@post
            val result = contentService.cloneCourse(resource.courseId, authorId, request)
            call.respondServiceResult(result, HttpStatusCode.Created)
        }

        post<ReportsResource> {
            val reporterId = call.requireUserId() ?: return@post
            val request = call.safeReceive<CreateReportRequest>() ?: return@post
            val result = contentService.createReport(reporterId, request)
            call.respondServiceResult(result, HttpStatusCode.Created)
        }

        get<ReportResource> { resource ->
            val result = contentService.getReport(resource.reportId)
            call.respondServiceResult(result)
        }

        delete<ReportResource> { resource ->
            val result = contentService.deleteReport(resource.reportId)
            call.respondServiceResult(result)
        }
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.safeReceive(): T? =
    runCatching { receive<T>() }.getOrElse {
        respondError(HttpStatusCode.BadRequest, "Invalid ${T::class.simpleName ?: "payload"}", "INVALID_BODY")
        null
    }

private suspend fun ApplicationCall.requireUserId(): String? {
    val userId = principal<JWTPrincipal>()?.getUserId()
    if (userId == null) {
        respondError(HttpStatusCode.Unauthorized, "Missing authenticated user", "UNAUTHORIZED")
    }
    return userId
}

private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String, code: String) {
    respond(status, ErrorResponse(success = false, error = message, code = code, timestamp = System.currentTimeMillis()))
}

@Suppress("UNCHECKED_CAST")
private suspend fun <T> ApplicationCall.respondServiceResult(
    result: Result<T>,
    successStatus: HttpStatusCode = HttpStatusCode.OK,
    writer: (suspend ApplicationCall.(T) -> Unit)? = null
) {
    result.fold(
        onSuccess = { value ->
            when {
                writer != null && value != null -> writer(value)
                writer != null && value == null -> respond(HttpStatusCode.NoContent)
                value == null -> respond(HttpStatusCode.NoContent)
                else -> respond(successStatus, value as Any)
            }
        },
        onFailure = { ex ->
            when (ex) {
                is ValidationException -> respondError(HttpStatusCode.BadRequest, ex.message ?: "Validation error", "VALIDATION_ERROR")
                is NoSuchElementException -> respondError(HttpStatusCode.NotFound, ex.message ?: "Not found", "NOT_FOUND")
                else -> respondError(HttpStatusCode.InternalServerError, "Unexpected error", "INTERNAL_ERROR")
            }
        }
    )
}
