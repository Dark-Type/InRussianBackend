package com.inRussian.routes.v3.media


import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/media")
class MediaResource {
    @Serializable
    @Resource("upload")
    data class Upload(val parent: MediaResource = MediaResource(), val userId: String? = null)

    @Serializable
    @Resource("id/{mediaId}")
    data class ById(
        val parent: MediaResource = MediaResource(),
        val mediaId: String,
        val userId: String? = null
    )
}