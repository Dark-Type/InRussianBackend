package com.inRussian.routes.v3.admin

import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserStatus
import com.inRussian.requests.users.StaffRegisterRequest
import com.inRussian.responses.auth.MessageResponse
import com.inRussian.services.v3.AdminService
import com.inRussian.utils.validation.FieldError
import com.inRussian.utils.validation.ValidationException
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID

@Serializable
data class UpdateStatusRequest(val status: String)

fun Route.adminRoutes(adminService: AdminService) {

    // Публичный GET /admin/users/{userId}
    get<AdminResource.UserById> { resource ->
        validateUserId(resource.userId)

        val result = adminService.getUserById(resource.userId)
        result.fold(
            onSuccess = { call.respond(HttpStatusCode.OK, it) },
            onFailure = { throw it }
        )
    }

    authenticate("admin-jwt", "content-jwt") {

        // PUT /admin/users/{userId}/status — регистрируем ДО get<UserById>
        put<AdminResource.UserStatus> { resource ->
            validateUserId(resource.userId)

            val statusRequest = runCatching { call.receive<UpdateStatusRequest>() }.getOrElse {
                throw ValidationException(listOf(FieldError("body", "invalid_json", "Invalid request body")))
            }

            if (statusRequest.status.isBlank()) {
                throw ValidationException(listOf(FieldError("status", "required", "Status is required")))
            }

            val status = runCatching { UserStatus.valueOf(statusRequest.status.uppercase()) }.getOrElse {
                throw ValidationException(
                    listOf(
                        FieldError(
                            "status",
                            "invalid_value",
                            "Invalid status. Allowed: ${UserStatus.entries.joinToString { it.name }}"
                        )
                    )
                )
            }

            val result = adminService.updateUserStatus(resource.userId, status)
            result.fold(
                onSuccess = {
                    call.respond(
                        HttpStatusCode.OK, MessageResponse(
                            success = true,
                            message = "User status updated successfully",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                },
                onFailure = { throw it }
            )
        }

        // POST /admin/users/staff
        post<AdminResource.Staff> {
            val request = call.receive<StaffRegisterRequest>()
            validateStaffRegisterRequest(request)

            val result = adminService.registerStaff(request)
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.Created, it) },
                onFailure = { throw it }
            )
        }

        // GET /admin/users
        get<AdminResource.Users> { resource ->
            val errors = mutableListOf<FieldError>()

            val page = resource.page ?: 1
            val size = resource.size ?: 20

            if (page < 1) {
                errors.add(FieldError("page", "invalid_value", "Page must be >= 1"))
            }
            if (size < 1 || size > 100) {
                errors.add(FieldError("size", "invalid_value", "Size must be between 1 and 100"))
            }

            val role = resource.role?.let { parseRole(it, errors) }
            val createdFrom = resource.createdFrom?.let { parseDate(it, "createdFrom", errors) }
            val createdTo = resource.createdTo?.let { parseDate(it, "createdTo", errors) }

            if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
                errors.add(FieldError("createdFrom", "invalid_range", "createdFrom must be before createdTo"))
            }

            val sortBy = resource.sortBy ?: "createdAt"
            val sortOrder = resource.sortOrder ?: "desc"

            if (sortOrder.lowercase() !in listOf("asc", "desc")) {
                errors.add(FieldError("sortOrder", "invalid_value", "sortOrder must be 'asc' or 'desc'"))
            }

            if (errors.isNotEmpty()) throw ValidationException(errors)

            val result = adminService.getAllUsers(page, size, role, createdFrom, createdTo, sortBy, sortOrder)
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.OK, it) },
                onFailure = { throw it }
            )
        }

        // GET /admin/users/count
        get<AdminResource.UsersCount> { resource ->
            val errors = mutableListOf<FieldError>()

            val role = resource.role?.let { parseRole(it, errors) }
            val createdFrom = resource.createdFrom?.let { parseDate(it, "createdFrom", errors) }
            val createdTo = resource.createdTo?.let { parseDate(it, "createdTo", errors) }

            if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
                errors.add(FieldError("createdFrom", "invalid_range", "createdFrom must be before createdTo"))
            }

            if (errors.isNotEmpty()) throw ValidationException(errors)

            val result = adminService.getUsersCount(role, createdFrom, createdTo)
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.OK, mapOf("count" to it)) },
                onFailure = { throw it }
            )
        }

        // GET /admin/users/{userId} — после более специфичных маршрутов
        get<AdminResource.UserById> { resource ->
            validateUserId(resource.userId)

            val result = adminService.getUserById(resource.userId)
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.OK, it) },
                onFailure = { throw it }
            )
        }

        // Остальные маршруты статистики...
        get<AdminResource.StudentsOverall> {
            val result = adminService.getOverallStudentsCount()
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.OK, mapOf("totalStudents" to it)) },
                onFailure = { throw it }
            )
        }

        get<AdminResource.StudentsByCourse> { resource ->
            validateCourseId(resource.courseId)
            val result = adminService.getStudentsCountByCourse(resource.courseId)
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.OK, mapOf("studentsCount" to it)) },
                onFailure = { throw it }
            )
        }

        get<AdminResource.CourseStatistics> { resource ->
            validateCourseId(resource.courseId)
            val result = adminService.getCourseStatistics(resource.courseId)
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.OK, it) },
                onFailure = { throw it }
            )
        }

        get<AdminResource.OverallStatistics> {
            val result = adminService.getOverallStatistics()
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.OK, it) },
                onFailure = { throw it }
            )
        }
    }
}

