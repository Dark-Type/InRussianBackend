package com.inRussian.models.tasks


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TaskBody {
    @Serializable
    @SerialName("TextConnectTask")
    data class TextConnectTask(
        val variant: List<Pair<String, String>>
    ) : TaskBody

    @SerialName("AudioTask")
    @Serializable
    data class AudioConnectTask(val variant: List<Pair<String, String>>) : TaskBody

    @SerialName("ImageTask")
    @Serializable
    data class ImageConnectTask(val variant: List<Pair<String, String>>) : TaskBody

    @SerialName("TextInputTask")
    @Serializable
    data class TextInputTask(val task: List<TextInputModel>) : TaskBody

    @SerialName("TextInputWithVariantTask")
    @Serializable
    data class TextInputWithVariantTask(val task: TextInputWithVariantModel) : TaskBody


    @SerialName("ListenAndSelect")
    @Serializable
    data class ListenAndSelect(val task: ListenAndSelectModel) : TaskBody

    @SerialName("ImageAndSelect")
    @Serializable
    data class ImageAndSelect(val task: ImageAndSelectModel) : TaskBody

}

@Serializable
data class TextInputModel(
    val label: String,
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
    val audioBlocks: List<AudioBlocks>,
    val variants: List<Pair<String, Boolean>>
)

@Serializable
data class AudioBlocks(
    val name: String,
    val description: String?,
    val audio: String,
    val descriptionTranslate: String?,
)
@Serializable
data class ImageAndSelectModel(
    val imageBlocks: List<ImageBlocks>,
    val variants: List<Pair<String, Boolean>>
)
@Serializable
data class ImageBlocks(
    val name: String,
    val description: String?,
    val image: String,
    val descriptionTranslate: String?,
)