package com.inRussian.config

import com.inRussian.models.tasks.TaskBody
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

fun Application.configureSerialization() {
    val taskBodyModule = SerializersModule {
        polymorphic(TaskBody::class) {
            subclass(TaskBody.TextTask::class)
            subclass(TaskBody.AudioTask::class)
            subclass(TaskBody.TextInputTask::class)
            subclass(TaskBody.TextInputWithVariantTask::class)
            subclass(TaskBody.ImageTask::class)
            subclass(TaskBody.ListenAndSelect::class)
        }
    }
    install(ContentNegotiation) {
        json(Json {
            serializersModule = taskBodyModule
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        })
    }
}