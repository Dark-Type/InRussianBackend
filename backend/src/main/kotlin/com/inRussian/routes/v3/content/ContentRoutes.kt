package com.inRussian.routes.v3.content

import com.inRussian.responses.common.ErrorResponse
import com.inRussian.services.v3.ContentService
import com.inRussian.utils.validation.FieldError
import com.inRussian.utils.validation.ValidationException
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import java.util.UUID

@Resource("/content/themes/{themeId}")
class ThemeResource(val themeId: String)

@Resource("/content/themes/by-course/{courseId}")
class ThemesByCourseResource(val courseId: String)

@Resource("/content/themes/by-theme/{themeId}")
class ThemesByThemeResource(val themeId: String)

@Resource("/content/themes/{themeId}/tasks")
class ThemeTasksResource(val themeId: String)

@Resource("/content/themes/{themeId}/contents")
class ThemeContentsResource(val themeId: String)

@Resource("/content/themes/{themeId}/tree")
class ThemeTreeResource(val themeId: String)

@Resource("/content/courses")
class CoursesResource

@Resource("/content/courses/{courseId}")
class CourseResource(val courseId: String)

@Resource("/content/courses/{courseId}/theme-tree")
class CourseThemeTreeResource(val courseId: String)

@Resource("/content/reports")
class ReportsResource

@Resource("/content/reports/{reportId}")
class ReportResource(val reportId: String)

@Resource("/content/stats")
class StatsResource

@Resource("/content/stats/theme/{themeId}/tasks-count")
class ThemeTasksCountResource(val themeId: String)

@Resource("/content/stats/course/{courseId}/tasks-count")
class CourseTasksCountResource(val courseId: String)

fun Route.contentRoutes(contentService: ContentService) {
    authenticate("auth-jwt") {

        get<ThemeResource> { resource ->
            val themeId = validateUuidParam(resource.themeId, "themeId")
            call.respondResult(contentService.getTheme(themeId))
        }

        get<ThemesByCourseResource> { resource ->
            val courseId = validateUuidParam(resource.courseId, "courseId")
            call.respondResult(contentService.getThemesByCourse(courseId))
        }

        get<ThemesByThemeResource> { resource ->
            val themeId = validateUuidParam(resource.themeId, "themeId")
            call.respondResult(contentService.getThemesByTheme(themeId))
        }

        get<ThemeTasksResource> { resource ->
            val themeId = validateUuidParam(resource.themeId, "themeId")
            call.respondResult(contentService.getTasksByTheme(themeId))
        }

        get<ThemeContentsResource> { resource ->
            val themeId = validateUuidParam(resource.themeId, "themeId")
            call.respondResult(contentService.getThemeContents(themeId))
        }

        get<ThemeTreeResource> { resource ->
            val themeId = validateUuidParam(resource.themeId, "themeId")
            call.respondResult(contentService.getThemeSubtree(themeId))
        }

        get<CoursesResource> {
            call.respondResult(contentService.getAllCourses())
        }

        get<CourseResource> { resource ->
            val courseId = validateUuidParam(resource.courseId, "courseId")
            call.respondResult(contentService.getCourse(courseId))
        }

        get<CourseThemeTreeResource> { resource ->
            val courseId = validateUuidParam(resource.courseId, "courseId")
            call.respondResult(contentService.getCourseThemeTree(courseId))
        }

        get<ReportsResource> {
            call.respondResult(contentService.getAllReports())
        }

        get<ReportResource> { resource ->
            val reportId = validateUuidParam(resource.reportId, "reportId")
            call.respondResult(contentService.getReport(reportId))
        }

        get<StatsResource> {
            call.respondResult(contentService.getCountStats())
        }

        get<ThemeTasksCountResource> { resource ->
            val themeId = validateUuidParam(resource.themeId, "themeId")
            val result = contentService.getThemeTasksCount(themeId).map { mapOf("count" to it) }
            call.respondResult(result)
        }

        get<CourseTasksCountResource> { resource ->
            val courseId = validateUuidParam(resource.courseId, "courseId")
            val result = contentService.getCourseTasksCount(courseId).map { mapOf("count" to it) }
            call.respondResult(result)
        }
    }
}

private fun validateUuidParam(raw: String?, field: String): String {
    if (raw.isNullOrBlank()) {
        throw ValidationException(
            listOf(FieldError(field = field, code = "REQUIRED", message = "Параметр $field обязателен"))
        )
    }
    runCatching { UUID.fromString(raw) }.getOrElse {
        throw ValidationException(
            listOf(
                FieldError(
                    field = field,
                    code = "INVALID_UUID",
                    message = "Параметр $field имеет неверный формат UUID"
                )
            )
        )
    }
    return raw
}

private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String, code: String) {
    respond(
        status,
        ErrorResponse(
            success = false,
            error = message,
            code = code,
            timestamp = System.currentTimeMillis()
        )
    )
}

private suspend fun <T> ApplicationCall.respondResult(
    result: Result<T>,
    successStatus: HttpStatusCode = HttpStatusCode.OK
) {
    result.fold(
        onSuccess = { value ->
            if (value == null) respond(HttpStatusCode.NoContent) else respond(successStatus, value as Any)
        },
        onFailure = { ex ->
            when (ex) {
                is ValidationException -> respondError(
                    HttpStatusCode.BadRequest,
                    ex.message ?: "Validation error",
                    "VALIDATION_ERROR"
                )

                is NoSuchElementException -> respondError(
                    HttpStatusCode.NotFound,
                    ex.message ?: "Not found",
                    "NOT_FOUND"
                )

                else -> respondError(HttpStatusCode.InternalServerError, "Unexpected error", "INTERNAL_ERROR")
            }
        }
    )
}
