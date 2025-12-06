package com.inRussian.routes

import com.inRussian.responses.common.ErrorResponse
import com.inRussian.services.v3.StudentService
import io.ktor.http.*
import io.ktor.resources.Resource
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import java.util.UUID

fun Route.studentRoutes(studentService: StudentService) {
    authenticate("student-jwt") {
        // GET /student/courses
        get<StudentResource.Courses> {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()

            if (!userId.isUuid()) {
                return@get call.respondError("invalid_userId", HttpStatusCode.BadRequest)
            }

            val result = studentService.getCoursesByUserLanguage(userId)
            if (result.isSuccess) {
                call.respond(HttpStatusCode.OK, result.getOrNull()!!)
            } else {
                val reason = result.exceptionOrNull()?.message ?: "failed_to_get_courses"
                val status = when (reason) {
                    "invalid_userId" -> HttpStatusCode.BadRequest
                    "user_not_found" -> HttpStatusCode.NotFound
                    else -> HttpStatusCode.InternalServerError
                }
                call.respondError(reason, status)
            }
        }

        // POST /student/courses/{courseId}/enroll
        post<StudentResource.Enroll> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()
            val courseId = res.courseId

            if (!userId.isUuid()) return@post call.respondError("invalid_userId", HttpStatusCode.BadRequest)
            if (!courseId.isUuid()) return@post call.respondError("invalid_courseId", HttpStatusCode.BadRequest)

            val result = studentService.enrollInCourse(userId, courseId)
            if (result.isSuccess) {
                call.respond(HttpStatusCode.OK, mapOf("enrolled" to true))
            } else {
                val reason = result.exceptionOrNull()?.message ?: "enroll_failed"
                val status = when (reason) {
                    "invalid_userId", "invalid_courseId" -> HttpStatusCode.BadRequest
                    "user_not_found", "course_not_found" -> HttpStatusCode.NotFound
                    "already_enrolled_or_failed" -> HttpStatusCode.Conflict
                    else -> HttpStatusCode.InternalServerError
                }
                call.respondError(reason, status)
            }
        }

        // GET /student/enrollments
        get<StudentResource.Enrollments> {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()

            if (!userId.isUuid()) {
                return@get call.respondError("invalid_userId", HttpStatusCode.BadRequest)
            }

            val result = studentService.getUserEnrollments(userId)
            if (result.isSuccess) {
                val enrollments = result.getOrNull()
                if (enrollments != null) {
                    call.respond(HttpStatusCode.OK, enrollments)
                } else {
                    call.respondError("enrollments_not_found", HttpStatusCode.NotFound)
                }
            } else {
                val reason = result.exceptionOrNull()?.message ?: "failed_to_get_enrollments"
                val status = when (reason) {
                    "invalid_userId" -> HttpStatusCode.BadRequest
                    "user_not_found" -> HttpStatusCode.NotFound
                    else -> HttpStatusCode.InternalServerError
                }
                call.respondError(reason, status)
            }
        }

        // DELETE /student/courses/{courseId}/enroll
        delete<StudentResource.Enroll> { res ->
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asString()
            val courseId = res.courseId

            if (!userId.isUuid()) return@delete call.respondError("invalid_userId", HttpStatusCode.BadRequest)
            if (!courseId.isUuid()) return@delete call.respondError("invalid_courseId", HttpStatusCode.BadRequest)

            val result = studentService.unenrollFromCourse(userId, courseId)
            if (result.isSuccess) {
                call.respond(HttpStatusCode.OK, mapOf("unenrolled" to true))
            } else {
                val reason = result.exceptionOrNull()?.message ?: "unenroll_failed"
                val status = when (reason) {
                    "invalid_userId", "invalid_courseId" -> HttpStatusCode.BadRequest
                    "user_not_found", "course_not_found", "enrollment_not_found" -> HttpStatusCode.NotFound
                    else -> HttpStatusCode.InternalServerError
                }
                call.respondError(reason, status)
            }
        }
    }
}

// -------- Resources --------
@Serializable
@Resource("/student")
class StudentResource {
    @Serializable
    @Resource("courses")
    class Courses(val parent: StudentResource = StudentResource())

    @Serializable
    @Resource("courses/{courseId}/enroll")
    class Enroll(val parent: StudentResource = StudentResource(), val courseId: String)

    @Serializable
    @Resource("enrollments")
    class Enrollments(val parent: StudentResource = StudentResource())
}

// -------- Helpers --------
private fun String.isUuid(): Boolean = runCatching { UUID.fromString(this) }.isSuccess

private suspend fun io.ktor.server.application.ApplicationCall.respondError(
    message: String,
    status: HttpStatusCode
) = respond(status, ErrorResponse(false, message, null, System.currentTimeMillis()))