package com.inRussian.config

import com.inRussian.models.tasks.TaskBody
import com.inRussian.repositories.ExposedTasksRepository
import com.inRussian.repositories.TasksRepository
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalSerializationApi::class)
object ByteArrayAsBase64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ByteArray", PrimitiveKind.STRING)

    @OptIn(ExperimentalEncodingApi::class)
    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(kotlin.io.encoding.Base64.encode(value))
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun deserialize(decoder: Decoder): ByteArray {
        return kotlin.io.encoding.Base64.decode(decoder.decodeString())
    }
}

val appJson: Json = Json {
    serializersModule = SerializersModule {
        polymorphic(TaskBody::class) {
            subclass(TaskBody.ContentBlocks::class)
            subclass(TaskBody.TextConnectTask::class)
            subclass(TaskBody.AudioConnectTask::class)
            subclass(TaskBody.TextInputTask::class)
            subclass(TaskBody.TextInputWithVariantTask::class)
            subclass(TaskBody.ImageConnectTask::class)
            subclass(TaskBody.ListenAndSelect::class)
            subclass(TaskBody.ImageAndSelect::class)
            subclass(TaskBody.ConstructSentenceTask::class)
            subclass(TaskBody.SelectWordsTask::class)
        }
        contextual(ByteArray::class, ByteArrayAsBase64Serializer)
    }
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    isLenient = true
    prettyPrint = false
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(appJson)
    }
}