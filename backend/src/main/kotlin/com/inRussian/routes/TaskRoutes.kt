package com.inRussian.routes

import com.inRussian.requests.content.CreateTaskModelRequest
import com.inRussian.requests.content.UpdateTaskModelRequest
import com.inRussian.repositories.TasksRepository
import io.ktor.http.*
import io.ktor.resources.Resource
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.resources.delete
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import java.util.UUID

fun Route.taskRoutes(taskRepository: TasksRepository) {

    authenticate("auth-jwt") {
        // GET /task/{id}
        get<TaskResource.ById> { res ->
            val id = res.id.toUuidOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid task id")
            val task = taskRepository.getTaskById(id)
            call.respond(HttpStatusCode.OK, task)
        }

        // GET /task/theme/{id}
        get<TaskResource.ByTheme> { res ->
            val themeId = res.id.toUuidOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid theme id")
            val tasks = taskRepository.getTaskByThemeId(themeId)
            call.respond(HttpStatusCode.OK, tasks)
        }
    }

    authenticate("content-jwt") {
        // POST /task
        post<TaskResource.Root> {
            val req = runCatching { call.receive<CreateTaskModelRequest>() }.getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid body")
            }
            val created = taskRepository.createTask(req)
            call.respond(HttpStatusCode.OK, created)
        }

        // PUT /task/{id}
        put<TaskResource.ById> { res ->
            val id = res.id.toUuidOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid task id")
            val req = runCatching { call.receive<UpdateTaskModelRequest>() }.getOrElse {
                return@put call.respond(HttpStatusCode.BadRequest, "Invalid body")
            }
            val updated = taskRepository.updateTask(id, req)
            if (updated == null) call.respond(HttpStatusCode.NotFound) else call.respond(HttpStatusCode.OK, updated)
        }

        // DELETE /task/{id}
        delete<TaskResource.ById> { res ->
            val id = res.id.toUuidOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid task id")
            val deleted = taskRepository.deleteTask(id)
            if (deleted) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
        }
    }
}

// -------- Resources --------

@Serializable
@Resource("/task")
class TaskResource {
    @Serializable
    @Resource("")
    class Root(val parent: TaskResource = TaskResource())

    @Serializable
    @Resource("{id}")
    class ById(val parent: TaskResource = TaskResource(), val id: String)

    @Serializable
    @Resource("theme/{id}")
    class ByTheme(val parent: TaskResource = TaskResource(), val id: String)
}

// -------- Helpers --------

private fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()