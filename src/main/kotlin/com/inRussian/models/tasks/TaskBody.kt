package com.inRussian.models.tasks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TaskBody {
    @Serializable
    @SerialName("TextTask")
    data class TextTask(
        val variant: List<Pair<String, String>>
    ) : TaskBody

    @Serializable
    @SerialName("AudioTask")
    data class AudioTask(val variant: List<Pair<ByteArray, String>>) : TaskBody

    @Serializable
    @SerialName("TextInputTask")
    data class TextInputTask(val variant: List<Pair<String, String>>) : TaskBody

    @Serializable
    @SerialName("TextInputWithVariantTask")
    data class TextInputWithVariantTask(val variant: List<Pair<String, List<String>>>) : TaskBody

    @Serializable
    @SerialName("ImageTask")
    data class ImageTask(val variant: List<Pair<String, String>>) : TaskBody
}