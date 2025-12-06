package com.inRussian.routes.v3.expert

import com.inRussian.responses.common.ErrorResponse
import com.inRussian.services.v3.ExpertService
import com.inRussian.utils.validation.FieldError
import com.inRussian.utils.validation.ValidationException
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID

@Resource("/expert/students")
class ExpertStudentsResource(
    val page: String? = null,
    val size: String? = null,
    val sortBy: String? = null,
    val sortOrder: String? = null,
    val createdFrom: String? = null,
    val createdTo: String? = null
)

@Resource("/expert/students/count")
class ExpertStudentsCountResource(
    val createdFrom: String? = null,
    val createdTo: String? = null
)

@Resource("/expert/statistics/students/overall")
class ExpertStatisticsStudentsOverallResource

@Resource("/expert/statistics/course/{courseId}/average-time")
class ExpertStatisticsCourseAverageTimeResource(val courseId: String)

@Resource("/expert/statistics/course/{courseId}/students-count")
class ExpertStatisticsCourseStudentsCountResource(val courseId: String)

@Resource("/expert/statistics/course/{courseId}/average-progress")
class ExpertStatisticsCourseAverageProgressResource(val courseId: String)

@Resource("/expert/statistics/overall-average-time")
class ExpertStatisticsOverallAverageTimeResource

@Resource("/expert/statistics/overall-average-progress")
class ExpertStatisticsOverallAverageProgressResource

fun Route.expertRoutes(expertService: ExpertService) {
    authenticate("expert-jwt") {

        get<ExpertStudentsResource> { resource ->
            val page = parsePositiveInt(resource.page, "page", default = 1, min = 1, max = 500)
            val size = parsePositiveInt(resource.size, "size", default = 20, min = 1, max = 200)
            val sortBy = parseSortBy(resource.sortBy)
            val sortOrder = parseSortOrder(resource.sortOrder)
            val createdFrom = parseDate(resource.createdFrom, "createdFrom")
            val createdTo = parseDate(resource.createdTo, "createdTo")

            call.respondResult(
                expertService.getAllStudents(page, size, createdFrom, createdTo, sortBy, sortOrder)
            )
        }

        get<ExpertStudentsCountResource> { resource ->
            val createdFrom = parseDate(resource.createdFrom, "createdFrom")
            val createdTo = parseDate(resource.createdTo, "createdTo")

            call.respondResult(
                expertService.getStudentsCount(createdFrom, createdTo)
            ) { count -> mapOf("count" to count) }
        }

        get<ExpertStatisticsStudentsOverallResource> {
            call.respondResult(expertService.getOverallStudentsCount()) { total ->
                mapOf("totalStudents" to total)
            }
        }

        get<ExpertStatisticsCourseAverageTimeResource> { resource ->
            val courseId = validateUuidParam(resource.courseId, "courseId")
            call.respondResult(
                expertService.getCourseAverageTime(courseId)
            ) { avg -> mapOf("averageTime" to avg) }
        }

        get<ExpertStatisticsCourseStudentsCountResource> { resource ->
            val courseId = validateUuidParam(resource.courseId, "courseId")
            call.respondResult(
                expertService.getStudentsCountByCourse(courseId)
            ) { count -> mapOf("count" to count) }
        }

        get<ExpertStatisticsCourseAverageProgressResource> { resource ->
            val courseId = validateUuidParam(resource.courseId, "courseId")
            call.respondResult(
                expertService.getCourseAverageProgress(courseId)
            ) { avg -> mapOf("averageProgress" to avg) }
        }

        get<ExpertStatisticsOverallAverageTimeResource> {
            call.respondResult(expertService.getOverallAverageTime()) { avg ->
                mapOf("averageTime" to avg)
            }
        }

        get<ExpertStatisticsOverallAverageProgressResource> {
            call.respondResult(expertService.getOverallAverageProgress()) { avg ->
                mapOf("averageProgress" to avg)
            }
        }
    }
}

private fun parsePositiveInt(raw: String?, field: String, default: Int, min: Int, max: Int): Int {
    if (raw.isNullOrBlank()) return default
    val value = raw.toIntOrNull()
        ?: throw ValidationException(
            listOf(FieldError(field, "INVALID_INT", "Параметр $field должен быть числом"))
        )
    if (value !in min..max) {
        throw ValidationException(
            listOf(FieldError(field, "OUT_OF_RANGE", "Параметр $field должен быть в диапазоне $min..$max"))
        )
    }
    return value
}

private fun parseSortBy(raw: String?): String {
    if (raw.isNullOrBlank()) return "createdAt"
    return when (raw.trim().lowercase()) {
        "createdat", "created_at" -> "createdAt"
        "email" -> "email"
        "role" -> "role"
        "status" -> "status"
        "lastactivity", "lastactivityat", "last_activity_at" -> "lastActivityAt"
        else -> throw ValidationException(
            listOf(FieldError("sortBy", "INVALID_VALUE", "Недопустимое значение sortBy"))
        )
    }
}

private fun parseSortOrder(raw: String?): String {
    if (raw.isNullOrBlank()) return "desc"
    return when (raw.trim().lowercase()) {
        "asc", "ascending" -> "asc"
        "desc", "descending" -> "desc"
        else -> throw ValidationException(
            listOf(FieldError("sortOrder", "INVALID_VALUE", "Недопустимое значение sortOrder"))
        )
    }
}

private fun parseDate(raw: String?, field: String): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return try {
        LocalDate.parse(raw)
    } catch (_: DateTimeParseException) {
        throw ValidationException(
            listOf(FieldError(field, "INVALID_DATE", "Параметр $field должен быть в формате yyyy-MM-dd"))
        )
    }
}

private fun validateUuidParam(raw: String?, field: String): String {
    if (raw.isNullOrBlank()) {
        throw ValidationException(
            listOf(FieldError(field, "REQUIRED", "Параметр $field обязателен"))
        )
    }
    runCatching { UUID.fromString(raw) }.getOrElse {
        throw ValidationException(
            listOf(FieldError(field, "INVALID_UUID", "Параметр $field имеет неверный формат UUID"))
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
    successStatus: HttpStatusCode = HttpStatusCode.OK,
    transform: ((T) -> Any)? = null
) {
    result.fold(
        onSuccess = { value ->
            when {
                transform != null -> respond(successStatus, transform(value))
                value == null -> respond(HttpStatusCode.NoContent)
                else -> respond(successStatus, value as Any)
            }
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
                else -> respondError(
                    HttpStatusCode.InternalServerError,
                    "Unexpected error",
                    "INTERNAL_ERROR"
                )
            }
        }
    )
}
