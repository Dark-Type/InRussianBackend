package com.inRussian

import com.inRussian.config.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    println("Configuring application module...")

    configureSerialization()
    configureHTTP()
    configureMonitoring()
    configureSecurity()
    configureRouting()

    println("Application module configured successfully!")
}