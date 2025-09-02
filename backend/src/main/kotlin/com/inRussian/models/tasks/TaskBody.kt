package com.inRussian.models.tasks


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TaskBody {
    @Serializable
    data class TextTask(
        val variant: List<Pair<String, String>>
    ) : TaskBody

    @SerialName("AudioTask")
    @Serializable
    data class AudioTask(val variant: List<Pair<String, String>>) : TaskBody

    @SerialName("TextInputTask")
    @Serializable
    data class TextInputTask(val task: TextInputModel) : TaskBody

    @SerialName("TextInputWithVariantTask")
    @Serializable
    data class TextInputWithVariantTask(val task: TextInputWithVariantModel) : TaskBody

    @SerialName("ImageTask")
    @Serializable
    data class ImageTask(val variant: List<Pair<String, String>>) : TaskBody

    @SerialName("ListenAndSelect")
    @Serializable
    data class ListenAndSelect(val task: ListenAndSelectModel) : TaskBody


}

@Serializable
data class TextInputModel(
    val text: String,
    val gaps: List<Gap>
)


@Serializable
data class Gap(
    val correctWord: String,
    val index: Int
)

@Serializable
data class TextInputWithVariantModel(
    val text: String,
    val gaps: List<GapWithVariantModel>
)

@Serializable
data class GapWithVariantModel(
    val position: Int,
    val variants: List<String>,
    val correctVariant: String,
)

@Serializable
data class ListenAndSelectModel(
    val text: String,
    val variants: List<Pair<String, Boolean>>
)