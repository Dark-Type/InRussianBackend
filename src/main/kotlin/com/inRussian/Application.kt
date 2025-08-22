package com.inRussian

import com.inRussian.config.*
import com.inRussian.tables.TaskModel

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/InRussian",
        driver = "org.postgresql.Driver",
        user = "nekit",
        password = "zex312ZEX"
    )
    transaction {
        addLogger(StdOutSqlLogger)
        arrayOf<Table>(TaskModel)
        Unit

    }

    println("Configuring application module...")
    configureSerialization()
    configureDatabase()
    configureHTTP()
    configureMonitoring()
    configureSecurity()
    configureRouting()
    println("Application module configured successfully!")
}