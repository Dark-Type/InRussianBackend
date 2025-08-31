package com.inRussian.routes

import com.inRussian.repositories.TaskRepository
import com.inRussian.requests.content.CreateTaskModelRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.taskRoutes(taskRepository: TaskRepository) {
    route("/task") {
        get("/{id}") {
            val courseId = UUID.fromString(call.parameters["id"])
            call.respond(HttpStatusCode.OK, taskRepository.getTaskById(courseId))

        }
        post {
            val request = call.receive<CreateTaskModelRequest>()
            val response = taskRepository.createTask(request)
            call.respond(HttpStatusCode.OK, response)
        }
        get("/course/{id}") {
            val courseId = UUID.fromString(call.parameters["id"])
            call.respond(HttpStatusCode.OK, taskRepository.getTaskByCourseId(courseId))
        }
    }
}