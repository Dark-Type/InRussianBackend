package com.inRussian.requests.users

import kotlinx.serialization.Serializable

@Serializable
data class UserLanguageSkillRequest(
    val language: String,
    val understands: Boolean,
    val speaks: Boolean,
    val reads: Boolean,
    val writes: Boolean
)