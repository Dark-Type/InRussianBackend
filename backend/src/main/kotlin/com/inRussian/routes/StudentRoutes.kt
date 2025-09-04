package com.inRussian.routes

import com.inRussian.responses.common.ErrorResponse
import com.inRussian.services.StudentService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentRoutes(studentService: StudentService) {
    authenticate("student-jwt") {
        route("/student") {

            get("/courses") {

                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val result = studentService.getCoursesByUserLanguage(userId)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить курсы", null, System.currentTimeMillis())
                    )
                }
            }

            post("/courses/{courseId}/enroll") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val courseId = call.parameters["courseId"]
                if (courseId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан courseId", null, System.currentTimeMillis())
                    )
                    return@post
                }
                val result = studentService.enrollInCourse(userId, courseId)
                if (result.isSuccess) {
                    val enrolled = result.getOrNull()
                    if (enrolled == true) {
                        call.respond(HttpStatusCode.OK, enrolled)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(false, "Запись на курс не удалась", null, System.currentTimeMillis())
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось записаться на курс", null, System.currentTimeMillis())
                    )
                }
            }

            get("/enrollments") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val result = studentService.getUserEnrollments(userId)
                if (result.isSuccess) {
                    val enrollments = result.getOrNull()
                    if (enrollments != null) {
                        call.respond(HttpStatusCode.OK, enrollments)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(false, "Курсы пользователя не найдены", null, System.currentTimeMillis())
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить курсы пользователя", null, System.currentTimeMillis())
                    )
                }
            }

            delete("/courses/{courseId}/enroll") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val courseId = call.parameters["courseId"]
                if (courseId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан courseId", null, System.currentTimeMillis())
                    )
                    return@delete
                }
                val result = studentService.unenrollFromCourse(userId, courseId)
                if (result.isSuccess) {
                    val unenrolled = result.getOrNull()
                    if (unenrolled == true) {
                        call.respond(HttpStatusCode.OK, unenrolled)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(false, "Отписка от курса не удалась", null, System.currentTimeMillis())
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось отписаться от курса", null, System.currentTimeMillis())
                    )
                }
            }

        }
    }
}