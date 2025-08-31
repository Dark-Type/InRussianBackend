package com.inRussian.routes

import com.inRussian.repositories.TaskRepository
import com.inRussian.requests.content.CreateTaskModelRequest
import com.inRussian.requests.content.UpdateTaskModelRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

fun Route.taskRoutes(taskRepository: TaskRepository) {

    route("/task") {
        authenticate("auth-jwt") {
            get("/{id}") {
                val id = UUID.fromString(call.parameters["id"])
                call.respond(HttpStatusCode.OK, taskRepository.getTaskById(id))
            }
            get("/theme/{id}") {
                val courseId = UUID.fromString(call.parameters["id"])
                call.respond(HttpStatusCode.OK, taskRepository.getTaskByThemeId(courseId))
            }
        }
        authenticate("content-jwt") {
            post {
                val request = call.receive<CreateTaskModelRequest>()
                val response = taskRepository.createTask(request)
                call.respond(HttpStatusCode.OK, response)
            }
            put("/{id}") {
                val id = UUID.fromString(call.parameters["id"])
                val request = call.receive<UpdateTaskModelRequest>()
                val updated = taskRepository.updateTask(id, request)
                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.OK, updated)
                }
            }
            delete("/{id}") {
                val id = UUID.fromString(call.parameters["id"])
                val deleted = taskRepository.deleteTask(id)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}