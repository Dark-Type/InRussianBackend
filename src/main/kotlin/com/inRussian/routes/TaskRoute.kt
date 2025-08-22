package com.inRussian.routes

import com.inRussian.requests.content.CreateTaskModelRequest
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.taskRoutes() {
    route("/task") {
        get() {

        }
        post {
            val request = call.receive<CreateTaskModelRequest>()
            println(request.taskType)

        }
    }
}