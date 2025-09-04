package com.inRussian.routes

import io.ktor.server.auth.jwt.JWTPrincipal
import com.inRussian.config.getUserId
import com.inRussian.requests.content.*
import com.inRussian.responses.common.ErrorResponse
import com.inRussian.services.ContentService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.contentManagerRoutes(contentService: ContentService) {
    authenticate("content-jwt") {
        route("/content") {

            route("/themes") {
                post {
                    val request = call.receive<CreateThemeRequest>()
                    val result = contentService.createTheme(request)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to create theme",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                put("/{themeId}") {
                    val themeId = call.parameters["themeId"]
                    if (themeId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing theme ID")
                        return@put
                    }
                    val request = call.receive<UpdateThemeRequest>()
                    val result = contentService.updateTheme(themeId, request)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Theme not found")
                    }
                }

                delete("/{themeId}") {
                    val themeId = call.parameters["themeId"]
                    if (themeId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing theme ID")
                        return@delete
                    }
                    val result = contentService.deleteTheme(themeId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Theme deleted successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Theme not found")
                    }
                }
            }

            route("/courses") {
                post {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getUserId()
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Missing author ID")
                        return@post
                    }
                    val request = call.receive<CreateCourseRequest>()
                    val result = contentService.createCourse(userId, request)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to create course",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                put("/{courseId}") {
                    val courseId = call.parameters["courseId"]
                    if (courseId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing course ID")
                        return@put
                    }
                    val request = call.receive<UpdateCourseRequest>()
                    val result = contentService.updateCourse(courseId, request)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Course not found")
                    }
                }

                delete("/{courseId}") {
                    val courseId = call.parameters["courseId"]
                    if (courseId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing course ID")
                        return@delete
                    }
                    val result = contentService.deleteCourse(courseId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Course deleted successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Course not found")
                    }
                }
            }

            route("/reports") {
                post {
                    val reporterId = call.principal<UserIdPrincipal>()?.name
                    if (reporterId == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Missing reporter ID")
                        return@post
                    }
                    val request = call.receive<CreateReportRequest>()
                    val result = contentService.createReport(reporterId, request)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to create report",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                get("/{reportId}") {
                    val reportId = call.parameters["reportId"]
                    if (reportId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing report ID")
                        return@get
                    }
                    val result = contentService.getReport(reportId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Report not found")
                    }
                }

                delete("/{reportId}") {
                    val reportId = call.parameters["reportId"]
                    if (reportId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing report ID")
                        return@delete
                    }
                    val result = contentService.deleteReport(reportId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Report deleted successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Report not found")
                    }
                }
            }

        }
    }
}