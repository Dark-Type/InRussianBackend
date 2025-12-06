package com.inRussian.config

import io.ktor.server.resources.*
import io.ktor.server.application.*

fun Application.configureResources() {
    install(Resources)
}
