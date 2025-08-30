package com.inRussian.requests.users

data class UserLanguageSkillRequest(
    val language: String,
    val understands: Boolean,
    val speaks: Boolean,
    val reads: Boolean,
    val writes: Boolean
)