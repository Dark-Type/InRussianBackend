package com.inRussian.routes

import com.inRussian.services.ContentService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.contentRoutes(contentService: ContentService) {
        // TODO("fix authenticate for `content-jwt`")
        get("/content/tasks/{taskId}") {
            val taskId = call.parameters["taskId"]
            if (taskId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing task ID")
                return@get
            }
            val result = contentService.getTask(taskId)
            if (result.isSuccess) {
                call.respond(HttpStatusCode.OK, result.getOrNull()!!)
            } else {
                call.respond(HttpStatusCode.NotFound, "Task not found")
            }
        }


    authenticate("auth-jwt") {
        route("/content") {

            route("/tasks") {


//                get("/{taskId}") {
//                    val taskId = call.parameters["taskId"]
//                    if (taskId == null) {
//                        call.respond(HttpStatusCode.BadRequest, "Missing task ID")
//                        return@get
//                    }
//
//                    val result = contentService.getTask(taskId)
//                    if (result.isSuccess) {
//                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
//                    } else {
//                        call.respond(HttpStatusCode.NotFound, "Task not found")
//                    }
//                }


                route("/{taskId}/content") {

                    get("/{contentId}") {
                        val contentId = call.parameters["contentId"]
                        if (contentId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing content ID")
                            return@get
                        }
                        val result = contentService.getTaskContent(contentId)
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "TaskContent not found")
                        }
                    }
                }

                route("/{taskId}/answer") {

                    get {
                        val taskId = call.parameters["taskId"]
                        if (taskId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing task ID")
                            return@get
                        }
                        val result = contentService.getTaskAnswer(taskId)
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "TaskAnswer not found")
                        }
                    }
                }

                route("/{taskId}/options") {
                    get("/{optionId}") {
                        val optionId = call.parameters["optionId"]
                        if (optionId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing option ID")
                            return@get
                        }
                        val result = contentService.getTaskAnswerOption(optionId)
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "TaskAnswerOption not found")
                        }
                    }
                }
            }

            route("/themes") {

                get("/{themeId}") {
                    val themeId = call.parameters["themeId"]
                    if (themeId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing theme ID")
                        return@get
                    }
                    val result = contentService.getTheme(themeId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Theme not found")
                    }
                }
                get("/{themeId}/tasks") {
                    val themeId = call.parameters["themeId"]
                    if (themeId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing theme ID")
                        return@get
                    }
                    val result = contentService.getTasksByTheme(themeId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to get tasks")
                    }
                }

                get("/by-section/{sectionId}") {
                    val sectionId = call.parameters["sectionId"]
                    if (sectionId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing section ID")
                        return@get
                    }
                    val result = contentService.getThemesBySection(sectionId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to get themes")
                    }
                }
            }
            route("/sections") {

                get("/{sectionId}") {
                    val sectionId = call.parameters["sectionId"]
                    if (sectionId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing section ID")
                        return@get
                    }
                    val result = contentService.getSection(sectionId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Section not found")
                    }
                }

                get("/by-course/{courseId}") {
                    val courseId = call.parameters["courseId"]
                    if (courseId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing course ID")
                        return@get
                    }
                    val result = contentService.getSectionsByCourse(courseId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to get sections")
                    }
                }
            }

            route("/courses") {
                get("/{courseId}") {
                    val courseId = call.parameters["courseId"]
                    if (courseId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing course ID")
                        return@get
                    }
                    val result = contentService.getCourse(courseId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Course not found")
                    }
                }


                get {
                    val result = contentService.getAllCourses()
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to get courses")
                    }
                }
            }

            route("/reports") {
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
                get {
                    val result = contentService.getAllReports()
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to get reports")
                    }
                }
            }

            route("/stats") {
                get {
                    val result = contentService.getCountStats()
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to get statistics")
                    }
                }
                get("/section/{sectionId}/tasks-count") {
                    val sectionId = call.parameters["sectionId"]
                    if (sectionId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing section ID")
                        return@get
                    }
                    val result = contentService.getSectionTasksCount(sectionId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("count" to result.getOrNull()))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to get tasks count")
                    }
                }
                get("/theme/{themeId}/tasks-count") {
                    val themeId = call.parameters["themeId"]
                    if (themeId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing theme ID")
                        return@get
                    }
                    val result = contentService.getThemeTasksCount(themeId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("count" to result.getOrNull()))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to get tasks count")
                    }
                }

                get("/course/{courseId}/tasks-count") {
                    val courseId = call.parameters["courseId"]
                    if (courseId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing course ID")
                        return@get
                    }

                    val result = contentService.getCourseTasksCount(courseId)
                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, mapOf("count" to result.getOrNull()))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to get tasks count")
                    }
                }
            }
        }
    }
}