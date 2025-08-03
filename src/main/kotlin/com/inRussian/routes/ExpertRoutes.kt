package com.inRussian.routes

import com.inRussian.responses.common.ErrorResponse
import com.inRussian.services.ExpertService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun Route.expertRoutes(expertService: ExpertService) {
    authenticate("expert-jwt") {
        route("/expert") {

            route("/students") {
                get {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val sortBy = call.request.queryParameters["sortBy"] ?: "createdAt"
                    val sortOrder = call.request.queryParameters["sortOrder"] ?: "desc"

                    val createdFrom = call.request.queryParameters["createdFrom"]?.let {
                        try {
                            LocalDate.parse(it)
                        } catch (_: DateTimeParseException) {
                            null
                        }
                    }
                    val createdTo = call.request.queryParameters["createdTo"]?.let {
                        try {
                            LocalDate.parse(it)
                        } catch (_: DateTimeParseException) {
                            null
                        }
                    }

                    val result = expertService.getAllStudents(page, size, createdFrom, createdTo, sortBy, sortOrder)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Не удалось получить список студентов",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/count") {
                    val createdFrom = call.request.queryParameters["createdFrom"]?.let {
                        try {
                            LocalDate.parse(it)
                        } catch (_: DateTimeParseException) {
                            null
                        }
                    }
                    val createdTo = call.request.queryParameters["createdTo"]?.let {
                        try {
                            LocalDate.parse(it)
                        } catch (_: DateTimeParseException) {
                            null
                        }
                    }

                    val result = expertService.getStudentsCount(createdFrom, createdTo)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("count" to result.getOrNull()!!))
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Не удалось получить количество студентов",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/with-profiles") {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20

                    val result = expertService.getStudentsWithProfiles(page, size)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message
                                    ?: "Не удалось получить студентов с профилями",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/course/{courseId}") {
                    val courseId = call.parameters["courseId"]
                    if (courseId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = "Не указан courseId",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@get
                    }

                    val result = expertService.getStudentsByCourse(courseId)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Не удалось получить студентов курса",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            route("/statistics") {
                get("/students/overall") {
                    val result = expertService.getOverallStudentsCount()

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("totalStudents" to result.getOrNull()!!))
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message
                                    ?: "Не удалось получить общее количество студентов",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/students/course/{courseId}") {
                    val courseId = call.parameters["courseId"]
                    if (courseId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = "Не указан courseId",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@get
                    }

                    val result = expertService.getStudentsCountByCourse(courseId)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("studentsCount" to result.getOrNull()!!))
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message
                                    ?: "Не удалось получить количество студентов курса",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/course/{courseId}/average-time") {
                    val courseId = call.parameters["courseId"]
                    if (courseId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = "Не указан courseId",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@get
                    }

                    val result = expertService.getCourseAverageTime(courseId)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("averageTime" to result.getOrNull()))
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Не удалось получить среднее время курса",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/course/{courseId}/average-progress") {
                    val courseId = call.parameters["courseId"]
                    if (courseId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = "Не указан courseId",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@get
                    }

                    val result = expertService.getCourseAverageProgress(courseId)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("averageProgress" to result.getOrNull()))
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message
                                    ?: "Не удалось получить средний прогресс курса",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/overall-average-time") {
                    val result = expertService.getOverallAverageTime()

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("averageTime" to result.getOrNull()))
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Не удалось получить общее среднее время",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/overall-average-progress") {
                    val result = expertService.getOverallAverageProgress()

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("averageProgress" to result.getOrNull()))
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message
                                    ?: "Не удалось получить общий средний прогресс",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            route("/content") {
                get("/courses") {
                    val result = expertService.getAllCourses()

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Не удалось получить курсы",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/courses/{courseId}") {
                    val courseId = call.parameters["courseId"]
                    if (courseId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = "Не указан courseId",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@get
                    }

                    val result = expertService.getCourse(courseId)

                    if (result.isSuccess) {
                        val course = result.getOrNull()
                        if (course != null) {
                            call.respond(HttpStatusCode.OK, course)
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(
                                    success = false,
                                    error = "Курс не найден",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Не удалось получить курс",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/reports") {
                    val result = expertService.getAllReports()

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Не удалось получить отчёты",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/stats") {
                    val result = expertService.getCountStats()

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Не удалось получить статистику",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }
}