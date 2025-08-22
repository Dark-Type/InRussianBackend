package com.inRussian.models.tasks
import kotlinx.serialization.Serializable

@Serializable
sealed interface TaskBody {
    @Serializable
    data class AudioTask(val variant: List<Pair<ByteArray, String>>) : TaskBody

    @Serializable
    data class TextTask(
        val variant: List<Pair<String, String>>
    ) : TaskBody

    @Serializable
    data class TextInputTask(
        val variant: List<Pair<String, String>>
    ) : TaskBody

    @Serializable
    data class TextInputWithVariantTask(
        val variant: List<Pair<String, List<String>>>,
    ) : TaskBody

    @Serializable
    data class ImageTask(
        val variant: List<Pair<String, String>>,
    ) : TaskBody
}