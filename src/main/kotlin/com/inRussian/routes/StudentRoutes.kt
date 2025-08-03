package com.inRussian.routes

import com.inRussian.responses.common.ErrorResponse
import com.inRussian.services.StudentService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
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

            get("/courses/{courseId}/sections") {
                val courseId = call.parameters["courseId"]
                if (courseId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан courseId", null, System.currentTimeMillis())
                    )
                    return@get
                }
                val result = studentService.getSectionsByCourse(courseId)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить секции", null, System.currentTimeMillis())
                    )
                }
            }

            get("/courses/{courseId}/themes") {
                val courseId = call.parameters["courseId"]
                if (courseId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан courseId", null, System.currentTimeMillis())
                    )
                    return@get
                }
                val result = studentService.getThemesByCourse(courseId)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить темы", null, System.currentTimeMillis())
                    )
                }
            }

            get("/themes/{themeId}/tasks") {
                val themeId = call.parameters["themeId"]
                if (themeId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан themeId", null, System.currentTimeMillis())
                    )
                    return@get
                }
                val result = studentService.getTasksByTheme(themeId)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить задачи", null, System.currentTimeMillis())
                    )
                }
            }

            get("/tasks/{taskId}/variants") {
                val taskId = call.parameters["taskId"]
                if (taskId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан taskId", null, System.currentTimeMillis())
                    )
                    return@get
                }
                val result = studentService.getTaskVariants(taskId)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить варианты задачи", null, System.currentTimeMillis())
                    )
                }
            }

            get("/tasks/{taskId}/content") {
                val taskId = call.parameters["taskId"]
                if (taskId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан taskId", null, System.currentTimeMillis())
                    )
                    return@get
                }
                val result = studentService.getTaskContent(taskId)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить контент задачи", null, System.currentTimeMillis())
                    )
                }
            }

            get("/tasks/{taskId}/query") {
                val taskId = call.parameters["taskId"]
                if (taskId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан taskId", null, System.currentTimeMillis())
                    )
                    return@get
                }
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val result = studentService.getTaskQuery(userId, taskId)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить query задачи", null, System.currentTimeMillis())
                    )
                }
            }

            get("/badges") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val result = studentService.getUserBadges(userId)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить бейджи", null, System.currentTimeMillis())
                    )
                }
            }
            post("/badges") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val req = call.receive<Map<String, String>>()
                val badgeId = req["badgeId"]
                val courseId = req["courseId"]
                val themeId = req["themeId"]
                if (badgeId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан badgeId", null, System.currentTimeMillis())
                    )
                    return@post
                }
                val result = studentService.createUserBadge(userId, badgeId, courseId, themeId)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось добавить бейдж", null, System.currentTimeMillis())
                    )
                }
            }

            post("/task-queue") {
                val request = call.receive<com.inRussian.repositories.CreateTaskQueueRequest>()
                val result = studentService.addTaskToQueue(request)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось добавить задачу в очередь", null, System.currentTimeMillis())
                    )
                }
            }

            get("/task-queue") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val result = studentService.getTaskQueue(userId)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить очередь задач", null, System.currentTimeMillis())
                    )
                }
            }

            patch("/task-queue/{queueId}/position") {
                val queueId = call.parameters["queueId"]
                val req = call.receive<Map<String, Int>>()
                val newPosition = req["newPosition"]
                if (queueId == null || newPosition == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указаны queueId или newPosition", null, System.currentTimeMillis())
                    )
                    return@patch
                }
                val result = studentService.updateTaskQueuePosition(queueId, newPosition)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось обновить позицию", null, System.currentTimeMillis())
                    )
                }
            }

            delete("/task-queue/{queueId}") {
                val queueId = call.parameters["queueId"]
                if (queueId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан queueId", null, System.currentTimeMillis())
                    )
                    return@delete
                }
                val result = studentService.removeTaskFromQueue(queueId)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось удалить задачу из очереди", null, System.currentTimeMillis())
                    )
                }
            }

            get("/task-queue/next") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val result = studentService.getNextTaskInQueue(userId)
                if (result.isSuccess) {
                    val item = result.getOrNull()
                    if (item != null) {
                        call.respond(HttpStatusCode.OK, item)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(
                                false,
                                "Следующая задача в очереди не найдена",
                                null,
                                System.currentTimeMillis()
                            )
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить следующую задачу", null, System.currentTimeMillis())
                    )
                }
            }

            post("/tasks/{taskId}/progress") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val taskId = call.parameters["taskId"]
                if (taskId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан taskId", null, System.currentTimeMillis())
                    )
                    return@post
                }
                val result = studentService.createTaskProgress(userId, taskId)
                if (result.isSuccess) {
                    val progress = result.getOrNull()
                    if (progress != null) {
                        call.respond(HttpStatusCode.OK, progress)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(false, "Прогресс задачи не найден", null, System.currentTimeMillis())
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось создать прогресс задачи", null, System.currentTimeMillis())
                    )
                }
            }

            get("/tasks/{taskId}/progress") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val taskId = call.parameters["taskId"]
                if (taskId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан taskId", null, System.currentTimeMillis())
                    )
                    return@get
                }
                val result = studentService.getTaskProgress(userId, taskId)
                if (result.isSuccess) {
                    val progress = result.getOrNull()
                    if (progress != null) {
                        call.respond(HttpStatusCode.OK, progress)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(false, "Прогресс задачи не найден", null, System.currentTimeMillis())
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить прогресс задачи", null, System.currentTimeMillis())
                    )
                }
            }

            patch("/tasks/{taskId}/progress") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val taskId = call.parameters["taskId"]
                val request = call.receive<com.inRussian.repositories.UpdateTaskProgressRequest>()
                if (taskId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан taskId", null, System.currentTimeMillis())
                    )
                    return@patch
                }
                val result = studentService.updateTaskProgress(userId, taskId, request)
                if (result.isSuccess) {
                    val progress = result.getOrNull()
                    if (progress != null) {
                        call.respond(HttpStatusCode.OK, progress)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(false, "Прогресс задачи не найден", null, System.currentTimeMillis())
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось обновить прогресс задачи", null, System.currentTimeMillis())
                    )
                }
            }

            post("/tasks/{taskId}/complete") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val taskId = call.parameters["taskId"]
                val req = call.receive<Map<String, Boolean>>()
                val isCorrect = req["isCorrect"] ?: false
                if (taskId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан taskId", null, System.currentTimeMillis())
                    )
                    return@post
                }
                val result = studentService.markTaskAsCompleted(userId, taskId, isCorrect)
                if (result.isSuccess) {
                    val progress = result.getOrNull()
                    if (progress != null) {
                        call.respond(HttpStatusCode.OK, progress)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(false, "Прогресс задачи не найден", null, System.currentTimeMillis())
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(
                            false,
                            "Не удалось отметить задачу как выполненную",
                            null,
                            System.currentTimeMillis()
                        )
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

            get("/sections/{sectionId}/progress") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val sectionId = call.parameters["sectionId"]
                if (sectionId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан sectionId", null, System.currentTimeMillis())
                    )
                    return@get
                }
                val result = studentService.getSectionProgress(userId, sectionId)
                if (result.isSuccess) {
                    val progress = result.getOrNull()
                    if (progress != null) {
                        call.respond(HttpStatusCode.OK, progress)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(false, "Прогресс секции не найден", null, System.currentTimeMillis())
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить прогресс секции", null, System.currentTimeMillis())
                    )
                }
            }

            get("/courses/{courseId}/progress") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val courseId = call.parameters["courseId"]
                if (courseId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан courseId", null, System.currentTimeMillis())
                    )
                    return@get
                }
                val result = studentService.getCourseProgress(userId, courseId)
                if (result.isSuccess) {
                    val progress = result.getOrNull()
                    if (progress != null) {
                        call.respond(HttpStatusCode.OK, progress)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(false, "Прогресс курса не найден", null, System.currentTimeMillis())
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить прогресс курса", null, System.currentTimeMillis())
                    )
                }
            }



            post("/tasks/{taskId}/report") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asString()
                val taskId = call.parameters["taskId"]
                val req = call.receive<Map<String, String>>()
                val description = req["description"]
                if (taskId == null || description == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан taskId или description", null, System.currentTimeMillis())
                    )
                    return@post
                }
                val result = studentService.createReport(userId, taskId, description)
                if (result.isSuccess) {
                    val report = result.getOrNull()
                    if (report != null) {
                        call.respond(HttpStatusCode.OK, report)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(false, "Отчёт не создан", null, System.currentTimeMillis())
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось создать отчёт", null, System.currentTimeMillis())
                    )
                }
            }

            get("/tasks/{taskId}/answer") {
                val taskId = call.parameters["taskId"]
                if (taskId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(false, "Не указан taskId", null, System.currentTimeMillis())
                    )
                    return@get
                }
                val result = studentService.getTaskAnswer(taskId)
                if (result.isSuccess) {
                    val answer = result.getOrNull()
                    if (answer != null) {
                        call.respond(HttpStatusCode.OK, answer)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(false, "Ответ задачи не найден", null, System.currentTimeMillis())
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(false, "Не удалось получить ответ задачи", null, System.currentTimeMillis())
                    )
                }
            }
        }
    }
}