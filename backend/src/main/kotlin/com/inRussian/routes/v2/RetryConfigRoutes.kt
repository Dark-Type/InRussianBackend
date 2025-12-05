package com.inRussian.routes.v2

import com.inRussian.services.v2.RetryService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import kotlinx.serialization.Serializable

@Serializable
data class RetrySwitchStatusDto(val enabled: Boolean)

fun Route.configurationRoutes(service: RetryService) {

    get("/configuration") {
        val result = service.getRetrySwitchStatus()
        result.fold(
            onSuccess = { status ->
                call.respond(HttpStatusCode.OK, RetrySwitchStatusDto(enabled = status))
            },
            onFailure = {
                call.respond(HttpStatusCode.InternalServerError, "Failed to get configuration")
            }
        )
    }
    authenticate("admin-jwt") {
        put ("/configuration") {
            val request = call.receive<RetrySwitchStatusDto>()
            val result = service.setRetrySwitchStatus(request.enabled)
            result.fold(
                onSuccess = { status ->
                    call.respond(HttpStatusCode.OK, RetrySwitchStatusDto(enabled = status))
                },
                onFailure = {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to update configuration")
                }
            )
        }
    }
}