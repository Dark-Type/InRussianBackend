package com.example.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserLanguageSkill(
    val userId: String,
    val language: String,
    val understands: Boolean = false,
    val speaks: Boolean = false,
    val reads: Boolean = false,
    val writes: Boolean = false
)