package com.inRussian.models.users

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    @Transient
    val passwordHash: String = "",
    val phone: String? = null,
    val role: UserRole,
    val systemLanguage: SystemLanguage,
    val avatarId: String? = null,
    val status: UserStatus = UserStatus.ACTIVE,
    val lastActivityAt: String? = null,
    val createdAt: String = LocalDateTime.now().toString(),
    val updatedAt: String = LocalDateTime.now().toString()
)