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

            route("/tasks") {
                post {
                    val request = call.receive<CreateTaskRequest>()
                    val result = contentService.createTask(request)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to create task",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                put("/{taskId}") {
                    val taskId = call.parameters["taskId"]
                    if (taskId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing task ID")
                        return@put
                    }

                    val request = call.receive<UpdateTaskRequest>()
                    val result = contentService.updateTask(taskId, request)

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Task not found")
                    }
                }

                delete("/{taskId}") {
                    val taskId = call.parameters["taskId"]
                    if (taskId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing task ID")
                        return@delete
                    }

                    val result = contentService.deleteTask(taskId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Task deleted successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Task not found")
                    }
                }

                route("/{taskId}/content") {
                    post {
                        val taskId = call.parameters["taskId"]
                        if (taskId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing task ID")
                            return@post
                        }
                        val request = call.receive<CreateTaskContentRequest>()
                        val result = contentService.createTaskContent(taskId, request)
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = result.exceptionOrNull()?.message ?: "Failed to create task content",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    put("/{contentId}") {
                        val contentId = call.parameters["contentId"]
                        if (contentId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing content ID")
                            return@put
                        }
                        val request = call.receive<UpdateTaskContentRequest>()
                        val result = contentService.updateTaskContent(contentId, request)
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "TaskContent not found")
                        }
                    }

                    delete("/{contentId}") {
                        val contentId = call.parameters["contentId"]
                        if (contentId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing content ID")
                            return@delete
                        }
                        val result = contentService.deleteTaskContent(contentId)
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, mapOf("message" to "TaskContent deleted successfully"))
                        } else {
                            call.respond(HttpStatusCode.NotFound, "TaskContent not found")
                        }
                    }
                }

                route("/{taskId}/answer") {
                    post {
                        val taskId = call.parameters["taskId"]
                        if (taskId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing task ID")
                            return@post
                        }
                        val request = call.receive<CreateTaskAnswerRequest>()
                        val result = contentService.createTaskAnswer(taskId, request)
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = result.exceptionOrNull()?.message ?: "Failed to create task answer",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    put {
                        val taskId = call.parameters["taskId"]
                        if (taskId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing task ID")
                            return@put
                        }
                        val request = call.receive<UpdateTaskAnswerRequest>()
                        val result = contentService.updateTaskAnswer(taskId, request)
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "TaskAnswer not found")
                        }
                    }

                    delete {
                        val taskId = call.parameters["taskId"]
                        if (taskId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing task ID")
                            return@delete
                        }
                        val result = contentService.deleteTaskAnswer(taskId)
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, mapOf("message" to "TaskAnswer deleted successfully"))
                        } else {
                            call.respond(HttpStatusCode.NotFound, "TaskAnswer not found")
                        }
                    }
                }

                route("/{taskId}/options") {
                    post {
                        val taskId = call.parameters["taskId"]
                        if (taskId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing task ID")
                            return@post
                        }
                        val request = call.receive<CreateTaskAnswerOptionRequest>()
                        val result = contentService.createTaskAnswerOption(taskId, request)
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    success = false,
                                    error = result.exceptionOrNull()?.message ?: "Failed to create answer option",
                                    code = null,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    put("/{optionId}") {
                        val optionId = call.parameters["optionId"]
                        if (optionId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing option ID")
                            return@put
                        }
                        val request = call.receive<UpdateTaskAnswerOptionRequest>()
                        val result = contentService.updateTaskAnswerOption(optionId, request)
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "TaskAnswerOption not found")
                        }
                    }

                    delete("/{optionId}") {
                        val optionId = call.parameters["optionId"]
                        if (optionId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing option ID")
                            return@delete
                        }
                        val result = contentService.deleteTaskAnswerOption(optionId)
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, mapOf("message" to "TaskAnswerOption deleted successfully"))
                        } else {
                            call.respond(HttpStatusCode.NotFound, "TaskAnswerOption not found")
                        }
                    }
                }
            }

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
            route("/sections") {
                post {
                    val request = call.receive<CreateSectionRequest>()
                    val result = contentService.createSection(request)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                success = false,
                                error = result.exceptionOrNull()?.message ?: "Failed to create section",
                                code = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                put("/{sectionId}") {
                    val sectionId = call.parameters["sectionId"]
                    if (sectionId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing section ID")
                        return@put
                    }
                    val request = call.receive<UpdateSectionRequest>()
                    val result = contentService.updateSection(sectionId, request)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Section not found")
                    }
                }

                delete("/{sectionId}") {
                    val sectionId = call.parameters["sectionId"]
                    if (sectionId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing section ID")
                        return@delete
                    }
                    val result = contentService.deleteSection(sectionId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Section deleted successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Section not found")
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