package com.example

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    println("Configuring application module...")

    configureSerialization()
    configureHTTP()
    configureMonitoring()
    configureRouting()
    configureDatabases()
    configureSecurity()

    println("Application module configured successfully!")
}