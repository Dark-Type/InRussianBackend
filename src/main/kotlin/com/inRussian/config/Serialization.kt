package com.inRussian.config

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSerialization() {
    // Content negotiation is now handled in configureHTTP()
    // This function can be used for additional serialization setup if needed
    // For now I will just keep it here anyways

    routing {
        get("/json/test") {
            call.respond(mapOf(
                "message" to "JSON serialization working",
                "timestamp" to java.time.LocalDateTime.now().toString(),
                "data" to listOf(1, 2, 3, 4, 5)
            ))
        }
    }
}