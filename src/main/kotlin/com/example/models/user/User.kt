package com.example.models.user

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val phone: String? = null,
    val role: UserRole,
    val systemLanguage: SystemLanguage,
    val avatarId: String? = null,
    val createdAt: String = LocalDateTime.now().toString(),
    val updatedAt: String = LocalDateTime.now().toString()
)