private fun validateUserId(userId: String) {
    if (userId.isBlank()) {
        throw ValidationException(listOf(FieldError("userId", "required", "User ID is required")))
    }
    try {
        UUID.fromString(userId)
    } catch (e: IllegalArgumentException) {
        throw ValidationException(listOf(FieldError("userId", "invalid_format", "User ID must be a valid UUID")))
    }
}

private fun validateCourseId(courseId: String) {
    if (courseId.isBlank()) {
        throw ValidationException(listOf(FieldError("courseId", "required", "Course ID is required")))
    }
    try {
        UUID.fromString(courseId)
    } catch (e: IllegalArgumentException) {
        throw ValidationException(listOf(FieldError("courseId", "invalid_format", "Course ID must be a valid UUID")))
    }
}

private fun parseRole(value: String, errors: MutableList<FieldError>): UserRole? {
    return try {
        UserRole.valueOf(value.uppercase())
    } catch (e: IllegalArgumentException) {
        errors.add(FieldError("role", "invalid_value", "Invalid role. Allowed: ${UserRole.entries.joinToString()}"))
        null
    }
}

private fun parseDate(value: String, field: String, errors: MutableList<FieldError>): LocalDate? {
    return try {
        LocalDate.parse(value)
    } catch (e: DateTimeParseException) {
        errors.add(FieldError(field, "invalid_format", "$field must be in ISO format (YYYY-MM-DD)"))
        null
    }
}

private fun validateStaffRegisterRequest(request: StaffRegisterRequest) {
    val errors = mutableListOf<FieldError>()

    if (request.email.isBlank()) {
        errors.add(FieldError("email", "required", "Email is required"))
    } else if (!request.email.matches(Regex("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$"))) {
        errors.add(FieldError("email", "invalid_format", "Invalid email format"))
    }

    if (request.password.isBlank()) {
        errors.add(FieldError("password", "required", "Password is required"))
    } else if (request.password.length < 8) {
        errors.add(FieldError("password", "too_short", "Password must be at least 8 characters"))
    }

    if (request.name.isBlank()) {
        errors.add(FieldError("name", "required", "Name is required"))
    } else if (request.name.length > 100) {
        errors.add(FieldError("name", "too_long", "Name must be at most 100 characters"))
    }

    if (errors.isNotEmpty()) throw ValidationException(errors)
}
